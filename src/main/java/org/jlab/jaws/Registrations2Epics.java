package org.jlab.jaws;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.jlab.jaws.entity.EPICSSource;
import org.jlab.jaws.entity.AlarmInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

/**
 * A Kafka Streams application to populate the epics2kafka epics-channels topic from the JAWS
 * effective-registrations topic for the subset of messages of type EPICSProducer.
 */
public final class Registrations2Epics {

    private static final Logger LOGGER = LoggerFactory.getLogger(Registrations2Epics.class);

    // TODO: these need to be configurable
    public static final String INPUT_TOPIC = "alarm-instances";
    public static final String OUTPUT_TOPIC = "epics-channels";

    public static final Serde<String> INPUT_KEY_SERDE = Serdes.String();
    public static final SpecificAvroSerde<AlarmInstance> INPUT_VALUE_SERDE = new SpecificAvroSerde<>();
    public static final Serde<String> OUTPUT_KEY_SERDE = INPUT_KEY_SERDE;
    public static final Serde<String> OUTPUT_VALUE_SERDE = INPUT_KEY_SERDE;

    static Properties getStreamsConfig() {

        LOGGER.trace("getStreamConfig()");

        String bootstrapServers = System.getenv("BOOTSTRAP_SERVERS");

        bootstrapServers = (bootstrapServers == null) ? "localhost:9092" : bootstrapServers;

        String registry = System.getenv("SCHEMA_REGISTRY");

        registry = (registry == null) ? "http://localhost:8081" : registry;

        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "registrations2epics");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 0); // Disable caching
        props.put(SCHEMA_REGISTRY_URL_CONFIG, registry);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    /**
     * Create the Kafka Streams Domain Specific Language (DSL) Topology.
     *
     * @param props The streams configuration
     * @return The Topology
     */
    static Topology createTopology(Properties props) {

        LOGGER.trace("createTopology()");

        final StreamsBuilder builder = new StreamsBuilder();
        Map<String, String> config = new HashMap<>();
        config.put(SCHEMA_REGISTRY_URL_CONFIG, props.getProperty(SCHEMA_REGISTRY_URL_CONFIG));
        INPUT_VALUE_SERDE.configure(config, false);

        final StoreBuilder<KeyValueStore<String, AlarmInstance>> storeBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("Registrations2EpicsStore"),
                INPUT_KEY_SERDE,
                INPUT_VALUE_SERDE
        ).withCachingEnabled();

        builder.addStateStore(storeBuilder);

        final KStream<String, AlarmInstance> input = builder.stream(INPUT_TOPIC, Consumed.with(INPUT_KEY_SERDE, INPUT_VALUE_SERDE));

        final KStream<String, String> output = input.process(new MyProcessorSupplier(storeBuilder.name()), storeBuilder.name());

        output.to(OUTPUT_TOPIC, Produced.with(OUTPUT_KEY_SERDE, OUTPUT_VALUE_SERDE));

        return builder.build();
    }

    private static String toJsonKey(String channel) {
        return "{\"topic\":\"alarm-activations\",\"channel\":\"" + channel + "\"}";
    }

    private static String toJsonValue(String outkey, AlarmInstance registration) {
        return registration == null ? null : "{\"mask\":\"a\",\"outkey\":\"" + outkey + "\"}";
    }

    /**
     * Entrypoint of the application.
     *
     * @param args The command line arguments
     */
    public static void main(String[] args) {
        final Properties props = getStreamsConfig();
        final Topology top = createTopology(props);
        final KafkaStreams streams = new KafkaStreams(top, props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (final Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * Factory to create Kafka Streams Transformer instances; references a stateStore to maintain previous
     * AlarmInstances.
     */
    private static final class MyProcessorSupplier implements ProcessorSupplier<String, AlarmInstance, String, String> {

        private final String storeName;

        /**
         * Create a new MsgTransformerFactory.
         *
         * @param storeName The state store name
         */
        public MyProcessorSupplier(String storeName) {
            this.storeName = storeName;
        }

        /**
         * Return a new {@link Transformer} instance.
         *
         * @return a new {@link Transformer} instance
         */
        @Override
        public Processor<String, AlarmInstance, String, String> get() {
            return new Processor<String, AlarmInstance, String, String>() {
                private ProcessorContext<String, String> context;
                private KeyValueStore<String, AlarmInstance> store;

                @Override
                public void init(ProcessorContext<String, String> context) {
                    this.context = context;
                    this.store = context.getStateStore(storeName);
                }

                @Override
                public void process(Record<String, AlarmInstance> input) {
                    Record<String, String> output = null; // null returned to mean no record - when not of type DirectCAAlarm OR when an unmatched tombstone is encountered

                    long timestamp = System.currentTimeMillis();

                    String channel;

                    if(input.value() == null) { // Tombstone - we need most recent non-null registration to transform
                        AlarmInstance previous = store.get(input.key());
                        if(previous != null) { // We only store EPICSProducer, so no need to check type
                            channel = ((EPICSSource)previous.getSource()).getPv();
                            output = new Record<>(toJsonKey(channel), toJsonValue(input.key(), input.value()), timestamp);
                            populateHeaders(output);
                        }
                    } else if(input.value().getSource() instanceof EPICSSource) {
                        channel = ((EPICSSource) input.value().getSource()).getPv();
                        output = new Record<>(toJsonKey(channel), toJsonValue(input.key(), input.value()), timestamp);
                        populateHeaders(output);
                        store.put(input.key(), input.value());  // Store most recent non-null registration for each CA alarm (key)
                    }

                    LOGGER.trace("Transformed: {}={} -> {}", input.key(), input.value(), output);

                    if(output != null) {
                        context.forward(output);
                    }
                }

                @Override
                public void close() {
                    // Nothing to do
                }
            };
        }

        private void populateHeaders(Record<String, String> record) {
            String host = "unknown";

            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOGGER.debug("Unable to obtain host name");
            }

            record.headers().add("user", System.getProperty("user.name").getBytes(StandardCharsets.UTF_8));
            record.headers().add("producer", "registrations2epics".getBytes(StandardCharsets.UTF_8));
            record.headers().add("host", host.getBytes(StandardCharsets.UTF_8));
        }
    }
}
