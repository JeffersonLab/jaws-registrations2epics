# registrations2epics [![Java CI with Gradle](https://github.com/JeffersonLab/registrations2epics/workflows/Java%20CI%20with%20Gradle/badge.svg)](https://github.com/JeffersonLab/registrations2epics/actions?query=workflow%3A%22Java+CI+with+Gradle%22) [![Docker Image Version (latest semver)](https://img.shields.io/docker/v/slominskir/registrations2epics?sort=semver&label=DockerHub)   ](https://hub.docker.com/r/slominskir/registrations2epics)
A [Kafka Streams](https://kafka.apache.org/documentation/streams/) application to continuously populate the [epics2kafka](https://github.com/JeffersonLab/epics2kafka) _epics-channels_ topic from the [JAWS](https://github.com/JeffersonLab/jaws) _alarm-instances_ topic for the subset of messages containing producer type __EPICSProducer__.  

---
 - [Overview](https://github.com/JeffersonLab/registrations2epics#overview)
 - [Quick Start with Compose](https://github.com/JeffersonLab/registrations2epics#quick-start-with-compose)
 - [Install](https://github.com/JeffersonLab/registrations2epics#install)
 - [Configure](https://github.com/JeffersonLab/registrations2epics#configure)
 - [Build](https://github.com/JeffersonLab/registrations2epics#build) 
 - [See Also](https://github.com/JeffersonLab/registrations2epics#see-also)
 ---

## Overview
This app keeps epics2kafka automatically configured based on the JAWS configuration.  This Kafka Streams app consumes the `alarm-instances` topic and looks for registration messages related to EPICS and for each of those produces a command message to epics2kafka via the `epics-channels` topic.   

When a JAWS registration is removed it is removed via a tombstone message, which is a null value for a given key.  This presents a challenge as a registration removal does not have a value payload to indicate whether it is an EPICS registration or what is the PV/channel name.  The epics2kafka epics-channels topic key contains a PV/channel whereas the JAWS alarm-instances key is an alarm name.   To overcome this challenge this app is not stateless, it uses a Kafka Streams store to track all JAWS registration records that have been used to command epics2kafka.  This way the registration record key, the alarm name, can be used in the tombstone case to lookup if the tombstone means action is needed.

## Quick Start with Compose 
1. Grab project
```
git clone https://github.com/JeffersonLab/registrations2epics
cd registrations2epics
```
2. Launch Docker
```
docker compose up
```
3. Register an alarm
```
docker exec -it jaws /scripts/client/set-instance.py alarm1 --producerpv channel1 
```
4. Verify that the epics-channels command topic received a new channel to monitor 
```
docker exec kafka /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic epics-channels --from-beginning --property "print.key=true" --property "key.separator==" 
```

**See**: [Docker Compose Strategy](https://gist.github.com/slominskir/a7da801e8259f5974c978f9c3091d52c)

## Install
This application requires a Java 8+ JVM and standard library to run.

Download from [Releases](https://github.com/JeffersonLab/registrations2epics/releases) or [build](https://github.com/JeffersonLab/registrations2epics#build) yourself.

Start scripts are created and dependencies collected by the Gradle distribution target:
```
gradlew assembleDist
```

Launch with:

UNIX:
```
bin/regisrations2epics
```
Windows:
```
bin/registrations2epics.bat
```

## Configure
Environment Variables

| Name | Description |
|---|---|
| BOOTSTRAP_SERVERS | Comma-separated list of host and port pairs pointing to a Kafka server to bootstrap the client connection to a Kafka Cluser; example: `kafka:9092` |
| SCHEMA_REGISTRY | URL to Confluent Schema Registry; example: `http://registry:8081` |

## Build
This project is built with [Java 17](https://adoptium.net/) (compiled to Java 8 bytecode), and uses the [Gradle 7](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/registrations2epics
cd registrations2epics
gradlew build
```
**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note for JLab On-Site Users**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)

**See**: [Docker Development Quick Reference](https://gist.github.com/slominskir/a7da801e8259f5974c978f9c3091d52c#development-quick-reference)

## See Also
   - [Developer Notes](https://github.com/JeffersonLab/registrations2epics/wiki/Developer-Notes)
