package com.zextras.carbonio.chats.meeting.infrastructure.videoserver.data;

import java.util.List;

public class JanusEventsResponse extends JanusResponse {

  private List<JanusEvent> events;

  public JanusEventsResponse() {
  }

  public JanusEventsResponse(List<JanusEvent> events) {
    this.events = events;
  }

  public List<JanusEvent> getEvents() {
    return events;
  }

  public void setEvents(List<JanusEvent> events) {
    this.events = events;
  }
}
