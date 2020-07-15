FROM gradle:6.5.1-jdk8

ARG CUSTOM_CRT_URL

RUN git clone https://github.com/JeffersonLab/kafka-streams-epics-alarms \
    && cd ./kafka-streams-epics-alarms

WORKDIR /

ENTRYPOINT ["bash"]