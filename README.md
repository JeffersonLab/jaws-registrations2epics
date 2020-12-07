# registrations2epics [![Build Status](https://travis-ci.com/JeffersonLab/registrations2epics.svg?branch=master)](https://travis-ci.com/JeffersonLab/registrations2epics)    
A [Kafka Streams](https://kafka.apache.org/documentation/streams/) application to continuously populate the [epics2kafka](https://github.com/JeffersonLab/epics2kafka) _epics-channels_ topic from the [kafka-alarm-system](https://github.com/JeffersonLab/kafka-alarm-system) _registered-alarms_ topic for the subset of messages containing producer type __DirectCAAlarm__.  

---
 - [Quick Start with Compose](https://github.com/JeffersonLab/registrations2epics#quick-start-with-compose)
 - [Build](https://github.com/JeffersonLab/registrations2epics#build)
 - [Docker](https://github.com/JeffersonLab/registrations2epics#docker)
 - [See Also](https://github.com/JeffersonLab/registrations2epics#see-also)
 ---

## Quick Start with Compose 
1. Grab project
```
git clone https://github.com/JeffersonLab/registrations2epics
cd registrations2epics
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

## See Also
   - [Developer Notes](https://github.com/JeffersonLab/registrations2epics/wiki/Developer-Notes)
