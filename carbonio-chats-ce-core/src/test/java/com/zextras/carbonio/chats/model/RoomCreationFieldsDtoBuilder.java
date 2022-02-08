package com.zextras.carbonio.chats.model;

import java.util.ArrayList;
import java.util.UUID;

public class RoomCreationFieldsDtoBuilder {

  private final RoomCreationFieldsDto roomCreationFieldsDto;

  public RoomCreationFieldsDtoBuilder() {
    this.roomCreationFieldsDto = new RoomCreationFieldsDto();
    this.roomCreationFieldsDto.setMembersIds(new ArrayList<>());
  }

  public static RoomCreationFieldsDtoBuilder create() {
    return new RoomCreationFieldsDtoBuilder();
  }

  public RoomCreationFieldsDto build() {
    return this.roomCreationFieldsDto;
  }

  public RoomCreationFieldsDtoBuilder name(String name) {
    this.roomCreationFieldsDto.setName(name);
    return this;
  }

  public RoomCreationFieldsDtoBuilder description(String description) {
    this.roomCreationFieldsDto.setDescription(description);
    return this;
  }

  public RoomCreationFieldsDtoBuilder type(RoomTypeDto type) {
    this.roomCreationFieldsDto.setType(type);
    return this;
  }

  public RoomCreationFieldsDtoBuilder addMemberId(UUID memberId) {
    this.roomCreationFieldsDto.getMembersIds().add(memberId);
    return this;
  }

  public RoomCreationFieldsDtoBuilder removeMemberId(UUID memberId) {
    this.roomCreationFieldsDto.getMembersIds().remove(memberId);
    return this;
  }

}
