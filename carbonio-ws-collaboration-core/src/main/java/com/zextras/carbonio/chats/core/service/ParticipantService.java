// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.model.*;

import java.util.List;
import java.util.UUID;

public interface ParticipantService {

  /**
   * Inserts a participant into the indicated meeting
   *
   * @param meetingId identifier of the meeting in which to insert the participant {@link UUID}
   * @param joinSettingsDto participation join settings for meeting {@link JoinSettingsDto}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @throws ConflictException if the session is already inserted into the meeting
   * @throws NotFoundException if the meeting doesn't exist
   * @throws NotFoundException if the associated room doesn't exist
   * @throws ForbiddenException if the user isn't an associated room member
   * @throws ForbiddenException if the user isn't an associated room owner and mustBeRoomOwner is
   *     true
   */
  JoinStatus insertMeetingParticipant(
      UUID meetingId, JoinSettingsDto joinSettingsDto, UserPrincipal currentUser);

  /**
   * Returns the list of users in queue for entering the meeting
   * @param meetingId the id of the meeting
   * @return the list of ids of the users in queue
   */
  List<UUID> getQueue(UUID meetingId);

  /**
   * Updates the status of a user in queue, accepting or refusing it
   * @param meetingId the id of the meeting
   * @param userId the id of the user in queue
   * @param status the action to perform
   */
  void updateQueue(UUID meetingId, UUID userId, QueuedUserStatusDto status, UserPrincipal currentUser);

  /**
   * Clears the queue of a meeting, deleting all users accepted and waiting from the list
   * @param meetingId the id of the meeting
   */
  void clearQueue(UUID meetingId);

  /**
   * Removes the participant of current user from the indicated meeting
   *
   * @param meetingId identifier of the meeting from which to remove the participant {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException if the meeting doesn't exist
   * @throws NotFoundException if the user session for indicated meeting doesn't exist.
   * @throws NotFoundException if the associated room doesn't exist
   * @throws ForbiddenException if the user isn't a room member
   * @throws ForbiddenException if the user isn't a room owner and mustBeOwner is true
   */
  void removeMeetingParticipant(UUID meetingId, UserPrincipal currentUser);

  /**
   * Removes the participant of the current user from a meeting. This method accepts entities
   * because it's intended to be used only to be called by services.
   *
   * @param meeting participant {@link Meeting}
   * @param room participant {@link Room}
   * @param userId identifier of the user to remove
   * @throws NotFoundException if the user session for indicated meeting doesn't exist.
   */
  void removeMeetingParticipant(Meeting meeting, Room room, UUID userId);

  /**
   * Removes the participant of the current user from a meeting. This method accepts entities
   * because it's intended to be used only to be called by services.
   *
   * @param meeting participant {@link Meeting}
   * @param room participant {@link Room}
   * @param userId identifier of the user to remove
   * @param queueId participant queue identifier
   * @throws NotFoundException if the user session for indicated meeting doesn't exist.
   */
  void removeMeetingParticipant(Meeting meeting, Room room, UUID userId, UUID queueId);

  /**
   * Updates the media stream status in the meeting for the current session and starts WebRTC
   * negotiation with VideoServer for the PeerConnection setup related to video stream when it has
   * to be enabled.
   *
   * @param meetingId meeting identifier {@link UUID}
   * @param mediaStreamSettingsDto user settings request to update the media stream status
   * @param currentUser currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException if the meeting doesn't exist
   * @throws NotFoundException if the user session for indicated meeting doesn't exist.
   * @throws NotFoundException if the associated room doesn't exist
   * @throws BadRequestException if another session tries to enable the stream
   * @throws ForbiddenException if the current user isn't a room owner
   */
  void updateMediaStream(
      UUID meetingId, MediaStreamSettingsDto mediaStreamSettingsDto, UserPrincipal currentUser);

  /**
   * Updates the audio stream status in the meeting for the current session
   *
   * @param meetingId meeting identifier {@link UUID}
   * @param audioStreamSettingsDto audio stream settings request to update a user's audio stream
   * @param currentUser currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException if the meeting doesn't exist
   * @throws NotFoundException if the user session for indicated meeting doesn't exist.
   * @throws NotFoundException if the associated room doesn't exist
   * @throws BadRequestException if another session tries to enable the stream
   * @throws ForbiddenException if the current user isn't a room owner
   */
  void updateAudioStream(
      UUID meetingId, AudioStreamSettingsDto audioStreamSettingsDto, UserPrincipal currentUser);

  /**
   * Completes WebRTC negotiation with VideoServer for the PeerConnection setup related to media
   * stream.
   *
   * @param meetingId meeting identifier {@link UUID}
   * @param sdp the answer rtc session description
   * @param currentUser currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException if the meeting doesn't exist
   * @throws NotFoundException if the user session for indicated meeting doesn't exist.
   * @throws NotFoundException if the associated room doesn't exist
   * @throws BadRequestException if the rtc session description type is not offer
   * @throws ForbiddenException if the current user isn't a room owner
   */
  void answerRtcMediaStream(UUID meetingId, String sdp, UserPrincipal currentUser);

  /**
   * Update subscriptions of the current session to the desired media streams.
   *
   * @param meetingId meeting identifier {@link UUID}
   * @param subscriptionUpdatesDto contains all media streams which user wants to update
   *     subscriptions for
   * @param currentUser currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException if the meeting doesn't exist
   * @throws NotFoundException if the user session for indicated meeting doesn't exist.
   * @throws NotFoundException if the associated room doesn't exist
   * @throws BadRequestException if the rtc session description type is not offer
   * @throws ForbiddenException if the current user isn't a room owner
   */
  void updateSubscriptionsMediaStream(
      UUID meetingId, SubscriptionUpdatesDto subscriptionUpdatesDto, UserPrincipal currentUser);

  /**
   * Starts WebRTC negotiation with VideoServer for the PeerConnection setup related to audio
   * stream.
   *
   * @param meetingId meeting identifier {@link UUID}
   * @param sdp the offer rtc session description
   * @param currentUser currentUser current authenticated user {@link UserPrincipal}
   * @throws NotFoundException if the meeting doesn't exist
   * @throws NotFoundException if the user session for indicated meeting doesn't exist.
   * @throws NotFoundException if the associated room doesn't exist
   * @throws BadRequestException if the rtc session description type is not offer
   * @throws ForbiddenException if the current user isn't a room owner
   */
  void offerRtcAudioStream(UUID meetingId, String sdp, UserPrincipal currentUser);
}
