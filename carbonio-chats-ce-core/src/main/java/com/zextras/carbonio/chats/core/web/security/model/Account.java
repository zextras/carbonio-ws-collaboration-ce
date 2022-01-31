// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security.model;

import java.util.UUID;

public class Account {

  private UUID   id;
  private String name;


  public Account(String id, String name) {
    this.id = UUID.fromString(id);
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

}
