// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.account;

import java.util.UUID;

public class Account {

  private String id;
  private String email;
  private String name;
  private String domain;

  public Account(String id) {
    this.id = id;
  }

  public static Account create(String id) {
    return new Account(id);
  }
  public static Account create(UUID id) {
    return new Account(id.toString());
  }

  public String getId() {
    return id;
  }

  public Account id(String id) {
    this.id = id;
    return this;
  }

  public String getEmail() {
    return email;
  }

  public Account email(String email) {
    this.email = email;
    return this;
  }

  public String getName() {
    return name;
  }

  public Account name(String name) {
    this.name = name;
    return this;
  }

  public String getDomain() {
    return domain;
  }

  public Account domain(String domain) {
    this.domain = domain;
    return this;
  }
}
