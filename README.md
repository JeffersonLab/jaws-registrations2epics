# kafka-streams-epics-alarms
A [Kafka Streams](https://kafka.apache.org/documentation/streams/) application to continuously populate the [epics2kafka](https://github.com/JeffersonLab/epics2kafka) _epics-channels_ topic from the [kafka-alarm-system](https://github.com/JeffersonLab/kafka-alarm-system) _registered-alarms_ topic given messages containing producer type _epics2kafka_.  

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
3. Register an alarm
```
docker exec -it console /scripts/registered-alarms/set-registered.py channel1 --producerpv channel1 --location INJ --category RF --docurl / --edmpath / 
```
4. Verify that the epics-channels command topic recieved a new channel to monitor 
```
docker exec kafka /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic epics-channels --from-beginning --property "print.key=true" --property "key.separator==" 
```
