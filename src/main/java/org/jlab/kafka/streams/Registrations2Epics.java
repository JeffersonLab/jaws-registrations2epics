package org.jlab.kafka.streams;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.jlab.kafka.alarms.DirectCAAlarm;
import org.jlab.kafka.alarms.RegisteredAlarm;
import sun.security.util.RegisteredDomain;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public final class Registrations2Epics {

    private static final Logger LOGGER = Logger.getLogger("org.jlab.kafka.streams.AutoConfigureConnectEpics");

    // TODO: these need to be configurable
    public static final String INPUT_TOPIC = "registered-alarms";
    public static final String OUTPUT_TOPIC = "epics-channels";

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
        final SpecificAvroSerde<RegisteredAlarm> serde = new SpecificAvroSerde<>();
        Map<String, String> config = new HashMap<>();
        config.put(SCHEMA_REGISTRY_URL_CONFIG, props.getProperty(SCHEMA_REGISTRY_URL_CONFIG));
        serde.configure(config, false);

        final KStream<String, RegisteredAlarm> input = builder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), serde));

        final KStream<String, String> output = input.filter((k, v) -> {
            //System.err.printf("key = %s, value = %s%n", k, v);

            return v.getProducer() instanceof DirectCAAlarm;
        }).map((key, value) -> new KeyValue<>(toJsonKey(key), toJsonValue()));

        output.to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));

        return builder.build();
    }

    private static String toJsonKey(String avroKey) {
        return "{\"topic\":\"active-alarms\",\"channel\":\"" + avroKey + "\"}";
    }

    private static String toJsonValue() {
        return "{\"mask\":\"a\"}";
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
}
