// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

public enum EventType {

  ROOM_CREATED,
  ROOM_UPDATED,
  ROOM_DELETED,
  ROOM_OWNER_PROMOTED,
  ROOM_OWNER_DEMOTED,
  ROOM_PICTURE_CHANGED,
  ROOM_PICTURE_DELETED,
  ROOM_MEMBER_ADDED,
  ROOM_MEMBER_REMOVED,
  ROOM_MUTED,
  ROOM_UNMUTED,
  USER_PICTURE_CHANGED,
  USER_PICTURE_DELETED,
  ROOM_HISTORY_CLEARED,
  MEETING_CREATED,
  MEETING_DELETED,
  MEETING_PARTICIPANT_JOINED,
  MEETING_PARTICIPANT_LEFT,
  MEETING_AUDIO_STREAM_CHANGED,
  MEETING_AUDIO_ANSWERED,
  MEETING_MEDIA_STREAM_CHANGED,
  MEETING_SDP_OFFERED,
  MEETING_SDP_ANSWERED,
  MEETING_STARTED,
  MEETING_STOPPED,
  MEETING_PARTICIPANT_TALKING,
  MEETING_PARTICIPANT_CLASHED,
  MEETING_PARTICIPANT_SUBSCRIBED,

}
