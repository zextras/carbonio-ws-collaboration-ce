// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

public enum EventType {

  ROOM_CREATED,
  ROOM_UPDATED,
  ROOM_DELETED,
  ROOM_OWNER_CHANGED,
  ROOM_PICTURE_CHANGED,
  ROOM_PICTURE_DELETED,
  ROOM_MEMBER_ADDED,
  ROOM_MEMBER_REMOVED,
  ATTACHMENT_ADDED,
  ATTACHMENT_REMOVED,
  ROOM_MUTED,
  ROOM_UNMUTED,
  USER_PICTURE_CHANGED,
  USER_PICTURE_DELETED,
  ROOM_HISTORY_CLEARED,
  MEETING_CREATED,
  MEETING_DELETED,
  MEETING_PARTICIPANT_JOINED,
  MEETING_PARTICIPANT_LEFT,
  MEETING_VIDEO_STREAM_ENABLED,
  MEETING_VIDEO_STREAM_DISABLED,
  MEETING_AUDIO_STREAM_ENABLED,
  MEETING_AUDIO_STREAM_DISABLED,
  MEETING_SCREEN_STREAM_ENABLED,
  MEETING_SCREEN_STREAM_DISABLED;

}
