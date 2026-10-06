// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.meeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zextras.carbonio.ws.plugin.api.MeetingAccess;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import com.zextras.carbonio.ws.plugin.api.UserType;
import com.zextras.carbonio.ws.testsupport.Seed;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MeetingAccessTest {

  @Inject DataSource ds;
  @Inject MeetingAccess access;

  UUID roomId;
  UUID meetingId;

  static UserPrincipal user(UUID id) {
    return new UserPrincipal(id, UserType.INTERNAL, "e", "n", "t", null, Map.of());
  }

  @BeforeEach
  void setUp() {
    Seed.clean(ds);
    roomId = UUID.randomUUID();
    meetingId = UUID.randomUUID();
    Seed.room(ds, roomId, "group", Map.of(TestUsers.OWNER, true, TestUsers.MEMBER, false));
  }

  @Test
  void ownerParticipantOfActiveMeetingIsModerator() {
    Seed.meeting(ds, meetingId, roomId, true);
    Seed.participant(ds, meetingId, TestUsers.OWNER);
    Seed.participant(ds, meetingId, TestUsers.MEMBER);

    var view = access.requireActiveMeetingModerator(meetingId, user(TestUsers.OWNER));

    assertThat(view.id()).isEqualTo(meetingId);
    assertThat(view.participantUserIds()).containsExactlyInAnyOrder(TestUsers.OWNER, TestUsers.MEMBER);
  }

  @Test
  void missingMeetingIsNotFound() {
    assertThatThrownBy(() -> access.requireActiveMeetingModerator(meetingId, user(TestUsers.OWNER)))
        .isInstanceOf(NotFoundException.class);
    assertThat(access.find(meetingId)).isEmpty();
  }

  @Test
  void inactiveMeetingIsBadRequest() {
    Seed.meeting(ds, meetingId, roomId, false);
    Seed.participant(ds, meetingId, TestUsers.OWNER);
    assertThatThrownBy(() -> access.requireActiveMeetingModerator(meetingId, user(TestUsers.OWNER)))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void nonParticipantIsNotFound() {
    Seed.meeting(ds, meetingId, roomId, true);
    assertThatThrownBy(() -> access.requireActiveMeetingModerator(meetingId, user(TestUsers.OWNER)))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void nonOwnerIsForbidden() {
    Seed.meeting(ds, meetingId, roomId, true);
    Seed.participant(ds, meetingId, TestUsers.MEMBER);
    assertThatThrownBy(() -> access.requireActiveMeetingModerator(meetingId, user(TestUsers.MEMBER)))
        .isInstanceOf(ForbiddenException.class);
  }
}
