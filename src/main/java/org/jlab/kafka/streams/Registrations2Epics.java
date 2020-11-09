package org.jlab.kafka.streams;

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
import org.jlab.kafka.alarms.DirectCAAlarm;
import org.jlab.kafka.alarms.RegisteredAlarm;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public final class Registrations2Epics {

    private static final Logger LOGGER = Logger.getLogger("org.jlab.kafka.streams.AutoConfigureConnectEpics");

    // TODO: these need to be configurable
    public static final String INPUT_TOPIC = "registered-alarms";
    public static final String OUTPUT_TOPIC = "epics-channels";

    public static final Serde<String> INPUT_KEY_SERDE = Serdes.String();
    public static final SpecificAvroSerde<RegisteredAlarm> INPUT_VALUE_SERDE = new SpecificAvroSerde();
    public static final Serde<String> OUTPUT_KEY_SERDE = INPUT_KEY_SERDE;
    public static final Serde<String> OUTPUT_VALUE_SERDE = INPUT_KEY_SERDE;

    static Properties getStreamsConfig() {

        String bootstrapServers = System.getenv("BOOTSTRAP_SERVERS");

        bootstrapServers = (bootstrapServers == null) ? "localhost:9092" : bootstrapServers;

        String registry = System.getenv("SCHEMA_REGISTRY");

        registry = (registry == null) ? "http://localhost:8081" : registry;

        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "auto-configure-connect-epics");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(SCHEMA_REGISTRY_URL_CONFIG, registry);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    static Topology createRuleTopology(Properties props) {
        final StreamsBuilder builder = new StreamsBuilder();
        Map<String, String> config = new HashMap<>();
        config.put(SCHEMA_REGISTRY_URL_CONFIG, props.getProperty(SCHEMA_REGISTRY_URL_CONFIG));
        INPUT_VALUE_SERDE.configure(config, false);

        final StoreBuilder<KeyValueStore<String, RegisteredAlarm>> storeBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("Registrations2EpicsStore"),
                INPUT_KEY_SERDE,
                INPUT_VALUE_SERDE
        ).withCachingEnabled();

        builder.addStateStore(storeBuilder);

        final KStream<String, RegisteredAlarm> input = builder.stream(INPUT_TOPIC, Consumed.with(INPUT_KEY_SERDE, INPUT_VALUE_SERDE));

        final KStream<String, String> output = input.transform(new MsgTransformer(storeBuilder.name()), storeBuilder.name());

        output.to(OUTPUT_TOPIC, Produced.with(OUTPUT_KEY_SERDE, OUTPUT_VALUE_SERDE));

        return builder.build();
    }

    private static String toJsonKey(String channel) {
        return "{\"topic\":\"active-alarms\",\"channel\":\"" + channel + "\"}";
    }

    private static String toJsonValue(RegisteredAlarm registration) {
        return registration == null ? null : "{\"mask\":\"a\"}";
    }

    public static void main(final String[] args) {
        final Properties props = getStreamsConfig();
        final Topology top = createRuleTopology(props);
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

    private static final class MsgTransformer implements TransformerSupplier<String, RegisteredAlarm, KeyValue<String, String>> {

        private final String storeName;

        public MsgTransformer(String storeName) {
            this.storeName = storeName;
        }

        /**
         * Return a new {@link Transformer} instance.
         *
         * @return a new {@link Transformer} instance
         */
        @Override
        public Transformer<String, RegisteredAlarm, KeyValue<String, String>> get() {
            return new Transformer<String, RegisteredAlarm, KeyValue<String, String>>() {
                private KeyValueStore<String, RegisteredAlarm> store;

                @Override
                public void init(ProcessorContext context) {
                    store = (KeyValueStore<String, RegisteredAlarm>) context.getStateStore(storeName);
                }

                @Override
                public KeyValue<String, String> transform(String key, RegisteredAlarm value) {
                    KeyValue<String, String> result = null; // null returned to mean no record - when not of type DirectCAAlarm OR when an unmatched tombstone is encountered

                    String channel;

                    if(value == null) { // Tombstone - we need most recent non-null registration to transform
                        RegisteredAlarm previous = store.get(key);
                        if(previous != null) { // We only store DirectCAAlarm, so no need to check type
                            channel = ((DirectCAAlarm)previous.getProducer()).getPv();
                            result = KeyValue.pair(toJsonKey(channel), toJsonValue(value));
                        }
                    } else if(value.getProducer() instanceof DirectCAAlarm) {
                        channel = ((DirectCAAlarm) value.getProducer()).getPv();
                        result = KeyValue.pair(toJsonKey(channel), toJsonValue(value));
                        store.put(key, value);  // Store most recent non-null registration for each CA alarm (key)
                    }

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
