package org.jlab.kafka.streams;

import io.confluent.kafka.serializers.KafkaAvroSerializer;

import org.jlab.AlarmMetadata;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jlab.NativeAlarm;
import org.jlab.StreamsAlarm;

import java.io.IOException;
import java.util.Properties;

public class AlarmGenerator {
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
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", KafkaAvroSerializer.class);
        props.put("schema.registry.url", registryUrl);

        Producer<String, AlarmMetadata> producer = new KafkaProducer<>(props);
        for (int i = 0; i < 100; i++) {
            AlarmMetadata metadata = new AlarmMetadata();
            NativeAlarm nativeType = new NativeAlarm();
            StreamsAlarm streamsType = new StreamsAlarm();
            nativeType.pv = "some pv name goes here";
            streamsType.jarPath = "some path goes here";


            Object type = nativeType;

            if(i % 2 == 0) {
                type = streamsType;
            }

            metadata.setName("alarm " + i);
            metadata.setCategory("Other");
            metadata.setDocUrl("");
            metadata.setType(type);
            metadata.setLocation("MCC");
            metadata.setOpsEdmScreenPath("/cs/opshome/edm");

            producer.send(new ProducerRecord<String, AlarmMetadata>("alarms", Integer.toString(i), metadata));
        }

        producer.close();
    }
    public static void main(String[] args) throws IOException {
        AlarmGenerator generator = new AlarmGenerator();

        generator.start();
    }
}
