// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.meeting;

import com.zextras.carbonio.ws.core.persistence.MeetingEntity;
import com.zextras.carbonio.ws.core.persistence.MeetingRepository;
import com.zextras.carbonio.ws.core.persistence.ParticipantRepository;
import com.zextras.carbonio.ws.core.persistence.SubscriptionEntity;
import com.zextras.carbonio.ws.core.persistence.SubscriptionRepository;
import com.zextras.carbonio.ws.plugin.api.MeetingAccess;
import com.zextras.carbonio.ws.plugin.api.MeetingView;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MeetingAccessImpl implements MeetingAccess {

  @Inject MeetingRepository meetings;
  @Inject ParticipantRepository participants;
  @Inject SubscriptionRepository subscriptions;
  @Inject MeetingMapper mapper;

  @Override
  @Transactional
  public Optional<MeetingView> find(UUID meetingId) {
    return meetings.findByIdOptional(meetingId.toString()).map(mapper::view);
  }

  @Override
  @Transactional
  public MeetingView requireActiveMeetingModerator(UUID meetingId, UserPrincipal user) {
    MeetingEntity meeting =
        meetings.findByIdOptional(meetingId.toString())
            .orElseThrow(() -> new NotFoundException(String.format("Meeting with id '%s' not found", meetingId)));
    if (!Boolean.TRUE.equals(meeting.active)) {
      throw new BadRequestException(String.format("Meeting '%s' is not active", meetingId));
    }
    String userId = user.id().toString();
    if (participants.find(meeting.id, userId).isEmpty()) {
      throw new NotFoundException(String.format("User '%s' not found into meeting '%s'", userId, meetingId));
    }
    SubscriptionEntity subscription =
        subscriptions.find(meeting.roomId, userId)
            .orElseThrow(() -> new NotFoundException(String.format("User with id '%s' not found", userId)));
    if (!Boolean.TRUE.equals(subscription.owner)) {
      throw new ForbiddenException(
          String.format("User '%s' is not a moderator of the meeting '%s'", userId, meetingId));
    }
    return mapper.view(meeting);
  }
}
