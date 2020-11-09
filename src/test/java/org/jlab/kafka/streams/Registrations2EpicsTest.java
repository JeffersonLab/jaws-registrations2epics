package org.jlab.kafka.streams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.jlab.kafka.alarms.AlarmCategory;
import org.jlab.kafka.alarms.AlarmLocation;
import org.jlab.kafka.alarms.DirectCAAlarm;
import org.jlab.kafka.alarms.RegisteredAlarm;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class Registrations2EpicsTest {
    private TopologyTestDriver testDriver;
    private TestInputTopic<String, RegisteredAlarm> inputTopic;
    private TestOutputTopic<String, String> outputTopic;
    private RegisteredAlarm alarm1;

    @Before
    public void setup() {
        final Properties streamsConfig = Registrations2Epics.getStreamsConfig();
        streamsConfig.put(SCHEMA_REGISTRY_URL_CONFIG, "mock://testing");
        final Topology top = Registrations2Epics.createTopology(streamsConfig);

        // setup test driver
        final Properties props = new Properties();
        props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "testing");
        props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        testDriver = new TopologyTestDriver(top, props);

        // setup test topics
        inputTopic = testDriver.createInputTopic(Registrations2Epics.INPUT_TOPIC, Registrations2Epics.INPUT_KEY_SERDE.serializer(), Registrations2Epics.INPUT_VALUE_SERDE.serializer());
        outputTopic = testDriver.createOutputTopic(Registrations2Epics.OUTPUT_TOPIC, Registrations2Epics.OUTPUT_KEY_SERDE.deserializer(), Registrations2Epics.OUTPUT_VALUE_SERDE.deserializer());

        DirectCAAlarm direct = new DirectCAAlarm();
        direct.setPv("channel1");
        alarm1 = new RegisteredAlarm();
        alarm1.setProducer(direct);
        alarm1.setCategory(AlarmCategory.Magnet);
        alarm1.setLocation(AlarmLocation.INJ);
        alarm1.setDocurl("/");
        alarm1.setEdmpath("/");
    }

    @After
    public void tearDown() {
        testDriver.close();
    }

    @Test
    public void matchedTombstoneMsg() {
        inputTopic.pipeInput("alarm1", alarm1);
        inputTopic.pipeInput("alarm1", null);
        KeyValue<String, String> result = outputTopic.readKeyValuesToList().get(1);
        Assert.assertNull(result.value);
    }


    @Test
    public void unmatchedTombstoneMsg() {
        inputTopic.pipeInput("alarm1", null);
        Assert.assertTrue(outputTopic.isEmpty()); // Cannot transform a tombstone without a prior registration!
    }

    @Test
    public void regularMsg() {
        inputTopic.pipeInput("alarm1", alarm1);
        KeyValue<String, String> result = outputTopic.readKeyValue();
        Assert.assertEquals("{\"topic\":\"active-alarms\",\"channel\":\"channel1\"}", result.key);
        Assert.assertEquals("{\"mask\":\"a\"}", result.value);
    }
}
