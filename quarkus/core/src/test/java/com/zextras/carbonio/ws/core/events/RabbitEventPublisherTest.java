// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.MeetingStarted;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RabbitEventPublisherTest {

  @Inject RabbitEventPublisher publisher;
  @Inject RabbitSessions sessions;

  @Test
  void eventReachesEverySessionOfTheUserOnly() {
    List<String> ownerA = new CopyOnWriteArrayList<>();
    List<String> ownerB = new CopyOnWriteArrayList<>();
    List<String> member = new CopyOnWriteArrayList<>();
    sessions.setup(TestUsers.OWNER.toString(), UUID.randomUUID().toString(), ownerA::add);
    sessions.setup(TestUsers.OWNER.toString(), UUID.randomUUID().toString(), ownerB::add);
    sessions.setup(TestUsers.MEMBER.toString(), UUID.randomUUID().toString(), member::add);

    UUID meetingId = UUID.randomUUID();
    publisher.sendToUsers(
        List.of(TestUsers.OWNER),
        MeetingStarted.create()
            .meetingId(meetingId)
            .starterUser(TestUsers.OWNER)
            .type(EventType.MEETING_STARTED)
            .sentDate(OffsetDateTime.now()));

    await().atMost(Duration.ofSeconds(5)).until(() -> ownerA.size() == 1 && ownerB.size() == 1);
    assertThat(ownerA.get(0)).contains("\"type\":\"MeetingStarted\"").contains(meetingId.toString());
    assertThat(member).isEmpty();
  }

  @Test
  void teardownStopsDelivery() {
    List<String> received = new CopyOnWriteArrayList<>();
    String queueId = UUID.randomUUID().toString();
    sessions.setup(TestUsers.MEMBER.toString(), queueId, received::add);
    sessions.teardown(queueId);

    publisher.sendToUsers(
        List.of(TestUsers.MEMBER),
        MeetingStarted.create().meetingId(UUID.randomUUID()).type(EventType.MEETING_STARTED)
            .sentDate(OffsetDateTime.now()));

    await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(1)).until(received::isEmpty);
  }
}
