/*
 * Copyright 2020 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.kafkarest.integration.v3;

import static io.confluent.kafkarest.TestUtils.TEST_WITH_PARAMETERIZED_QUORUM_NAME;
import static io.confluent.kafkarest.TestUtils.testWithRetry;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.confluent.kafkarest.entities.ConfigSource;
import io.confluent.kafkarest.entities.v3.ConfigSynonymData;
import io.confluent.kafkarest.entities.v3.GetTopicConfigResponse;
import io.confluent.kafkarest.entities.v3.ListTopicConfigsResponse;
import io.confluent.kafkarest.entities.v3.Resource;
import io.confluent.kafkarest.entities.v3.ResourceCollection;
import io.confluent.kafkarest.entities.v3.TopicConfigData;
import io.confluent.kafkarest.integration.ClusterTestHarness;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TopicConfigsResourceIntegrationTest extends ClusterTestHarness {

  private static final String TOPIC_1 = "topic-1";

  public TopicConfigsResourceIntegrationTest() {
    super(/* numBrokers= */ 1, /* withSchemaRegistry= */ false);
  }

  @BeforeEach
  @Override
  public void setUp(TestInfo testInfo) throws Exception {
    super.setUp(testInfo);

    createTopic(TOPIC_1, 1, (short) 1);
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void listTopicConfigs_existingTopic_returnsConfigs(String quorum) {
    String baseUrl = restConnect;
    String clusterId = getClusterId();

    ResourceCollection.Metadata expectedMetadata =
        ResourceCollection.Metadata.builder()
            .setSelf(baseUrl + "/v3/clusters/" + clusterId + "/topics/" + TOPIC_1 + "/configs")
            .build();

    TopicConfigData expectedConfig1 =
        TopicConfigData.builder()
            .setMetadata(
                Resource.Metadata.builder()
                    .setSelf(
                        baseUrl
                            + "/v3/clusters/"
                            + clusterId
                            + "/topics/"
                            + TOPIC_1
                            + "/configs/cleanup.policy")
                    .setResourceName(
                        "crn:///kafka="
                            + clusterId
                            + "/topic="
                            + TOPIC_1
                            + "/config=cleanup.policy")
                    .build())
            .setClusterId(clusterId)
            .setTopicName(TOPIC_1)
            .setName("cleanup.policy")
            .setValue("delete")
            .setDefault(true)
            .setReadOnly(false)
            .setSensitive(false)
            .setSource(ConfigSource.DEFAULT_CONFIG)
            .setSynonyms(
                singletonList(
                    ConfigSynonymData.builder()
                        .setName("log.cleanup.policy")
                        .setValue("delete")
                        .setSource(ConfigSource.DEFAULT_CONFIG)
                        .build()))
            .build();
    TopicConfigData expectedConfig2 =
        TopicConfigData.builder()
            .setMetadata(
                Resource.Metadata.builder()
                    .setSelf(
                        baseUrl
                            + "/v3/clusters/"
                            + clusterId
                            + "/topics/"
                            + TOPIC_1
                            + "/configs/compression.type")
                    .setResourceName(
                        "crn:///kafka="
                            + clusterId
                            + "/topic="
                            + TOPIC_1
                            + "/config=compression.type")
                    .build())
            .setClusterId(clusterId)
            .setTopicName(TOPIC_1)
            .setName("compression.type")
            .setValue("producer")
            .setDefault(true)
            .setReadOnly(false)
            .setSensitive(false)
            .setSource(ConfigSource.DEFAULT_CONFIG)
            .setSynonyms(
                singletonList(
                    ConfigSynonymData.builder()
                        .setName("compression.type")
                        .setValue("producer")
                        .setSource(ConfigSource.DEFAULT_CONFIG)
                        .build()))
            .build();
    TopicConfigData expectedConfig3 =
        TopicConfigData.builder()
            .setMetadata(
                Resource.Metadata.builder()
                    .setSelf(
                        baseUrl
                            + "/v3/clusters/"
                            + clusterId
                            + "/topics/"
                            + TOPIC_1
                            + "/configs/delete.retention.ms")
                    .setResourceName(
                        "crn:///kafka="
                            + clusterId
                            + "/topic="
                            + TOPIC_1
                            + "/config=delete.retention.ms")
                    .build())
            .setClusterId(clusterId)
            .setTopicName(TOPIC_1)
            .setName("delete.retention.ms")
            .setValue("86400000")
            .setDefault(true)
            .setReadOnly(false)
            .setSensitive(false)
            .setSource(ConfigSource.DEFAULT_CONFIG)
            .setSynonyms(
                singletonList(
                    ConfigSynonymData.builder()
                        .setName("log.cleaner.delete.retention.ms")
                        .setValue("86400000")
                        .setSource(ConfigSource.DEFAULT_CONFIG)
                        .build()))
            .build();

    Response response =
        request("/v3/clusters/" + clusterId + "/topics/" + TOPIC_1 + "/configs")
            .accept(MediaType.APPLICATION_JSON)
            .get();
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    ListTopicConfigsResponse responseBody = response.readEntity(ListTopicConfigsResponse.class);
    assertEquals(expectedMetadata, responseBody.getValue().getMetadata());
    assertTrue(
        responseBody.getValue().getData().contains(expectedConfig1),
        String.format("Not true that `%s' contains `%s'.", responseBody, expectedConfig1));
    assertTrue(
        responseBody.getValue().getData().contains(expectedConfig2),
        String.format("Not true that `%s' contains `%s'.", responseBody, expectedConfig2));
    assertTrue(
        responseBody.getValue().getData().contains(expectedConfig3),
        String.format("Not true that `%s' contains `%s'.", responseBody, expectedConfig3));
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void listTopicConfigs_nonExistingTopic_throwsNotFound(String quorum) {
    String clusterId = getClusterId();

    Response response =
        request("/v3/clusters/" + clusterId + "/topics/foobar/configs")
            .accept(MediaType.APPLICATION_JSON)
            .get();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void listTopicConfigs_nonExistingCluster_throwsNotFound(String quorum) {
    Response response =
        request("/v3/clusters/foobar/topics/" + TOPIC_1 + "/configs")
            .accept(MediaType.APPLICATION_JSON)
            .get();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void getTopicConfig_existingConfig_returnsConfig(String quorum) {
    String baseUrl = restConnect;
    String clusterId = getClusterId();

    GetTopicConfigResponse expected =
        GetTopicConfigResponse.create(
            TopicConfigData.builder()
                .setMetadata(
                    Resource.Metadata.builder()
                        .setSelf(
                            baseUrl
                                + "/v3/clusters/"
                                + clusterId
                                + "/topics/"
                                + TOPIC_1
                                + "/configs/cleanup.policy")
                        .setResourceName(
                            "crn:///kafka="
                                + clusterId
                                + "/topic="
                                + TOPIC_1
                                + "/config=cleanup.policy")
                        .build())
                .setClusterId(clusterId)
                .setTopicName(TOPIC_1)
                .setName("cleanup.policy")
                .setValue("delete")
                .setDefault(true)
                .setReadOnly(false)
                .setSensitive(false)
                .setSource(ConfigSource.DEFAULT_CONFIG)
                .setSynonyms(
                    singletonList(
                        ConfigSynonymData.builder()
                            .setName("log.cleanup.policy")
                            .setValue("delete")
                            .setSource(ConfigSource.DEFAULT_CONFIG)
                            .build()))
                .build());

    Response response =
        request("/v3/clusters/" + clusterId + "/topics/" + TOPIC_1 + "/configs/cleanup.policy")
            .accept(MediaType.APPLICATION_JSON)
            .get();
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    GetTopicConfigResponse actual = response.readEntity(GetTopicConfigResponse.class);
    assertEquals(expected, actual);
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void getTopicConfig_nonExistingConfig_throwsNotFound(String quorum) {
    String clusterId = getClusterId();

    Response response =
        request("/v3/clusters/" + clusterId + "/topics/" + TOPIC_1 + "/configs/foobar")
            .accept(MediaType.APPLICATION_JSON)
            .get();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void getTopicConfig_nonExistingTopic_throwsNotFound(String quorum) {
    String clusterId = getClusterId();

    Response response =
        request("/v3/clusters/" + clusterId + "/topics/foobar/configs/cleanup.policy")
            .accept(MediaType.APPLICATION_JSON)
            .get();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void getTopicConfig_nonExistingCluster_throwsNotFound(String quorum) {
    Response response =
        request("/v3/clusters/foobar/topics/" + TOPIC_1 + "/configs/cleanup.policy")
            .accept(MediaType.APPLICATION_JSON)
            .get();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void getUpdateReset_withExistingConfig(String quorum) {
    String baseUrl = restConnect;
    String clusterId = getClusterId();

    GetTopicConfigResponse expectedBeforeUpdate =
        GetTopicConfigResponse.create(
            TopicConfigData.builder()
                .setMetadata(
                    Resource.Metadata.builder()
                        .setSelf(
                            baseUrl
                                + "/v3/clusters/"
                                + clusterId
                                + "/topics/"
                                + TOPIC_1
                                + "/configs/cleanup.policy")
                        .setResourceName(
                            "crn:///kafka="
                                + clusterId
                                + "/topic="
                                + TOPIC_1
                                + "/config=cleanup.policy")
                        .build())
                .setClusterId(clusterId)
                .setTopicName(TOPIC_1)
                .setName("cleanup.policy")
                .setValue("delete")
                .setDefault(true)
                .setReadOnly(false)
                .setSensitive(false)
                .setSource(ConfigSource.DEFAULT_CONFIG)
                .setSynonyms(
                    singletonList(
                        ConfigSynonymData.builder()
                            .setName("log.cleanup.policy")
                            .setValue("delete")
                            .setSource(ConfigSource.DEFAULT_CONFIG)
                            .build()))
                .build());

    Response responseBeforeUpdate =
        request("/v3/clusters/" + clusterId + "/topics/" + TOPIC_1 + "/configs/cleanup.policy")
            .accept(MediaType.APPLICATION_JSON)
            .get();
    assertEquals(Status.OK.getStatusCode(), responseBeforeUpdate.getStatus());

    GetTopicConfigResponse actualResponseBeforeUpdate =
        responseBeforeUpdate.readEntity(GetTopicConfigResponse.class);

    assertEquals(expectedBeforeUpdate, actualResponseBeforeUpdate);

    Response updateResponse =
        request("/v3/clusters/" + clusterId + "/topics/" + TOPIC_1 + "/configs/cleanup.policy")
            .accept(MediaType.APPLICATION_JSON)
            .put(Entity.entity("{\"value\":\"compact\"}", MediaType.APPLICATION_JSON));
    assertEquals(Status.NO_CONTENT.getStatusCode(), updateResponse.getStatus());

    GetTopicConfigResponse expectedAfterUpdate =
        GetTopicConfigResponse.create(
            TopicConfigData.builder()
                .setMetadata(
                    Resource.Metadata.builder()
                        .setSelf(
                            baseUrl
                                + "/v3/clusters/"
                                + clusterId
                                + "/topics/"
                                + TOPIC_1
                                + "/configs/cleanup.policy")
                        .setResourceName(
                            "crn:///kafka="
                                + clusterId
                                + "/topic="
                                + TOPIC_1
                                + "/config=cleanup.policy")
                        .build())
                .setClusterId(clusterId)
                .setTopicName(TOPIC_1)
                .setName("cleanup.policy")
                .setValue("compact")
                .setDefault(false)
                .setReadOnly(false)
                .setSensitive(false)
                .setSource(ConfigSource.DYNAMIC_TOPIC_CONFIG)
                .setSynonyms(
                    Arrays.asList(
                        ConfigSynonymData.builder()
                            .setName("cleanup.policy")
                            .setValue("compact")
                            .setSource(ConfigSource.DYNAMIC_TOPIC_CONFIG)
                            .build(),
                        ConfigSynonymData.builder()
                            .setName("log.cleanup.policy")
                            .setValue("delete")
                            .setSource(ConfigSource.DEFAULT_CONFIG)
                            .build()))
                .build());

    testWithRetry(
        () -> {
          Response responseAfterUpdate =
              request(
                      "/v3/clusters/"
                          + clusterId
                          + "/topics/"
                          + TOPIC_1
                          + "/configs/cleanup.policy")
                  .accept(MediaType.APPLICATION_JSON)
                  .get();
          assertEquals(Status.OK.getStatusCode(), responseAfterUpdate.getStatus());

          GetTopicConfigResponse actualResponseAfterUpdate =
              responseAfterUpdate.readEntity(GetTopicConfigResponse.class);
          assertEquals(expectedAfterUpdate, actualResponseAfterUpdate);
        });

    Response resetResponse =
        request("/v3/clusters/" + clusterId + "/topics/" + TOPIC_1 + "/configs/cleanup.policy")
            .accept(MediaType.APPLICATION_JSON)
            .delete();
    assertEquals(Status.NO_CONTENT.getStatusCode(), resetResponse.getStatus());

    GetTopicConfigResponse expectedAfterReset =
        GetTopicConfigResponse.create(
            TopicConfigData.builder()
                .setMetadata(
                    Resource.Metadata.builder()
                        .setSelf(
                            baseUrl
                                + "/v3/clusters/"
                                + clusterId
                                + "/topics/"
                                + TOPIC_1
                                + "/configs/cleanup.policy")
                        .setResourceName(
                            "crn:///kafka="
                                + clusterId
                                + "/topic="
                                + TOPIC_1
                                + "/config=cleanup.policy")
                        .build())
                .setClusterId(clusterId)
                .setTopicName(TOPIC_1)
                .setName("cleanup.policy")
                .setValue("delete")
                .setDefault(true)
                .setReadOnly(false)
                .setSensitive(false)
                .setSource(ConfigSource.DEFAULT_CONFIG)
                .setSynonyms(
                    singletonList(
                        ConfigSynonymData.builder()
                            .setName("log.cleanup.policy")
                            .setValue("delete")
                            .setSource(ConfigSource.DEFAULT_CONFIG)
                            .build()))
                .build());

    testWithRetry(
        () -> {
          Response responseAfterReset =
              request(
                      "/v3/clusters/"
                          + clusterId
                          + "/topics/"
                          + TOPIC_1
                          + "/configs/cleanup.policy")
                  .accept(MediaType.APPLICATION_JSON)
                  .get();
          assertEquals(Status.OK.getStatusCode(), responseAfterReset.getStatus());

          GetTopicConfigResponse actualResponseAfterReset =
              responseAfterReset.readEntity(GetTopicConfigResponse.class);
          assertEquals(expectedAfterReset, actualResponseAfterReset);
        });
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void updateTopicConfig_nonExistingConfig_throwsNotFound(String quorum) {
    String clusterId = getClusterId();

    Response response =
        request("/v3/clusters/" + clusterId + "/topics/" + TOPIC_1 + "/configs/foobar")
            .accept(MediaType.APPLICATION_JSON)
            .put(Entity.entity("{\"value\":\"compact\"}", MediaType.APPLICATION_JSON));
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void updateTopicConfig_nonExistingTopic_throwsNotFound(String quorum) {
    String clusterId = getClusterId();

    Response response =
        request("/v3/clusters/" + clusterId + "/topics/foobar/configs/cleanup.policy")
            .accept(MediaType.APPLICATION_JSON)
            .put(Entity.entity("{\"value\":\"compact\"}", MediaType.APPLICATION_JSON));
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void updateTopicConfig_nonExistingCluster_throwsNotFound(String quorum) {
    Response response =
        request("/v3/clusters/foobar/topics/" + TOPIC_1 + "/configs/cleanup.policy")
            .accept(MediaType.APPLICATION_JSON)
            .put(Entity.entity("{\"value\":\"compact\"}", MediaType.APPLICATION_JSON));
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void updateTopicConfig_nonExistingCluster_noContentType_throwsNotFound(String quorum) {
    Response response =
        request("/v3/clusters/foobar/topics/" + TOPIC_1 + "/configs/cleanup.policy")
            .put(Entity.entity("{\"value\":\"compact\"}", MediaType.APPLICATION_JSON));
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void resetTopicConfig_nonExistingConfig_throwsNotFound(String quorum) {
    String clusterId = getClusterId();

    Response response =
        request("/v3/clusters/" + clusterId + "/topics/" + TOPIC_1 + "/configs/foobar")
            .accept(MediaType.APPLICATION_JSON)
            .delete();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void resetTopicConfig_nonExistingTopic_throwsNotFound(String quorum) {
    String clusterId = getClusterId();

    Response response =
        request("/v3/clusters/" + clusterId + "/topics/foobar/configs/cleanup.policy")
            .accept(MediaType.APPLICATION_JSON)
            .delete();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void resetTopicConfig_nonExistingCluster_throwsNotFound(String quorum) {
    Response response =
        request("/v3/clusters/foobar/topics/" + TOPIC_1 + "/configs/cleanup.policy")
            .accept(MediaType.APPLICATION_JSON)
            .delete();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void resetTopicConfig_nonExistingCluster_noContentType_throwsNotFound(String quorum) {
    Response response =
        request("/v3/clusters/foobar/topics/" + TOPIC_1 + "/configs/cleanup.policy").delete();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void alterConfigBatch_withExistingConfig(String quorum) {
    String baseUrl = restConnect;
    String clusterId = getClusterId();

    Response updateResponse =
        request("/v3/clusters/" + clusterId + "/topics/" + TOPIC_1 + "/configs:alter")
            .accept(MediaType.APPLICATION_JSON)
            .post(
                Entity.entity(
                    "{\"data\":["
                        + "{\"name\": \"cleanup.policy\",\"value\":\"compact\"},"
                        + "{\"name\": \"compression.type\",\"value\":\"gzip\"}]}",
                    MediaType.APPLICATION_JSON));
    assertEquals(Status.NO_CONTENT.getStatusCode(), updateResponse.getStatus());

    GetTopicConfigResponse expectedAfterUpdate1 =
        GetTopicConfigResponse.create(
            TopicConfigData.builder()
                .setMetadata(
                    Resource.Metadata.builder()
                        .setSelf(
                            baseUrl
                                + "/v3/clusters/"
                                + clusterId
                                + "/topics/"
                                + TOPIC_1
                                + "/configs/cleanup.policy")
                        .setResourceName(
                            "crn:///kafka="
                                + clusterId
                                + "/topic="
                                + TOPIC_1
                                + "/config=cleanup.policy")
                        .build())
                .setClusterId(clusterId)
                .setTopicName(TOPIC_1)
                .setName("cleanup.policy")
                .setValue("compact")
                .setDefault(false)
                .setReadOnly(false)
                .setSensitive(false)
                .setSource(ConfigSource.DYNAMIC_TOPIC_CONFIG)
                .setSynonyms(
                    Arrays.asList(
                        ConfigSynonymData.builder()
                            .setName("cleanup.policy")
                            .setValue("compact")
                            .setSource(ConfigSource.DYNAMIC_TOPIC_CONFIG)
                            .build(),
                        ConfigSynonymData.builder()
                            .setName("log.cleanup.policy")
                            .setValue("delete")
                            .setSource(ConfigSource.DEFAULT_CONFIG)
                            .build()))
                .build());
    GetTopicConfigResponse expectedAfterUpdate2 =
        GetTopicConfigResponse.create(
            TopicConfigData.builder()
                .setMetadata(
                    Resource.Metadata.builder()
                        .setSelf(
                            baseUrl
                                + "/v3/clusters/"
                                + clusterId
                                + "/topics/"
                                + TOPIC_1
                                + "/configs/compression.type")
                        .setResourceName(
                            "crn:///kafka="
                                + clusterId
                                + "/topic="
                                + TOPIC_1
                                + "/config=compression.type")
                        .build())
                .setClusterId(clusterId)
                .setTopicName(TOPIC_1)
                .setName("compression.type")
                .setValue("gzip")
                .setDefault(false)
                .setReadOnly(false)
                .setSensitive(false)
                .setSource(ConfigSource.DYNAMIC_TOPIC_CONFIG)
                .setSynonyms(
                    Arrays.asList(
                        ConfigSynonymData.builder()
                            .setName("compression.type")
                            .setValue("gzip")
                            .setSource(ConfigSource.DYNAMIC_TOPIC_CONFIG)
                            .build(),
                        ConfigSynonymData.builder()
                            .setName("compression.type")
                            .setValue("producer")
                            .setSource(ConfigSource.DEFAULT_CONFIG)
                            .build()))
                .build());

    testWithRetry(
        () -> {
          Response responseAfterUpdate1 =
              request(
                      "/v3/clusters/"
                          + clusterId
                          + "/topics/"
                          + TOPIC_1
                          + "/configs/cleanup.policy")
                  .accept(MediaType.APPLICATION_JSON)
                  .get();
          assertEquals(Status.OK.getStatusCode(), responseAfterUpdate1.getStatus());
          GetTopicConfigResponse actualResponseAfterUpdate1 =
              responseAfterUpdate1.readEntity(GetTopicConfigResponse.class);
          assertEquals(expectedAfterUpdate1, actualResponseAfterUpdate1);
        });

    testWithRetry(
        () -> {
          Response responseAfterUpdate2 =
              request(
                      "/v3/clusters/"
                          + clusterId
                          + "/topics/"
                          + TOPIC_1
                          + "/configs/compression.type")
                  .accept(MediaType.APPLICATION_JSON)
                  .get();
          assertEquals(Status.OK.getStatusCode(), responseAfterUpdate2.getStatus());
          GetTopicConfigResponse actualResponseAfterUpdate2 =
              responseAfterUpdate2.readEntity(GetTopicConfigResponse.class);
          assertEquals(expectedAfterUpdate2, actualResponseAfterUpdate2);
        });
  }
}
