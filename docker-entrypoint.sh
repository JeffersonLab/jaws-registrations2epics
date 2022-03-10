#!/bin/sh

echo "--------------------------------------------------------------------"
echo "Step 1: Waiting for alarm-instances-value in Schema Registry"
echo "--------------------------------------------------------------------"
url=$SCHEMA_REGISTRY
echo "waiting on: $url"
while [ $(curl -s -o /dev/null -w %{http_code} $url/subjects/alarm-instances-value/versions) -ne 200 ] ; do
  echo -e $(date) " Kafka Registry listener HTTP state: " $(curl -s -o /dev/null -w %{http_code} $url/subjects/alarm-instances-value/versions) " (waiting for 200)"
  sleep 5
done

export REGISTRATIONS2EPICS_OPTS=-Dlog.dir=/opt/registrations2epics/logs
/opt/registrations2epics/bin/registrations2epics &

sleep infinity