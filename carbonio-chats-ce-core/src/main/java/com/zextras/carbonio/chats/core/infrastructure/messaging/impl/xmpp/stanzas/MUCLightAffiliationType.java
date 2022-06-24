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
