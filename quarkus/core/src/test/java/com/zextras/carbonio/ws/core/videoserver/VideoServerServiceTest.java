// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.videoserver;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.zextras.carbonio.ws.core.persistence.VideoServerMeetingRepository;
import com.zextras.carbonio.ws.plugin.api.VideoServerGateway;
import com.zextras.carbonio.ws.plugin.api.VideoServerRooms;
import com.zextras.carbonio.ws.testsupport.Seed;
import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class VideoServerServiceTest {

  @Inject DataSource ds;
  @Inject VideoServerService videoServer;
  @Inject VideoServerGateway gateway;
  @Inject VideoServerMeetingRepository repo;

  WireMockServer wm;
  UUID meetingId;

  @BeforeEach
  void setUp() {
    wm = WireMockResource.server();
    wm.resetAll();
    Stubs.janus(wm);
    Seed.clean(ds);
    UUID roomId = UUID.randomUUID();
    meetingId = UUID.randomUUID();
    Seed.room(ds, roomId, "group", Map.of(TestUsers.OWNER, true));
    Seed.meeting(ds, meetingId, roomId, false);
  }

  @Test
  void startMeetingCreatesRoomsAndPersistsHandles() {
    QuarkusTransaction.requiringNew().run(() -> videoServer.startMeeting(meetingId.toString()));

    QuarkusTransaction.requiringNew().run(() -> {
      var vsm = repo.findById(meetingId.toString());
      assertThat(vsm.serverId).isEqualTo("vs-1");
      assertThat(vsm.connectionId).isEqualTo("conn-1");
      assertThat(vsm.audioHandleId).isEqualTo("h-audio");
      assertThat(vsm.videoHandleId).isEqualTo("h-video");
      assertThat(vsm.audioRoomId).isEqualTo("audio-room");
      assertThat(vsm.videoRoomId).isEqualTo("video-room");
    });
    wm.verify(postRequestedFor(urlPathEqualTo("/janus"))
        .withQueryParam("service_id", equalTo("vs-1"))
        .withRequestBody(matchingJsonPath("$.apisecret", equalTo("test-secret"))));
    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1/h-video"))
        .withRequestBody(matchingJsonPath("$.body.room", equalTo("video_" + meetingId)))
        .withRequestBody(matchingJsonPath("$.body.bitrate", equalTo("8000000")))
        .withRequestBody(matchingJsonPath("$.body.videocodec", equalTo("vp8,h264,vp9,h265,av1"))));
    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1/h-audio"))
        .withRequestBody(matchingJsonPath("$.body.room", equalTo("audio_" + meetingId)))
        .withRequestBody(matchingJsonPath("$.body.sampling_rate", equalTo("16000"))));
  }

  @Test
  void startMeetingIsIdempotent() {
    QuarkusTransaction.requiringNew().run(() -> videoServer.startMeeting(meetingId.toString()));
    QuarkusTransaction.requiringNew().run(() -> videoServer.startMeeting(meetingId.toString()));

    wm.verify(1, postRequestedFor(urlPathEqualTo("/janus"))
        .withRequestBody(matchingJsonPath("$.janus", equalTo("create"))));
  }

  @Test
  void startMeetingFailsWhenTheRoomIsNotCreated() {
    wm.stubFor(post(urlPathEqualTo("/janus/conn-1/h-audio")).atPriority(0)
        .willReturn(okJson("{\"janus\":\"success\",\"plugindata\":{\"data\":{\"audiobridge\":\"event\",\"error\":\"boom\"}}}")));

    assertThatThrownBy(() ->
            QuarkusTransaction.requiringNew().run(() -> videoServer.startMeeting(meetingId.toString())))
        .isInstanceOf(VideoServerException.class);
    assertThat(Seed.count(ds, "SELECT count(*) FROM chats.videoserver_meeting")).isZero();
  }

  @Test
  void stopMeetingDestroysEverythingAndDeletesTheRow() {
    Seed.videoServerMeeting(ds, meetingId, "vs-1");

    QuarkusTransaction.requiringNew().run(() -> videoServer.stopMeeting(meetingId.toString()));

    assertThat(Seed.count(ds, "SELECT count(*) FROM chats.videoserver_meeting")).isZero();
    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1/h-video"))
        .withRequestBody(matchingJsonPath("$.body.request", equalTo("destroy"))));
    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1/h-audio"))
        .withRequestBody(matchingJsonPath("$.janus", equalTo("detach"))));
    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1"))
        .withRequestBody(matchingJsonPath("$.janus", equalTo("destroy"))));
  }

  @Test
  void gatewaySendsPluginMessagesToTheMeetingHandles() {
    Seed.videoServerMeeting(ds, meetingId, "vs-1");

    var response = gateway.sendVideoRoomMessage(meetingId, Map.of("request", "enable_recording"));

    assertThat(JanusClient.isSuccess(response)).isTrue();
    assertThat(gateway.roomsOf(meetingId)).contains(new VideoServerRooms("vs-1", "audio-room", "video-room"));
    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1/h-video"))
        .withQueryParam("service_id", equalTo("vs-1"))
        .withRequestBody(matchingJsonPath("$.janus", equalTo("message")))
        .withRequestBody(matchingJsonPath("$.body.request", equalTo("enable_recording"))));
  }

  @Test
  void gatewayWithoutServerIdOmitsTheRoutingParameter() {
    Seed.videoServerMeeting(ds, meetingId, null);

    gateway.sendAudioBridgeMessage(meetingId, Map.of("request", "enable_mjrs"));

    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1/h-audio"))
        .withQueryParam("service_id", absent()));
  }

  @Test
  void gatewayFailsForAMeetingWithoutVideoServerRow() {
    assertThatThrownBy(() -> gateway.sendVideoRoomMessage(meetingId, Map.of()))
        .isInstanceOf(VideoServerException.class);
    assertThat(gateway.roomsOf(meetingId)).isEmpty();
  }
}
