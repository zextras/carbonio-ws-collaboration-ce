package com.zextras.carbonio.chats.meeting.data.event;

import com.fasterxml.jackson.annotation.JsonValue;
import com.zextras.carbonio.chats.core.data.event.EventTypeDescription;

public enum MeetingEventType implements EventTypeDescription {

  MEETING_CREATED("meetingCreated"),
  MEETING_DELETED("meetingDeleted"),
  MEETING_PARTICIPANT_JOINED("meetingParticipantJoined");

  private final String description;

  MeetingEventType(String description) {
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
