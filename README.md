# kafka-streams-epics-alarms
A [Kafka Streams](https://kafka.apache.org/documentation/streams/) application to continuously and conditionally aggregate [EPICS](https://epics-controls.org) alarms read from [Kafka Connect EPICS](https://github.com/JeffersonLab/epics2web) topics into a single __active-alarms__ topic.  

This Kafka Streams application uses the [kafka-alarm-system](https://github.com/JeffersonLab/kafka-alarm-system) __alarms__ topic to determine which EPICS channel topics to monitor.   Messages are propogated into the consolidated active-alarm topic if the messages indicate an EPICs alarm state.
