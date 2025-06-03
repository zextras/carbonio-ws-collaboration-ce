// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.*;
import java.util.UUID;

public interface ParticipantService {

  JoinStatus insertMeetingParticipant(
      UUID meetingId, JoinSettingsDto joinSettingsDto, UserPrincipal currentUser);

  void removeMeetingParticipant(UUID meetingId, UserPrincipal currentUser);

  void removeMeetingParticipant(Meeting meeting, Room room, UUID userId);

  void removeMeetingParticipant(UUID queueId);

  void updateMediaStream(
      UUID meetingId, MediaStreamSettingsDto mediaStreamSettingsDto, UserPrincipal currentUser);

  void updateAudioStream(
      UUID meetingId, AudioStreamSettingsDto audioStreamSettingsDto, UserPrincipal currentUser);

  void answerRtcMediaStream(UUID meetingId, String sdp, UserPrincipal currentUser);

  void updateSubscriptionsMediaStream(
      UUID meetingId, SubscriptionUpdatesDto subscriptionUpdatesDto, UserPrincipal currentUser);

  void offerRtcAudioStream(UUID meetingId, String sdp, UserPrincipal currentUser);

  void updateHandStatus(UUID meetingId, HandStatusDto handStatusDto, UserPrincipal currentUser);

  void clear(UUID meetingId);
}
