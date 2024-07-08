package org.jlab.jaws.util;

import java.util.*;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;

public class ListTopic {
  public static void main(String[] args) throws ExecutionException, InterruptedException {

    String bootstrapServers = System.getenv("BOOTSTRAP_SERVERS");

    if (bootstrapServers == null) {
      bootstrapServers = "localhost:9094";
    }

    System.err.println("Using BOOTSTRAP_SERVERS = " + bootstrapServers);

    Map<String, Object> conf = new HashMap<>();
    conf.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    conf.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
    AdminClient client = AdminClient.create(conf);

    ListTopicsResult ltr = client.listTopics();
    KafkaFuture<Set<String>> names = ltr.names();
    Set<String> nameSet = names.get();

    System.out.println(nameSet);

    for (String name : nameSet) {
      if ("alarm-instances".equals(name)) {
        System.out.println("Topic 'alarm-instances' exists!");
        System.exit(0);
      }
    }

    System.exit(1);
  }
}
