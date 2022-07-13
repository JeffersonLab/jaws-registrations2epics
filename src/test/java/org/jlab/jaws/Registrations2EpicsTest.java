package org.jlab.jaws;

import org.apache.kafka.streams.*;
import org.jlab.jaws.entity.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class Registrations2EpicsTest {
    private TopologyTestDriver testDriver;
    private TestInputTopic<String, AlarmInstance> inputTopic;
    private TestOutputTopic<String, String> outputTopic;
    private AlarmInstance instance = new AlarmInstance();

    @Before
    public void setup() {
        final Properties streamsConfig = Registrations2Epics.getStreamsConfig();
        streamsConfig.put(SCHEMA_REGISTRY_URL_CONFIG, "mock://testing");
        final Topology top = Registrations2Epics.createTopology(streamsConfig);
        testDriver = new TopologyTestDriver(top, streamsConfig);

        // setup test topics
        inputTopic = testDriver.createInputTopic(Registrations2Epics.INPUT_TOPIC, Registrations2Epics.INPUT_KEY_SERDE.serializer(), Registrations2Epics.INPUT_VALUE_SERDE.serializer());
        outputTopic = testDriver.createOutputTopic(Registrations2Epics.OUTPUT_TOPIC, Registrations2Epics.OUTPUT_KEY_SERDE.deserializer(), Registrations2Epics.OUTPUT_VALUE_SERDE.deserializer());

        EPICSSource source = new EPICSSource();
        source.setPv("channel1");

        instance.setSource(source);
        instance.setAlarmclass("base");
        instance.setLocation(Arrays.asList("INJ"));
        instance.setScreencommand("/");
    }

    @After
    public void tearDown() {
        if(testDriver != null) {
            testDriver.close();
        }
    }

    @Test
    public void matchedTombstoneMsg() {
        inputTopic.pipeInput("alarm1", instance);
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
        inputTopic.pipeInput("alarm1", instance);
        KeyValue<String, String> result = outputTopic.readKeyValue();
        Assert.assertEquals("{\"topic\":\"alarm-activations\",\"channel\":\"channel1\"}", result.key);
        Assert.assertEquals("{\"mask\":\"a\",\"outkey\":\"alarm1\"}", result.value);
    }
}
