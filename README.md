# kafka-streams-epics-alarms
A [Kafka Streams](https://kafka.apache.org/documentation/streams/) application to continuously and conditionally aggregate [EPICS](https://epics-controls.org) alarms read from [Kafka Connect EPICS](https://github.com/JeffersonLab/epics2kafka) topics into a single __active-alarms__ topic.  

This Kafka Streams application uses the [kafka-alarm-system](https://github.com/JeffersonLab/kafka-alarm-system) __alarms__ topic to determine which EPICS channel topics to monitor.   Messages are propagated into the consolidated active-alarms topic if the messages indicate an EPICS alarm state.

## Quick Start with Docker 
1. Grab project
```
git clone https://github.com/JeffersonLab/kafka-streams-epics-alarms
cd kafka-streams-epics-alarms
```
2. Launch Docker
```
docker-compose up
```
3. Monitor active alarms
```
docker exec -it client /scripts/active-alarms/list-active.py --monitor
```
4. Trip an EPICS alarm  
```
docker exec softioc caput channel1 1
```
