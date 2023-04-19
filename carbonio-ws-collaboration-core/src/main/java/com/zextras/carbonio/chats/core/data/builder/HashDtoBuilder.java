// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.builder;

import com.zextras.carbonio.chats.model.HashDto;

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
