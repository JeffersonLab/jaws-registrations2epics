# kafka-streams-epics-alarms
Conditionally aggregates [EPICS](https://epics-controls.org) alarms read from [Kafka Connect EPICS](https://github.com/JeffersonLab/epics2web) into a single topic.  

Messages are propogated into the consolidated active-alarm topic if the messages indicate an EPICs alarm state. 
