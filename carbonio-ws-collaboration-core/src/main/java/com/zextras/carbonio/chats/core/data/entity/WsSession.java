// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "WS_SESSION", schema = "CHATS")
public class WsSession {

  @Id
  @Column(name = "SESSION_ID", length = 64, nullable = false)
  private String sessionId;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "SERVICE_ID", length = 64, nullable = false)
  private String serviceId;

  public static WsSession create() {
    return new WsSession();
  }

  public String getSessionId() {
    return sessionId;
  }

  public WsSession sessionId(String sessionId) {
    this.sessionId = sessionId;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public WsSession userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getServiceId() {
    return serviceId;
  }

  public WsSession serviceId(String serviceId) {
    this.serviceId = serviceId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WsSession that = (WsSession) o;
    return Objects.equals(sessionId, that.sessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sessionId);
  }
}
