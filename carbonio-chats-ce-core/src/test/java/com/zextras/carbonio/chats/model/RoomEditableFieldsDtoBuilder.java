package com.zextras.carbonio.chats.model;

public class RoomEditableFieldsDtoBuilder {

  private final RoomEditableFieldsDto roomEditableFieldsDto;

  public RoomEditableFieldsDtoBuilder() {
    this.roomEditableFieldsDto = new RoomEditableFieldsDto();
  }

  public static RoomEditableFieldsDtoBuilder create() {
    return new RoomEditableFieldsDtoBuilder();
  }

  public RoomEditableFieldsDto build() {
    return this.roomEditableFieldsDto;
  }

  public RoomEditableFieldsDtoBuilder name(String name) {
    this.roomEditableFieldsDto.setName(name);
    return this;
  }

  public RoomEditableFieldsDtoBuilder description(String description) {
    this.roomEditableFieldsDto.setDescription(description);
    return this;
  }
}
