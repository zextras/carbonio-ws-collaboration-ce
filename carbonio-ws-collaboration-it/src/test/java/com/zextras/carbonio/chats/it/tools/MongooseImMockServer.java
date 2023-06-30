// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import org.mockserver.model.StringBody;

public class MongooseImMockServer extends ClientAndServer implements CloseableResource {

  public MongooseImMockServer(Integer... ports) {
    super(ports);
  }

  public MongooseImMockServer(String remoteHost, Integer remotePort, Integer... ports) {
    super(remoteHost, remotePort, ports);
  }

  private HttpRequest getRequest(String method, String body) {
    HttpRequest request = request().withMethod(method).withPath("/api/graphql")
      .withHeaders(Header.header("Authorization", "Basic dXNlcm5hbWU6cGFzc3dvcmQ="));
    if ("GET".equals(method)) {
      request.withQueryStringParameter(Parameter.param("query", body));
    }
    if ("POST".equals(method)) {
      request.withHeaders(Header.header("Content-Type", "application/json"),
        Header.header("Accept", "application/json")).withBody(StringBody.exact(body));
    }
    return request;
  }

  private HttpResponse getResponse(boolean success) {
    return response().withStatusCode(200)
      .withBody(success ? "{ \"data\": { \"mock\": \"success\" } }" : "{ \"errors\": [ { \"mock\": \"failure\" } ] }")
      .withHeaders(Header.header("Authorization", "Basic dXNlcm5hbWU6cGFzc3dvcmQ="),
        Header.header("Accept", "application/json"));
  }

  public HttpRequest getIsAliveRequest() {
    return getRequest("GET", "query checkAuth { checkAuth { authStatus } }");
  }

  public void mockIsAlive(boolean success) {
    HttpRequest request = getIsAliveRequest();
    clear(request);
    when(request).respond(getResponse(success));
  }

  public HttpRequest getCreateRoomRequest(String roomId, String senderId) {
    StringBuilder body = new StringBuilder(
      "{\"query\":\"mutation muc_light { muc_light { createRoom (mucDomain: \\\"muclight.carbonio\\\", "
        + String.format("id: \\\"%s\\\", ", roomId) + String.format("owner: \\\"%s@carbonio\\\"", senderId));
    body.append(") { jid } } }\",\"operationName\":\"muc_light\",\"variables\":{}}");

    return getRequest("POST", body.toString());
  }

  public void mockCreateRoom(String roomId, String senderId,
    boolean success) {
    HttpRequest request = getCreateRoomRequest(roomId, senderId);
    clear(request);
    when(request).respond(getResponse(success));
  }

  public HttpRequest getDeleteRoomRequest(String roomId) {
    return getRequest("POST", "{\"query\":\"mutation muc_light { muc_light { deleteRoom (" + String.format(
      "room: \\\"%s@muclight.carbonio\\\") ", roomId) + "} }\",\"operationName\":\"muc_light\",\"variables\":{}}");
  }

  public void mockDeleteRoom(String roomId, boolean success) {
    HttpRequest request = getDeleteRoomRequest(roomId);
    clear(request);
    when(request).respond(getResponse(success));
  }

  public HttpRequest getAddRoomMemberRequest(String roomId, String senderId, String recipientId) {
    return getRequest("POST", "{\"query\":\"mutation muc_light { muc_light { inviteUser (" + String.format(
      "room: \\\"%s@muclight.carbonio\\\", ", roomId) + String.format("sender: \\\"%s@carbonio\\\", ", senderId)
      + String.format("recipient: \\\"%s@carbonio\\\") ", recipientId)
      + "} }\",\"operationName\":\"muc_light\",\"variables\":{}}");
  }

  public void mockAddRoomMember(String roomId, String senderId, String recipientId, boolean success) {
    HttpRequest request = getAddRoomMemberRequest(roomId, senderId, recipientId);
    clear(request);
    when(request).respond(getResponse(success));
  }

  public HttpRequest getRemoveRoomMemberRequest(String roomId, String idToRemove) {
    return getRequest("POST",
      "{\"query\":\"mutation muc_light { muc_light { kickUser (" + String.format("room: \\\"%s@muclight.carbonio\\\", ",
        roomId) + String.format("user: \\\"%s@carbonio\\\") ", idToRemove)
        + "} }\",\"operationName\":\"muc_light\",\"variables\":{}}");
  }

  public void mockRemoveRoomMember(String roomId, String idToRemove, boolean success) {
    HttpRequest request = getRemoveRoomMemberRequest(roomId, idToRemove);
    clear(request);
    when(request).respond(getResponse(success));
  }

  public HttpRequest getAddUserToContactsRequest(String user1id, String user2id) {
    return getRequest("POST",
      "{\"query\":\"mutation roster { roster { setMutualSubscription (" + String.format("userA: \\\"%s@carbonio\\\", ",
        user1id) + String.format("userB: \\\"%s@carbonio\\\", ", user2id)
        + "action: CONNECT) } }\",\"operationName\":\"roster\",\"variables\":{}}");
  }

  public void mockAddUserToContacts(String user1id, String user2id, boolean success) {
    HttpRequest request = getAddUserToContactsRequest(user1id, user2id);
    clear(request);
    when(request).respond(getResponse(success));
  }

  public HttpRequest getSendStanzaRequest(
    String roomId, String senderId, @Nullable String type, List<SimpleEntry<String, String>> content,
    @Nullable String body, @Nullable String messageId, @Nullable String replyId
  ) {
    return getSendStanzaRequest(
      "<message xmlns='jabber:client' " + String.format("from='%s@carbonio' ", senderId) + (messageId == null ? ""
        : String.format("id='%s' ", messageId)) + String.format("to='%s@muclight.carbonio' ", roomId)
        + "type='groupchat'>" + Optional.ofNullable(content.isEmpty() && type == null ? null : content).map(
        list -> "<x xmlns='urn:xmpp:muclight:0#configuration'>" + Optional.ofNullable(type)
          .map(t -> String.format("<operation>%s</operation>", t)).orElse("") + list.stream()
          .map(c -> "<" + c.getKey() + ">" + c.getValue() + "</" + c.getKey() + ">").collect(Collectors.joining())
          + "</x>").orElse("") + Optional.ofNullable("".equals(body) ? null : body)
        .map(b -> String.format("<body>%s</body>", b)).orElse("<body/>") + (replyId == null ? ""
        : "<reply xmlns='urn:xmpp:reply:0' " + String.format("id='%s' ", replyId) + String.format(
          "to='%s@muclight.carbonio'", roomId) + "/>") + "</message>");
  }

  public HttpRequest getSendStanzaRequest(String xmppMessage) {
    return getRequest("POST", "{\"query\":\"mutation stanza { stanza { sendStanza (stanza: \\\"\\\"\\\""
      + xmppMessage + "\\\"\\\"\\\") { id } } }\",\"operationName\":\"stanza\",\"variables\":{}}");
  }

  public void mockSendStanza(
    String roomId, String senderId, @Nullable String type, List<SimpleEntry<String, String>> content,
    @Nullable String body, @Nullable String messageId, @Nullable String replyId, boolean success
  ) {
    HttpRequest request = getSendStanzaRequest(roomId, senderId, type, content, body, messageId, replyId);
    clear(request);
    when(request).respond(getResponse(success));
  }

  public void mockSendStanza(String xmppMessage, boolean success) {
    HttpRequest request = getSendStanzaRequest(xmppMessage);
    clear(request);
    when(request).respond(getResponse(success));
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping mongooseIM mock...");
    super.close();
  }
}
