package org.jlab.kafka.consumer;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.jlab.AlarmMetadata;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class AlarmConsumer {
    public void start() throws IOException {
        String kafkaUrl = System.getenv("KAFKA_URL");
        String registryUrl = System.getenv("SCHEMA_REGISTRY_URL");

        if(kafkaUrl == null) {
            throw new IOException("Environment variable KAFKA_URL not found");
        }

        if(registryUrl == null) {
            throw new IOException("Environment variable SCHEMA_REGISTRY_URL not found");
        }

        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaUrl);
        props.put("group.id", "test");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", KafkaAvroDeserializer.class);
        props.put("schema.registry.url", registryUrl);
        KafkaConsumer<String, AlarmMetadata> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("alarms"));
        while (true) {
            ConsumerRecords<String, AlarmMetadata> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, AlarmMetadata> record : records)
                System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
        }
    }

    public static void main(String[] args) throws IOException {
        AlarmConsumer consumer = new AlarmConsumer();
        consumer.start();
    }
}
