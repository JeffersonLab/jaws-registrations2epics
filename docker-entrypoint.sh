#!/bin/bash

echo "------------------------------------------------------"
echo "Step 1: Waiting for Schema Registry to start listening"
echo "------------------------------------------------------"
url=$SCHEMA_REGISTRY
echo "waiting on: $url"
while [ $(curl -s -o /dev/null -w %{http_code} $url/subjects) -eq 000 ] ; do
  echo -e $(date) " Kafka Registry listener HTTP state: " $(curl -s -o /dev/null -w %{http_code} $url/subjects) " (waiting for 200)"
  sleep 5
done

/opt/kafka-streams-epics-alarms/bin/kafka-streams-epics-alarms