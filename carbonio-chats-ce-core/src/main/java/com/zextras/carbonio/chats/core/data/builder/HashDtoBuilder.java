package com.zextras.carbonio.chats.core.data.builder;

import com.zextras.carbonio.chats.core.model.HashDto;

public class HashDtoBuilder {

  public HashDto hashDto;

  public HashDtoBuilder() {
    this.hashDto = new HashDto();
  }

  public static HashDtoBuilder create() {
    return new HashDtoBuilder();
  }

  public HashDto build() {
    return this.hashDto;
  }

  public HashDtoBuilder hash(String hash) {
    this.hashDto.setHash(hash);
    return this;
  }
}
