// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.builder;

import com.zextras.carbonio.chats.model.IdDto;
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
