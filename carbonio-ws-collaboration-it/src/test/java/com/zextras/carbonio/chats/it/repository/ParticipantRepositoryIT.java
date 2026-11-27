// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository.StaleParticipant;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MeetingTestUtils;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class ParticipantRepositoryIT {

  private static final Duration TWELVE_HOURS = Duration.ofHours(12);

  private final ParticipantRepository participantRepository;
  private final IntegrationTestUtils integrationTestUtils;
  private final MeetingTestUtils meetingTestUtils;
  private final Database database;

  private final UUID staleUserId = UUID.randomUUID();
  private final String staleQueueId = UUID.randomUUID().toString();
  private UUID meetingId;

  ParticipantRepositoryIT(
      ParticipantRepository participantRepository,
      IntegrationTestUtils integrationTestUtils,
      MeetingTestUtils meetingTestUtils,
      Database database) {
    this.participantRepository = participantRepository;
    this.integrationTestUtils = integrationTestUtils;
    this.meetingTestUtils = meetingTestUtils;
    this.database = database;
  }

  @BeforeEach
  void seedOneStaleAndOneActiveParticipant() {
    UUID roomId = UUID.randomUUID();
    UUID activeUserId = UUID.randomUUID();
    String activeQueueId = UUID.randomUUID().toString();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "room", List.of(staleUserId, activeUserId));
    meetingId =
        meetingTestUtils.generateAndSaveMeeting(
            roomId,
            List.of(
                ParticipantBuilder.create(staleUserId, staleQueueId),
                ParticipantBuilder.create(activeUserId, activeQueueId)));
    database
        .sqlUpdate(
            "UPDATE CHATS.PARTICIPANT SET UPDATED_AT = now() - CASE WHEN QUEUE_ID = :staleQueueId"
                + " THEN interval '13 hours' ELSE interval '11 hours' END"
                + " WHERE QUEUE_ID IN (:staleQueueId, :activeQueueId)")
        .setParameter("staleQueueId", staleQueueId)
        .setParameter("activeQueueId", activeQueueId)
        .execute();
  }

  @Test
  void claimStale_returnsOnlyParticipantsUntouchedForLongerThanTheThreshold() {
    assertEquals(
        List.of(new StaleParticipant(meetingId.toString(), staleUserId.toString(), staleQueueId)),
        participantRepository.claimStale(TWELVE_HOURS));
  }

  @Test
  void claimStale_doesNotReturnTheSameParticipantTwice() {
    participantRepository.claimStale(TWELVE_HOURS);

    assertTrue(participantRepository.claimStale(TWELVE_HOURS).isEmpty());
  }
}
