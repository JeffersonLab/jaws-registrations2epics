FROM gradle:6.5.1-jdk8 as builder

ARG CUSTOM_CRT_URL

RUN git clone https://github.com/JeffersonLab/kafka-streams-epics-alarms \
    && cd ./kafka-streams-epics-alarms \
    && gradle build -x test \
    && cp -r ./build/install/* /opt \
    && cp /docker-entrypoint.sh /

ENTRYPOINT ["/docker-entrypoint.sh"]