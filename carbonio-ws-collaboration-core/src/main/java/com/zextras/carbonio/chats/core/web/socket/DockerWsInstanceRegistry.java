// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.google.inject.Singleton;
import java.util.Set;
import java.util.UUID;

@Singleton
public class DockerWsInstanceRegistry implements WsInstanceRegistry {

  static final String INSTANCE_ID_ENV = "WSC_INSTANCE_ID";
  static final String DEFAULT_INSTANCE_ID = "00000000-0000-4000-8000-000000000001";

  private final String selfId;

  public DockerWsInstanceRegistry() {
    this(System.getenv(INSTANCE_ID_ENV));
  }

  DockerWsInstanceRegistry(String configuredId) {
    this.selfId =
        configuredId == null || configuredId.isBlank()
            ? DEFAULT_INSTANCE_ID
            : UUID.fromString(configuredId.trim()).toString();
  }

  @Override
  public String selfId() {
    return selfId;
  }

  @Override
  public Set<String> liveInstanceIds() {
    return Set.of(selfId);
  }
}
