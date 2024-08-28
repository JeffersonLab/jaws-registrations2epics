package org.jlab.jaws.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;

public class CreateTopic {
  public static void main(String[] args) throws ExecutionException, InterruptedException {
    createEpics2KafkaSourceTopic();
  }

  public static void createEpics2KafkaSourceTopic()
      throws ExecutionException, InterruptedException {
    String bootstrapServers = System.getenv("BOOTSTRAP_SERVERS");

    if (bootstrapServers == null) {
      bootstrapServers = "localhost:9094";
    }

    System.out.println("Using BOOTSTRAP_SERVERS = " + bootstrapServers);

    Map<String, Object> conf = new HashMap<>();
    conf.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    conf.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
    AdminClient client = AdminClient.create(conf);

    int partitions = 1;
    short replicationFactor = 1;

    KafkaFuture<Void> future =
        client
            .createTopics(
                Collections.singleton(
                    new NewTopic("epics-channels", partitions, replicationFactor)),
                new CreateTopicsOptions().timeoutMs(10000))
            .all();
    future.get();

    ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, "epics-channels");

    ConfigEntry entry =
        new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);
    Map<ConfigResource, Collection<AlterConfigOp>> updateConfig = new HashMap<>();
    updateConfig.put(
        resource, Collections.singleton(new AlterConfigOp(entry, AlterConfigOp.OpType.SET)));

    AlterConfigsResult alterConfigsResult = client.incrementalAlterConfigs(updateConfig);
    alterConfigsResult.all();

    System.out.println("Topic 'epics-channels' created");
  }
}
