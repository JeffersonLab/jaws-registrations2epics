FROM gradle:6.5.1-jdk8 as builder

RUN git clone https://github.com/JeffersonLab/registrations2epics \
    && cd ./registrations2epics \
    && gradle build -x test \
    && cp -r ./build/install/* /opt \
    && cp ./docker-entrypoint.sh / \
    && chmod +x /docker-entrypoint.sh \
    && rm -rf /home/gradle/registrations2epics

WORKDIR /

ENTRYPOINT ["/docker-entrypoint.sh"]