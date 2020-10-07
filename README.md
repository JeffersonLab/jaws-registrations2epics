# kafka-streams-epics-alarms
A [Kafka Streams](https://kafka.apache.org/documentation/streams/) application to continuously populate the [epics2kafka](https://github.com/JeffersonLab/epics2kafka) epics-channels topic from the [kafka-alarm-system](https://github.com/JeffersonLab/kafka-alarm-system) topic registered-alarms given messages containing producer type "epics2kafka".  

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
docker exec -it console /scripts/active-alarms/list-active.py --monitor
```
4. Trip an EPICS alarm  
```
docker exec softioc caput channel1 1
```

This compose extends the [kafka-alarm-system](https://github.com/JeffersonLab/kafka-alarm-system) with the following services:
   - [Connect EPICS](https://github.com/JeffersonLab/epics2kafka) - transfer EPICS CA messages into Kafka, one topic per EPICS channel
   - Streams EPICS Alarms - defined in this project; conditionally consolidates and propogates EPICS alarms read from Connect EPICS topics to the __active-alarms__ topic based on configuration in the master __alarms__ topic
   - [softioc](https://github.com/JeffersonLab/softioc) - hosts an EPICS CA database, a softioc is used for testing and demonstration, but this could be replaced with an entire EPICS environment
