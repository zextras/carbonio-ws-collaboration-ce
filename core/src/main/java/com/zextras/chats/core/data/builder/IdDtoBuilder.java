package com.zextras.chats.core.data.builder;

import com.zextras.chats.core.model.IdDto;
import java.util.UUID;

public class IdDtoBuilder {

  public IdDto idDto;

  public IdDtoBuilder() {
    this.idDto = new IdDto();
  }

  public static IdDtoBuilder create() {
    return new IdDtoBuilder();
  }

  public IdDto build() {
    return this.idDto;
  }

  public IdDtoBuilder id(UUID id) {
    this.idDto.setId(id);
    return this;
  }
}
