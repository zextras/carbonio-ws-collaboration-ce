// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import com.zextras.carbonio.chats.core.data.type.MeetingType;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class MeetingBuilder {

  private final Meeting meeting;

  public MeetingBuilder(UUID meetingId) {
    this.meeting = Meeting.create().id(meetingId.toString());
  }

  public static MeetingBuilder create(UUID meetingId) {
    return new MeetingBuilder(meetingId);
  }

  public MeetingBuilder roomId(UUID roomId) {
    meeting.roomId(roomId.toString());
    return this;
  }

  public MeetingBuilder participants(List<Participant> participants) {
    meeting.participants(participants);
    return this;
  }

  public MeetingBuilder meetingType(MeetingType meetingType) {
    meeting.meetingType(meetingType);
    return this;
  }

  public MeetingBuilder createdAt(OffsetDateTime createdAt) {
    try {
      Field createdAtField = Meeting.class.getDeclaredField("createdAt");
      createdAtField.setAccessible(true);
      createdAtField.set(meeting, createdAt);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public Meeting build() {
    return meeting;
  }
}
