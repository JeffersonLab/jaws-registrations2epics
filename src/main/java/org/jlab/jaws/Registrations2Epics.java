package org.jlab.jaws;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.jlab.jaws.entity.EPICSSource;
import org.jlab.jaws.entity.AlarmInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        final KStream<String, String> output = input.transform(new MsgTransformerFactory(storeBuilder.name()), storeBuilder.name());

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
    private static final class MsgTransformerFactory implements TransformerSupplier<String, AlarmInstance, KeyValue<String, String>> {

        private final String storeName;

        /**
         * Create a new MsgTransformerFactory.
         *
         * @param storeName The state store name
         */
        public MsgTransformerFactory(String storeName) {
            this.storeName = storeName;
        }

        /**
         * Return a new {@link Transformer} instance.
         *
         * @return a new {@link Transformer} instance
         */
        @Override
        public Transformer<String, AlarmInstance, KeyValue<String, String>> get() {
            return new Transformer<String, AlarmInstance, KeyValue<String, String>>() {
                private KeyValueStore<String, AlarmInstance> store;

                @Override
                @SuppressWarnings("unchecked") // https://cwiki.apache.org/confluence/display/KAFKA/KIP-478+-+Strongly+typed+Processor+API
                public void init(ProcessorContext context) {
                    store = (KeyValueStore<String, AlarmInstance>) context.getStateStore(storeName);
                }

                @Override
                public KeyValue<String, String> transform(String key, AlarmInstance value) {
                    KeyValue<String, String> result = null; // null returned to mean no record - when not of type DirectCAAlarm OR when an unmatched tombstone is encountered

                    String channel;

                    if(value == null) { // Tombstone - we need most recent non-null registration to transform
                        AlarmInstance previous = store.get(key);
                        if(previous != null) { // We only store EPICSProducer, so no need to check type
                            channel = ((EPICSSource)previous.getSource()).getPv();
                            result = KeyValue.pair(toJsonKey(channel), toJsonValue(key, value));
                        }
                    } else if(value.getSource() instanceof EPICSSource) {
                        channel = ((EPICSSource) value.getSource()).getPv();
                        result = KeyValue.pair(toJsonKey(channel), toJsonValue(key, value));
                        store.put(key, value);  // Store most recent non-null registration for each CA alarm (key)
                    }

                    LOGGER.trace("Transformed: {}={} -> {}", key, value, result);

                    return result;
                }

                @Override
                public void close() {
                    // Nothing to do
                }
            };
        }
    }
}
