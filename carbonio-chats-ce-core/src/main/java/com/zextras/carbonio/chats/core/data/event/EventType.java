// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventType implements EventTypeDescription {

  ROOM_CREATED("roomCreated"),
  ROOM_UPDATED("roomUpdated"),
  ROOM_DELETED("roomDeleted"),
  ROOM_OWNER_CHANGED("roomOwnerChanged"),
  ROOM_PICTURE_CHANGED("roomPictureChanged"),
  ROOM_PICTURE_DELETED("roomPictureDeleted"),
  ROOM_MEMBER_ADDED("roomMemberAdded"),
  ROOM_MEMBER_REMOVED("roomMemberRemoved"),
  ATTACHMENT_ADDED("attachmentAdded"),
  ATTACHMENT_REMOVED("attachmentRemoved"),
  ROOM_MUTED("roomMuted"),
  ROOM_UNMUTED("roomUnmuted"),
  USER_PICTURE_CHANGED("userPictureChanged"),
  USER_PICTURE_DELETED("userPictureDeleted"),
  ROOM_HISTORY_CLEARED("roomHistoryCleared"),
  MEETING_CREATED("meetingCreated"),
  MEETING_DELETED("meetingDeleted"),
  MEETING_PARTICIPANT_JOINED("meetingParticipantJoined"),
  MEETING_PARTICIPANT_LEFT("meetingParticipantLeft"),
  MEETING_PARTICIPANT_VIDEO_STREAM_OPENED("meetingParticipantVideoStreamOpened"),
  MEETING_PARTICIPANT_VIDEO_STREAM_CLOSED("meetingParticipantVideoStreamClosed"),
  MEETING_PARTICIPANT_AUDIO_STREAM_OPENED("meetingParticipantAudioStreamOpened"),
  MEETING_PARTICIPANT_AUDIO_STREAM_CLOSED("meetingParticipantAudioStreamClosed"),
  MEETING_PARTICIPANT_SCREEN_STREAM_OPENED("meetingParticipantScreenStreamOpened"),
  MEETING_PARTICIPANT_SCREEN_STREAM_CLOSED("meetingParticipantScreenStreamClosed");

  private final String description;

  EventType(String description) {
    this.description = description;
  }

  @JsonValue
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return this.description;
  }
}
