package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity.JanusEvent;
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
