package org.jlab.kafka.streams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import java.util.*;
import java.util.concurrent.CountDownLatch;

public final class CAPassthrough {

    public static final String INPUT_TOPIC1 = "channel1";
    public static final String INPUT_TOPIC2 = "channel2";
    public static final String OUTPUT_TOPIC = "active-alarms";

    static Properties getStreamsConfig() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-test-rule");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafkatest:9092");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    static void createTestRuleStream(final StreamsBuilder builder) {
        KStream<String, String> source1 = builder.stream(INPUT_TOPIC1);
        source1 = source1.mapValues(value -> "channel1 " + value);

        KStream<String, String> source2 = builder.stream(INPUT_TOPIC2);
        source2 = source2.mapValues(value -> "channel2 " + value);

        final KStream<String, String> source = source1.merge(source2);

        final KStream<String, String> alarms = source.filter((k, v) -> {
            System.out.printf("Handling msg; key = %s, value = %s%n", k, v);

            return v != null && v.contains("MAJOR_ALARM");
        });

        alarms.to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));
    }

    public static void main(final String[] args) {
        final Properties props = getStreamsConfig();

        final StreamsBuilder builder = new StreamsBuilder();
        createTestRuleStream(builder);
        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-ca-passthrough-shutdown-hook") {
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
