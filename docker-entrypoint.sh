#!/bin/sh

echo "-------------------------------------------------------"
echo "Step 1: Waiting for Schema Registry to start listening "
echo "-------------------------------------------------------"
while [ $(curl -s -o /dev/null -w %{http_code} $SCHEMA_REGISTRY/schemas/types) -eq 000 ] ; do
  echo -e $(date) " Registry listener HTTP state: " $(curl -s -o /dev/null -w %{http_code} $SCHEMA_REGISTRY/schemas/types) " (waiting for 200)"
  sleep 5
done


# We must wait for jaws container to create "alarms" topic else streams app will fail.
# Optionally create output topic 'epics-channels'.  Missing output isn't fatal.

APP_HOME=/opt/jaws-registrations2epics
CLASSPATH=$APP_HOME/lib/jaws-registrations2epics.jar:$APP_HOME/lib/kafka-streams-3.5.0.jar:$APP_HOME/lib/kafka-streams-avro-serde-7.4.0.jar:$APP_HOME/lib/jaws-libj-4.6.0.jar:$APP_HOME/lib/kafka-avro-serializer-7.4.0.jar:$APP_HOME/lib/kafka-schema-serializer-7.4.0.jar:$APP_HOME/lib/kafka-schema-registry-client-7.4.0.jar:$APP_HOME/lib/avro-1.11.2.jar:$APP_HOME/lib/kafka-common-1.2.0.jar:$APP_HOME/lib/common-utils-7.4.0.jar:$APP_HOME/lib/slf4j-reload4j-1.7.36.jar:$APP_HOME/lib/kafka-clients-7.4.0-ccs.jar:$APP_HOME/lib/slf4j-api-1.7.36.jar:$APP_HOME/lib/jackson-core-2.14.2.jar:$APP_HOME/lib/jackson-databind-2.14.2.jar:$APP_HOME/lib/jackson-annotations-2.14.2.jar:$APP_HOME/lib/rocksdbjni-7.1.2.jar:$APP_HOME/lib/commons-compress-1.22.jar:$APP_HOME/lib/zstd-jni-1.5.2-1.jar:$APP_HOME/lib/lz4-java-1.8.0.jar:$APP_HOME/lib/snappy-java-1.1.8.4.jar:$APP_HOME/lib/guava-30.1.1-jre.jar:$APP_HOME/lib/logredactor-1.0.11.jar:$APP_HOME/lib/snakeyaml-2.0.jar:$APP_HOME/lib/swagger-annotations-2.1.10.jar:$APP_HOME/lib/reload4j-1.2.19.jar:$APP_HOME/lib/log4j-1.2.17.jar:$APP_HOME/lib/failureaccess-1.0.1.jar:$APP_HOME/lib/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar:$APP_HOME/lib/jsr305-3.0.2.jar:$APP_HOME/lib/checker-qual-3.8.0.jar:$APP_HOME/lib/error_prone_annotations-2.5.1.jar:$APP_HOME/lib/j2objc-annotations-1.3.jar:$APP_HOME/lib/re2j-1.6.jar:$APP_HOME/lib/logredactor-metrics-1.0.11.jar:$APP_HOME/lib/minimal-json-0.9.5.jar:$APP_HOME/config

echo "-----------------------------------------------"
echo "Step 2: Waiting for Topic alarms to be created "
echo "-----------------------------------------------"
while ! java -Dlog.dir=/opt/jaws-registrations2epics/logs -cp $CLASSPATH org.jlab.jaws.util.ListTopic; do
  echo -e $(date) " Waiting for alarms topic to exist"
  sleep 5
done

if [[ -n ${CREATE_SOURCE_TOPIC} ]]; then
  java -Dlog.dir=/opt/jaws-registrations2epics/logs -cp $CLASSPATH org.jlab.jaws.util.CreateTopic
fi

export JAWS_REGISTRATIONS2EPICS_OPTS=-Dlog.dir=/opt/jaws-registrations2epics/logs
/opt/jaws-registrations2epics/bin/jaws-registrations2epics &

sleep infinity