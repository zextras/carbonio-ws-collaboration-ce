// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp.stanzas;

public enum MUCLightAffiliationType {
  NONE("none"),
  MEMBER("member"),
  OWNER("owner");

  private final String stringValue;

  MUCLightAffiliationType(String stringValue) {
    this.stringValue = stringValue;
  }

  public String toString() {
    return stringValue;
  }
}
