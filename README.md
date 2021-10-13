# registrations2epics [![Java CI with Gradle](https://github.com/JeffersonLab/registrations2epics/workflows/Java%20CI%20with%20Gradle/badge.svg)](https://github.com/JeffersonLab/registrations2epics/actions?query=workflow%3A%22Java+CI+with+Gradle%22) [![Docker Image Version (latest semver)](https://img.shields.io/docker/v/slominskir/registrations2epics?sort=semver&label=DockerHub)   ](https://hub.docker.com/r/slominskir/registrations2epics)
A [Kafka Streams](https://kafka.apache.org/documentation/streams/) application to continuously populate the [epics2kafka](https://github.com/JeffersonLab/epics2kafka) _epics-channels_ topic from the [JAWS](https://github.com/JeffersonLab/jaws) _alarm-registrations_ topic for the subset of messages containing producer type __EPICSProducer__.  

---
 - [Quick Start with Compose](https://github.com/JeffersonLab/registrations2epics#quick-start-with-compose)
 - [Build](https://github.com/JeffersonLab/registrations2epics#build)
 - [Configure](https://github.com/JeffersonLab/registrations2epics#configure)
 - [Deploy](https://github.com/JeffersonLab/registrations2epics#deploy)
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
docker exec -it jaws /scripts/client/set-registered.py alarm1 --producerpv channel1 --location INJ --category RF --screenpath / 
```
4. Verify that the epics-channels command topic received a new channel to monitor 
```
docker exec kafka /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic epics-channels --from-beginning --property "print.key=true" --property "key.separator==" 
```

**Note**: When developing the app you can mount the build artifact into the container by substituting the `docker-compose up` command with:
```
docker-compose -f docker-compose.yml -f docker-compose-dev.yml up
```

## Build
This [Java 11](https://adoptopenjdk.net/) project uses the [Gradle 6](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/registrations2epics
cd registrations2epics
gradlew build
```
**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)

## Configure
Environment Variables

| Name | Description |
|---|---|
| BOOTSTRAP_SERVERS | Comma-separated list of host and port pairs pointing to a Kafka server to bootstrap the client connection to a Kafka Cluser; example: `kafka:9092` |
| SCHEMA_REGISTRY | URL to Confluent Schema Registry; example: `http://registry:8081` |

## Deploy
The Kafka Streams app is a regular Java application, and start scripts are created and dependencies collected by the Gradle distribution targets:

```
gradlew assembleDist
```

[Releases](https://github.com/JeffersonLab/registrations2epics/releases)

Launch with:

UNIX:
```
bin/regisrations2epics
```
Windows:
```
bin/registrations2epics.bat
```


## Docker
```
docker pull slominskir/registrations2epics
```
Image hosted on [DockerHub](https://hub.docker.com/r/slominskir/registrations2epics)

## See Also
   - [Developer Notes](https://github.com/JeffersonLab/registrations2epics/wiki/Developer-Notes)
