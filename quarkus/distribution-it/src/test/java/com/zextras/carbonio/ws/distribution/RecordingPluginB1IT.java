// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.distribution;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.zextras.carbonio.ws.testsupport.Seed;
import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@TestMethodOrder(OrderAnnotation.class)
@ExtendWith(RecordingPluginB1IT.FailureFlag.class)
class RecordingPluginB1IT {

  static final String START_RECORDING = "/meetings/{meetingId}/startRecording";

  /** Metrics are only trustworthy when every step passed. */
  static volatile boolean anyTestFailed;

  static class FailureFlag implements TestWatcher {
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
      anyTestFailed = true;
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
      anyTestFailed = true;
    }
  }

  static final Path APP = Path.of(System.getProperty("app.dir"));
  static final Path RECORDING_JAR = Path.of(System.getProperty("recording.jar"));
  static final Path INCOMPATIBLE_JAR = Path.of(System.getProperty("incompatible.jar"));
  static final Path WORK = Path.of(System.getProperty("work.dir"));

  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");
  static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");
  static WireMockServer wm;
  static PGSimpleDataSource ds;
  static DistributionRunner dist;
  static int port;
  static final Properties metrics = new Properties();

  @BeforeAll
  static void setUp() throws IOException {
    Files.deleteIfExists(WORK.resolve("b1-metrics.properties"));
    postgres.start();
    rabbit.start();
    wm = new WireMockServer(options().dynamicPort());
    wm.start();
    Stubs.janus(wm);
    Stubs.activeUser(wm, TestUsers.OWNER, Map.of("carbonioWscRecordingEnabled", "true"));
    wm.stubFor(post(urlPathEqualTo("/internal/recordings/process")).willReturn(okJson("{}")));
    ds = new PGSimpleDataSource();
    ds.setUrl(postgres.getJdbcUrl());
    ds.setUser(postgres.getUsername());
    ds.setPassword(postgres.getPassword());
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    dist = DistributionRunner.freshCopy(APP, WORK.resolve("dist-b1"));
    metrics.setProperty("dist.size.mb", String.valueOf(sizeMb(APP)));
    metrics.setProperty("deployment.size.mb", String.valueOf(sizeMb(APP.resolve("lib/deployment"))));
  }

  /** A failed assertion must not leave the application running for the next test. */
  @AfterEach
  void stopApplication() throws InterruptedException {
    if (dist != null) {
      dist.stop();
    }
  }

  @AfterAll
  static void tearDown() throws Exception {
    try {
      if (dist != null) {
        dist.close();
      }
      if (!anyTestFailed) {
        try (var out = Files.newOutputStream(WORK.resolve("b1-metrics.properties"))) {
          metrics.store(out, "B1 measurements");
        }
      }
    } finally {
      try {
        if (wm != null) {
          wm.stop();
        }
      } finally {
        try {
          rabbit.stop();
        } finally {
          postgres.stop();
        }
      }
    }
  }

  static Map<String, String> props() {
    Map<String, String> p = new HashMap<>();
    p.put("quarkus.http.host", "127.0.0.1");
    p.put("quarkus.http.port", String.valueOf(port));
    p.put("quarkus.datasource.jdbc.url", postgres.getJdbcUrl());
    p.put("quarkus.datasource.username", postgres.getUsername());
    p.put("quarkus.datasource.password", postgres.getPassword());
    p.put("ws.consul.enabled", "false");
    p.put("ws.discovery.available-services", "none");
    p.put("ws.rabbitmq.host", rabbit.getHost());
    p.put("ws.rabbitmq.port", String.valueOf(rabbit.getAmqpPort()));
    p.put("ws.rabbitmq.username", rabbit.getAdminUsername());
    p.put("ws.rabbitmq.password", rabbit.getAdminPassword());
    p.put("ws.user-management.url", wm.baseUrl());
    p.put("ws.videoserver.url", wm.baseUrl());
    p.put("ws.videoserver.api-secret", "it-secret");
    p.put("ws.videoserver.server-id", "vs-1");
    p.put("ws.video-recorder.host", "localhost");
    p.put("ws.video-recorder.port", String.valueOf(wm.port()));
    return p;
  }

  static RequestSpecification as(UUID user) {
    return given().port(port).cookie("ZM_AUTH_TOKEN", TestUsers.token(user)).contentType(ContentType.JSON);
  }

  /** Seeds a room, creates/starts/joins a meeting through the API and returns its id. */
  static String activeMeetingWithOwner() {
    UUID roomId = UUID.randomUUID();
    Seed.room(ds, roomId, "group", Map.of(TestUsers.OWNER, true));
    String id = as(TestUsers.OWNER)
        .body(Map.of("name", "B1", "meetingType", "permanent", "roomId", roomId.toString()))
        .post("/meetings").then().statusCode(200).extract().path("id");
    as(TestUsers.OWNER).post("/meetings/" + id + "/start").then().statusCode(200);
    as(TestUsers.OWNER).post("/meetings/" + id + "/join").then().statusCode(200);
    return id;
  }

  static boolean tableExists(String table) {
    return Seed.count(ds,
        "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'chats' AND table_name = ?", table) == 1;
  }

  static long sizeMb(Path dir) throws IOException {
    try (Stream<Path> walk = Files.walk(dir)) {
      return walk.filter(Files::isRegularFile).mapToLong(p -> p.toFile().length()).sum() / (1024 * 1024);
    }
  }

  @Test
  @Order(1)
  void ceDistributionHasNoRecording() throws Exception {
    dist.start(props());
    metrics.setProperty("startup.ce.seconds", String.valueOf(dist.awaitReady(port).toMillis() / 1000.0));

    given().port(port).get("/plugins").then().statusCode(200).body("$", hasSize(0));
    String meetingId = activeMeetingWithOwner();
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/startRecording").then().statusCode(404);
    given().port(port).queryParam("format", "json").get("/q/openapi").then().statusCode(200)
        .body("paths", not(hasKey(START_RECORDING)));
    assertThat(tableExists("recording")).isFalse();
    dist.stop();
  }

  @Test
  @Order(2)
  void droppedJarIsIgnoredUntilReaugmentation() throws Exception {
    dist.addPlugin(RECORDING_JAR);
    dist.start(props());
    dist.awaitReady(port);

    given().port(port).get("/plugins").then().statusCode(200).body("$", hasSize(0));
    assertThat(tableExists("recording")).isFalse();
    dist.stop();
  }

  @Test
  @Order(3)
  void reaugmentationActivatesTheRecordingPlugin() throws Exception {
    metrics.setProperty("reaugment.seconds", String.valueOf(dist.reaugment().toMillis() / 1000.0));
    dist.start(props());
    metrics.setProperty("startup.recording.seconds", String.valueOf(dist.awaitReady(port).toMillis() / 1000.0));

    given().port(port).get("/plugins").then().statusCode(200).body("id", hasItem("recording"));
    assertThat(dist.log()).contains("Loaded plugin recording 0.1.0 (API 1.0)");
    assertThat(tableExists("recording")).isTrue();
    assertThat(tableExists("flyway_history_recording")).isTrue();

    String meetingId = activeMeetingWithOwner();
    as(TestUsers.OWNER).body(Map.of("folderId", "f-b1"))
        .post("/meetings/" + meetingId + "/startRecording").then().statusCode(204);
    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1/h-video"))
        .withRequestBody(matchingJsonPath("$.body.request", equalTo("edit"))));
    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1/h-audio"))
        .withRequestBody(matchingJsonPath("$.body.request", equalTo("enable_mjrs")))
        .withRequestBody(matchingJsonPath("$.body.mjrs", equalTo("true"))));
    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1/h-video"))
        .withRequestBody(matchingJsonPath("$.body.request", equalTo("enable_recording"))));
    // The OpenAPI document is produced at augmentation, so the rebuild brings the plugin's paths in.
    given().port(port).queryParam("format", "json").get("/q/openapi").then().statusCode(200)
        .body("paths", hasKey(START_RECORDING));
    as(TestUsers.OWNER).get("/meetings/" + meetingId).then().statusCode(200).body("recStartedAt", notNullValue());

    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/stop").then().statusCode(200);
    assertThat(Seed.count(ds, "SELECT count(*) FROM chats.recording WHERE meeting_id = ? AND status = 'STOPPED'", meetingId))
        .isEqualTo(1);
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
        wm.verify(postRequestedFor(urlPathEqualTo("/internal/recordings/process"))
            .withHeader("service-id", equalTo("vs-1"))
            .withRequestBody(matchingJsonPath("$.meetingId", equalTo(meetingId)))));
    dist.stop();
  }

  @Test
  @Order(4)
  void incompatiblePluginFailsFast() throws Exception {
    try (DistributionRunner broken = DistributionRunner.freshCopy(APP, WORK.resolve("dist-incompatible"))) {
      broken.addPlugin(INCOMPATIBLE_JAR);
      broken.reaugment();
      broken.start(props());

      assertThat(broken.awaitExit(Duration.ofSeconds(90))).isNotZero();
      assertThat(broken.log()).contains("Plugin 'incompatible' 0.1.0 requires plugin API 2.0 but core provides 1.0");
    }
  }
}
