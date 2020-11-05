package org.jlab.kafka.streams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.jlab.kafka.alarms.AlarmCategory;
import org.jlab.kafka.alarms.AlarmLocation;
import org.jlab.kafka.alarms.DirectCAAlarm;
import org.jlab.kafka.alarms.RegisteredAlarm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class Registrations2EpicsTest {
    private TopologyTestDriver testDriver;
    private TestInputTopic<String, RegisteredAlarm> inputTopic;
    private TestOutputTopic<String, String> outputTopic;

    @Before
    public void setup() {
        final Properties streamsConfig = Registrations2Epics.getStreamsConfig();
        streamsConfig.put(SCHEMA_REGISTRY_URL_CONFIG, "mock://testing");
        final Topology top = Registrations2Epics.createRuleTopology(streamsConfig);

        // setup test driver
        final Properties props = new Properties();
        props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "testing");
        props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        testDriver = new TopologyTestDriver(top, props);

        // setup test topics
        inputTopic = testDriver.createInputTopic(Registrations2Epics.INPUT_TOPIC, Registrations2Epics.INPUT_KEY_SERDE.serializer(), Registrations2Epics.INPUT_VALUE_SERDE.serializer());
        outputTopic = testDriver.createOutputTopic(Registrations2Epics.OUTPUT_TOPIC, Registrations2Epics.OUTPUT_KEY_SERDE.deserializer(), Registrations2Epics.OUTPUT_VALUE_SERDE.deserializer());
    }

    @After
    public void tearDown() {
        testDriver.close();
    }


    @Test
    public void tombstoneMsg() {
        RegisteredAlarm registration = null;
        inputTopic.pipeInput("alarm1", registration);
        KeyValue<String, String> key = outputTopic.readKeyValue();
        System.err.println(key);
    }

    @Test
    public void regularMsg() {
        RegisteredAlarm registration = new RegisteredAlarm();
        DirectCAAlarm direct = new DirectCAAlarm();
        direct.setPv("testpv");
        registration.setProducer(direct);
        registration.setCategory(AlarmCategory.Magnet);
        registration.setLocation(AlarmLocation.INJ);
        registration.setDocurl("/");
        registration.setEdmpath("/");
        inputTopic.pipeInput("alarm1", registration);
        KeyValue<String, String> key = outputTopic.readKeyValue();
        System.err.println(key);
    }
}
