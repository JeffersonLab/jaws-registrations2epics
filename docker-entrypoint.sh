#!/bin/sh

echo "-------------------------------------------------------"
echo "Step 1: Waiting for Schema Registry to start listening "
echo "-------------------------------------------------------"
while [ $(curl -s -o /dev/null -w %{http_code} $SCHEMA_REGISTRY/schemas/types) -eq 000 ] ; do
  echo -e $(date) " Registry listener HTTP state: " $(curl -s -o /dev/null -w %{http_code} $SCHEMA_REGISTRY/schemas/types) " (waiting for 200)"
  sleep 5
done


# TODO: We must wait for jaws container to create "alarm-instances" topic else streams app will fail.
# For now we use a fixed sleep, but ideally we use a docker healthcheck or else query Kafka for needed / loop / sleep
# Also note we currently don't have output topic 'epics-channels' either.  Missing output isn't fatal though.
sleep 5

export JAWS_REGISTRATIONS2EPICS_OPTS=-Dlog.dir=/opt/jaws-registrations2epics/logs
/opt/jaws-registrations2epics/bin/jaws-registrations2epics &

sleep infinity