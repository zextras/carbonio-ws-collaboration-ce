// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.seam;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.ws.plugin.api.MeetingAccess;
import com.zextras.carbonio.ws.plugin.api.MeetingView;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link MeetingAccess} over the CE repositories, with the checks of ADV's recording validation:
 * meeting exists (404), is active (400), the user is a participant (404) and a room owner (403).
 * The failures are CE {@code ChatsHttpException}s, rendered by the CE exception mappers.
 */
@ApplicationScoped
public class MeetingAccessImpl implements MeetingAccess {

  private final MeetingRepository meetingRepository;
  private final ParticipantRepository participantRepository;
  private final SubscriptionRepository subscriptionRepository;

  @Inject
  public MeetingAccessImpl(
      MeetingRepository meetingRepository,
      ParticipantRepository participantRepository,
      SubscriptionRepository subscriptionRepository) {
    this.meetingRepository = meetingRepository;
    this.participantRepository = participantRepository;
    this.subscriptionRepository = subscriptionRepository;
  }

  @Override
  public Optional<MeetingView> find(UUID meetingId) {
    return meetingRepository.getById(meetingId.toString()).map(MeetingViews::of);
  }

  @Override
  public MeetingView requireActiveMeetingModerator(UUID meetingId, UserPrincipal user) {
    Meeting meeting =
        meetingRepository
            .getById(meetingId.toString())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Meeting with id '%s' not found", meetingId)));
    if (!Boolean.TRUE.equals(meeting.getActive())) {
      throw new BadRequestException(String.format("Meeting '%s' is not active", meetingId));
    }
    String userId = user.id().toString();
    if (participantRepository.getById(meeting.getId(), userId).isEmpty()) {
      throw new NotFoundException(
          String.format("User '%s' not found into meeting '%s'", userId, meetingId));
    }
    boolean owner =
        subscriptionRepository
            .getById(meeting.getRoomId(), userId)
            .orElseThrow(
                () -> new NotFoundException(String.format("User with id '%s' not found", userId)))
            .isOwner();
    if (!owner) {
      throw new ForbiddenException(
          String.format("User '%s' is not a moderator of the meeting '%s'", userId, meetingId));
    }
    return MeetingViews.of(meeting);
  }
}
