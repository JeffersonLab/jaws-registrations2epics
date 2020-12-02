# registrations2epics [![Build Status](https://travis-ci.com/JeffersonLab/registrations2epics.svg?branch=master)](https://travis-ci.com/JeffersonLab/registrations2epics)    
A [Kafka Streams](https://kafka.apache.org/documentation/streams/) application to continuously populate the [epics2kafka](https://github.com/JeffersonLab/epics2kafka) _epics-channels_ topic from the [kafka-alarm-system](https://github.com/JeffersonLab/kafka-alarm-system) _registered-alarms_ topic for the subset of messages containing producer type __DirectCAAlarm__.  

---
 - [Quick Start with Compose](https://github.com/JeffersonLab/registrations2epics#quick-start-with-compose)
 - [Build](https://github.com/JeffersonLab/registrations2epics#build)
 - [Docker](https://github.com/JeffersonLab/registrations2epics#docker)
 - [Tombstones](https://github.com/JeffersonLab/registrations2epics#tombstones)
 - [Reset](https://github.com/JeffersonLab/registrations2epics#reset)
---

## Quick Start with Compose 
1. Grab project
```
git clone https://github.com/JeffersonLab/kafka-streams-epics-alarms
cd kafka-streams-epics-alarms
```
2. Launch Docker
```
docker-compose up
```
3. Register an alarm
```
docker exec -it console /scripts/registered-alarms/set-registered.py channel1 --producerpv channel1 --location INJ --category RF --docurl / --edmpath / 
```
4. Verify that the epics-channels command topic received a new channel to monitor 
```
docker exec kafka /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic epics-channels --from-beginning --property "print.key=true" --property "key.separator==" 
```

## Build
```
gradlew build
```

## Docker
```
docker pull slominskir/registrations2epics
```
Image hosted on [DockerHub](https://hub.docker.com/r/slominskir/registrations2epics)

## Tombstones
A RegisteredAlarm record is unset via a tombstone message, but a null value means the Streams app cannot use the value to determine:
   1. If the message is of type DirectCAAlarm 
   1. What is the name of the channel
   
Both of these pieces of information are needed to produce a tombstone message to the epics-channels topic.  To make this work, the app relies on a state store to maintain the most recent RegisteredAlarm record for a given key (alarm name).   This works in the general case, but will not work in the rare corner case of an unmatched tombstone: a tombstone record is encountered, but the state store does not have a record of the previous registration given the same key.  This can occur if the registrations2epics app is offline while changes are made to the registered-alarms topic and topic compaction runs and removes a registration that was tombstoned.  In this case, an unregister message is ignored.

## Reset
During development and testing simply blowing away all Docker containers is the easiest method to start with fresh state.   However, if you have a running Kafka instance that you'd prefer not to blow away, such as in production, you'll need to use the [reset tool](https://kafka.apache.org//documentation/streams/developer-guide/app-reset-tool) if you need to clear state to reprocess data.   For example, if you wanted to keep the "registered-alarms" topic as is, but re-create the epics-channels topic then you'll need to reset the consumer offsets in the registered-alarms topic using the reset tool, manually delete and re-create the epics-channels topic, and finally manually delete local app state:

```
/kafka/bin/kafka-streams-application-reset --input-topics registered-alarms --bootstrap-servers kafka:9092
/kafka/bin/kafka-topics.sh --delete --topic epics-channels --bootstrap-servers kafka:9092
/kafka/bin/kafka-topics.sh --create --topic epics-channels --bootstrap-servers kafka:9092 --config cleanup.policy=compact
rm /tmp/kafka-streams/registrations2epics
```
