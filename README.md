# jaws-registrations2epics [![CI](https://github.com/JeffersonLab/jaws-registrations2epics/actions/workflows/ci.yaml/badge.svg)](https://github.com/JeffersonLab/jaws-registrations2epics/actions/workflows/ci.yaml) [![Docker Image Version (latest semver)](https://img.shields.io/docker/v/jeffersonlab/jaws-registrations2epics?sort=semver&label=DockerHub)   ](https://hub.docker.com/r/jeffersonlab/jaws-registrations2epics)
A [Kafka Streams](https://kafka.apache.org/documentation/streams/) application to continuously populate the [epics2kafka](https://github.com/JeffersonLab/epics2kafka) _epics-channels_ topic from the [JAWS](https://github.com/JeffersonLab/jaws) _alarm-instances_ topic for the subset of messages containing source type __EPICSSource__.  

---
 - [Overview](https://github.com/JeffersonLab/jaws-registrations2epics#overview)
 - [Quick Start with Compose](https://github.com/JeffersonLab/jaws-registrations2epics#quick-start-with-compose)
 - [Install](https://github.com/JeffersonLab/jaws-registrations2epics#install)
 - [Configure](https://github.com/JeffersonLab/jaws-registrations2epics#configure)
 - [Build](https://github.com/JeffersonLab/jaws-registrations2epics#build) 
 - [Develop](https://github.com/JeffersonLab/jaws-registration2epics#develop)
 - [Release](https://github.com/JeffersonLab/jaws-registrations2epics#release)  
 - [See Also](https://github.com/JeffersonLab/jaws-registrations2epics#see-also)
 ---

## Overview
This app keeps epics2kafka automatically configured based on the JAWS configuration.  This Kafka Streams app consumes the `alarm-instances` topic and looks for registration messages related to EPICS and for each of those produces a command message to epics2kafka via the `epics-channels` topic.   

When a JAWS registration is removed it is removed via a tombstone message, which is a null value for a given key.  This presents a challenge as a registration removal does not have a value payload to indicate whether it is an EPICS registration or what is the PV/channel name.  The epics2kafka epics-channels topic key contains a PV/channel whereas the JAWS alarm-instances key is an alarm name.   To overcome this challenge this app is not stateless, it uses a Kafka Streams store to track all JAWS registration records that have been used to command epics2kafka.  This way the registration record key, the alarm name, can be used in the tombstone case to lookup if the tombstone means action is needed.

## Quick Start with Compose 
1. Grab project
```
git clone https://github.com/JeffersonLab/jaws-registrations2epics
cd jaws-registrations2epics
```
2. Launch [Compose](https://github.com/docker/compose)
```
docker compose up
```
3. Monitor the epics-channels command topic for updated channels to monitor 
```
docker exec kafka kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic epics-channels --from-beginning --property "print.key=true" --property "key.separator==" 
```
4. Register an alarm
```
docker exec -it cli set_instance alarm1 --pv channel1 
```

**See**: [Docker Compose Strategy](https://gist.github.com/slominskir/a7da801e8259f5974c978f9c3091d52c)

## Install
This application requires a Java 11+ JVM and standard library to run.

Download from [Releases](https://github.com/JeffersonLab/jaws-registrations2epics/releases) or [build](https://github.com/JeffersonLab/jaws-registrations2epics#build) yourself.

Start scripts are created and dependencies collected by the Gradle distribution target:
```
gradlew assembleDist
```

Launch with:

UNIX:
```
bin/jaws-regisrations2epics
```
Windows:
```
bin/jaws-registrations2epics.bat
```

## Configure
Environment Variables

| Name | Description |
|---|---|
| BOOTSTRAP_SERVERS | Comma-separated list of host and port pairs pointing to a Kafka server to bootstrap the client connection to a Kafka Cluser; example: `kafka:9092` |
| SCHEMA_REGISTRY | URL to Confluent Schema Registry; example: `http://registry:8081` |

## Build
This project is built with [Java 17](https://adoptium.net/) (compiled to Java 11 bytecode), and uses the [Gradle 7](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/registrations2epics
cd registrations2epics
gradlew build
```
**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note for JLab On-Site Users**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)

**See**: [Docker Development Quick Reference](https://gist.github.com/slominskir/a7da801e8259f5974c978f9c3091d52c#development-quick-reference)

## Develop
In order to iterate rapidly when making changes it's often useful to run the app directly on the local workstation, perhaps leveraging an IDE. In this scenario run the service dependencies with:
```
docker compose -f deps.yaml up
```
Then run the app with:
```
gradlew run
```

**Note**: The `STATE_DIR` config is set to the gradle `build` dir such that running a `clean` task will clear the local state.  You may need to reset the Kafka server state after running a clean task by restarting Kafka from scratch else using the [Reset Tool](https://kafka.apache.org//documentation/streams/developer-guide/app-reset-tool).

**Note**: Javadocs can be generated with the command:
```
gradlew javadoc
```

## Release
1. Bump the version number in the VERSION file and commit and push to GitHub (using [Semantic Versioning](https://semver.org/)).
2. The [CD](https://github.com/JeffersonLab/jaws-registrations2epics/blob/main/.github/workflows/cd.yaml) GitHub Action should run automatically invoking:
    - The [Create release](https://github.com/JeffersonLab/java-workflows/blob/main/.github/workflows/gh-release.yaml) GitHub Action to tag the source and create release notes summarizing any pull requests.   Edit the release notes to add any missing details.  A zip file artifact is attached to the release.
    - The [Publish docker image](https://github.com/JeffersonLab/container-workflows/blob/main/.github/workflows/docker-publish.yaml) GitHub Action to create a new demo Docker image.


## See Also
   - [Developer Notes](https://github.com/JeffersonLab/jaws-registrations2epics/wiki/Developer-Notes)
