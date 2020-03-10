package org.jlab.kafka.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.util.Properties;

public class AlarmGenerator {
    public void start() throws IOException {
        String url = System.getenv("KAFKA_URL");

        if(url == null) {
            throw new IOException("Environment variable KAFKA_URL not found");
        }

        Properties props = new Properties();
        props.put("bootstrap.servers", url);
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<>(props);
        for (int i = 0; i < 100; i++)
            producer.send(new ProducerRecord<String, String>("alarms", Integer.toString(i), Integer.toString(i)));

        producer.close();
    }
    public static void main(String[] args) throws IOException {
        AlarmGenerator generator = new AlarmGenerator();

        generator.start();
    }
}
