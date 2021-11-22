package com.zextras.team.core.web.dispatcher.model;


import com.zextras.team.core.data.type.MessageType;
import com.zextras.team.core.model.RoomTypeDto;
import java.time.LocalDateTime;

public class Event {

  private String        id;
  private LocalDateTime sentTimestamp;
  private LocalDateTime editTimestamp;
  private MessageType   eventType;
  private RoomTypeDto   messageType;
  private short         indexStatus;
  private String        sender;
  private String        destination;
  private boolean       deleted;
  private String        repliedTo;
  private String        forwardedFrom;
  private String        text;
  private String        reactions;
  private String        typeExtrainfo;

  public static Event create() {
    return new Event();
  }

  public Event id(String id) {
    this.id = id;
    return this;
  }

  public Event sentTimestamp(LocalDateTime sentTimestamp) {
    this.sentTimestamp = sentTimestamp;
    return this;
  }

  public Event editTimestamp(LocalDateTime editTimestamp) {
    this.editTimestamp = editTimestamp;
    return this;
  }

  public Event eventType(MessageType eventType) {
    this.eventType = eventType;
    return this;
  }

  public Event messageType(RoomTypeDto messageType) {
    this.messageType = messageType;
    return this;
  }

  public Event indexStatus(short indexStatus) {
    this.indexStatus = indexStatus;
    return this;
  }

  public Event sender(String sender) {
    this.sender = sender;
    return this;
  }

  public Event destination(String destination) {
    this.destination = destination;
    return this;
  }

  public Event deleted(boolean deleted) {
    this.deleted = deleted;
    return this;
  }

  public Event repliedTo(String repliedTo) {
    this.repliedTo = repliedTo;
    return this;
  }

  public Event forwardedFrom(String forwardedFrom) {
    this.forwardedFrom = forwardedFrom;
    return this;
  }

  public Event text(String text) {
    this.text = text;
    return this;
  }

  public Event reactions(String reactions) {
    this.reactions = reactions;
    return this;
  }

  public Event typeExtrainfo(String typeExtrainfo) {
    this.typeExtrainfo = typeExtrainfo;
    return this;
  }
}
