// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql;

import java.util.Map;

public class GraphQlMutation {

  private static final String MUC_LIGHT = "muc_light";

  private GraphQlMutation() {}

  public static String checkAuth() {
    return "query checkAuth { checkAuth { authStatus } }";
  }

  public static GraphQlBody createRoom(String mucDomain, String roomId, String ownerJid) {
    String query =
        "mutation muc_light { muc_light { createRoom ("
            + String.format("mucDomain: \"%s\", ", mucDomain)
            + String.format("id: \"%s\", ", roomId)
            + String.format("owner: \"%s\"", ownerJid)
            + ") { jid } } }";
    return GraphQlBody.create(query, MUC_LIGHT, Map.of());
  }

  public static GraphQlBody inviteUser(String roomJid, String senderJid, String recipientJid) {
    String query =
        "mutation muc_light { muc_light { inviteUser ("
            + String.format("room: \"%s\", ", roomJid)
            + String.format("sender: \"%s\", ", senderJid)
            + String.format("recipient: \"%s\") ", recipientJid)
            + "} }";
    return GraphQlBody.create(query, MUC_LIGHT, Map.of());
  }

  public static GraphQlBody kickUser(String roomJid, String userJid) {
    String query =
        "mutation muc_light { muc_light { kickUser ("
            + String.format("room: \"%s\", ", roomJid)
            + String.format("user: \"%s\") ", userJid)
            + "} }";
    return GraphQlBody.create(query, MUC_LIGHT, Map.of());
  }

  public static GraphQlBody setMutualSubscription(String userAJid, String userBJid) {
    String query =
        "mutation roster { roster { setMutualSubscription ("
            + String.format("userA: \"%s\", ", userAJid)
            + String.format("userB: \"%s\", ", userBJid)
            + "action: CONNECT) } }";
    return GraphQlBody.create(query, "roster", Map.of());
  }

  public static GraphQlBody sendStanza(String stanza) {
    String query =
        "mutation stanza { stanza { sendStanza ("
            + String.format("stanza: \"\"\"%s\"\"\") ", stanza)
            + "{ id stanza_id } } }";
    return GraphQlBody.create(query, "stanza", Map.of());
  }
}
