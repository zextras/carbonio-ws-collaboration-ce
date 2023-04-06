// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import java.util.UUID;

public class UserProfile {

  private String id;
  private String email;
  private String name;
  private String domain;

  public UserProfile(String id) {
    this.id = id;
  }

  public static UserProfile create(String id) {
    return new UserProfile(id);
  }
  public static UserProfile create(UUID id) {
    return new UserProfile(id.toString());
  }

  public String getId() {
    return id;
  }

  public UserProfile id(String id) {
    this.id = id;
    return this;
  }

  public String getEmail() {
    return email;
  }

  public UserProfile email(String email) {
    this.email = email;
    return this;
  }

  public String getName() {
    return name;
  }

  public UserProfile name(String name) {
    this.name = name;
    return this;
  }

  public String getDomain() {
    return domain;
  }

  public UserProfile domain(String domain) {
    this.domain = domain;
    return this;
  }
}
