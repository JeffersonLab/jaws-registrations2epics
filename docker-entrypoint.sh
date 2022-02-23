#!/bin/bash

echo "--------------------------------------------------------------------"
echo "Step 1: Waiting for effective-registrations-value in Schema Registry"
echo "--------------------------------------------------------------------"
url=$SCHEMA_REGISTRY
echo "waiting on: $url"
while [ $(curl -s -o /dev/null -w %{http_code} $url/subjects/effective-registrations-value/versions) -ne 200 ] ; do
  echo -e $(date) " Kafka Registry listener HTTP state: " $(curl -s -o /dev/null -w %{http_code} $url/subjects/effective-registrations-value/versions) " (waiting for 200)"
  sleep 5
done

/opt/registrations2epics/bin/registrations2epics