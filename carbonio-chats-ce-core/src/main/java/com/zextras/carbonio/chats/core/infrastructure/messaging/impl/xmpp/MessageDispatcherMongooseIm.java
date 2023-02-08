// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.exception.MessageDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.StandardExtensionElement.Builder;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jxmpp.stringprep.XmppStringprepException;

public class MessageDispatcherMongooseIm implements MessageDispatcher {

  private static final String DOMAIN = "muclight.carbonio";

  private final String       mongooseimUrl;
  private final ObjectMapper objectMapper;
  private final String       authToken;

  public MessageDispatcherMongooseIm(
    String mongooseimUrl, String username, String password, ObjectMapper objectMapper
  ) {
    this.mongooseimUrl = mongooseimUrl;
    this.authToken = String.format("Basic %s",
      Base64.getEncoder().encodeToString(String.join(":", username, password).getBytes()));
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean isAlive() {
    try {
      return executeQuery("query checkAuth { checkAuth { authStatus } }").getErrors() == null;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public void createRoom(Room room, String senderId) {
    GraphQlResponse result = executeMutation(GraphQlBody.create(
      "mutation muc_light { muc_light { createRoom (" +
        String.format("mucDomain: \"%s\", ", DOMAIN) +
        String.format("id: \"%s\", ", room.getId()) +
        String.format("owner: \"%s\", ", userIdToUserDomain(senderId)) +
        String.format("name: \"%s\", ", room.getName()) +
        String.format("subject: \"%s\") ", room.getDescription()) +
        "{ jid } } }", "muc_light", Map.of()));
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
          String.format("Error while creating a room: %s", objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
    room.getSubscriptions().stream()
      .filter(member -> !member.getUserId().equals(senderId))
      .forEach(member -> addRoomMember(room.getId(), senderId, member.getUserId()));
  }

  @Override
  public void deleteRoom(String roomId, String userId) {
    GraphQlResponse result = executeMutation(GraphQlBody.create(
      "mutation muc_light { muc_light { deleteRoom (" +
        String.format("room: \"%s\") ", roomIdToRoomDomain(roomId)) +
        "} }", "muc_light", Map.of()));
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
          String.format("Error while deleting a room: %s", objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void updateRoomName(String roomId, String senderId, String name) {
    GraphQlResponse result = sendStanza(roomId, senderId, MessageType.ROOM_NAME_CHANGED, Map.of("value", name), null);
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
          String.format("Error while sending update room name: %s", objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void updateRoomDescription(String roomId, String senderId, String description) {
    GraphQlResponse result = sendStanza(roomId, senderId, MessageType.ROOM_DESCRIPTION_CHANGED,
      Map.of("value", description), null);
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
          String.format("Error while sending update room description: %s",
            objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void updateRoomPicture(String roomId, String senderId, String pictureId, String pictureName) {
    GraphQlResponse result = sendStanza(roomId, senderId, MessageType.ROOM_PICTURE_UPDATED,
      Map.of("picture-id", pictureId, "picture-name", pictureName), null);
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
          String.format("Error while sending update room picture: %s",
            objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void deleteRoomPicture(String roomId, String senderId) {
    GraphQlResponse result = sendStanza(roomId, senderId, MessageType.ROOM_PICTURE_DELETED, null, null);
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
          String.format("Error while sending update room picture: %s",
            objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void addRoomMember(String roomId, String senderId, String recipientId) {
    GraphQlResponse result = executeMutation(GraphQlBody.create(
      "mutation muc_light { muc_light { inviteUser (" +
        String.format("room: \"%s\", ", roomIdToRoomDomain(roomId)) +
        String.format("sender: \"%s\", ", userIdToUserDomain(senderId)) +
        String.format("recipient: \"%s\") ", userIdToUserDomain(recipientId)) +
        "} }", "muc_light", Map.of()));
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
          String.format("Error while adding a room member: %s", objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void removeRoomMember(String roomId, String senderId, String idToRemove) {
    GraphQlResponse result = executeMutation(GraphQlBody.create(
      "mutation muc_light { muc_light { kickUser (" +
        String.format("room: \"%s\", ", roomIdToRoomDomain(roomId)) +
        String.format("user: \"%s\") ", userIdToUserDomain(idToRemove)) +
        "} }", "muc_light", Map.of()));
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
          String.format("Error while removing a room member: %s", objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void addUsersToContacts(String user1id, String user2id) {
    GraphQlResponse result = executeMutation(GraphQlBody.create(
      "mutation roster { roster { setMutualSubscription (" +
        String.format("userA: \"%s\", ", userIdToUserDomain(user1id)) +
        String.format("userB: \"%s\", ", userIdToUserDomain(user2id)) +
        "action: CONNECT) } }", "roster", Map.of()));
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
          String.format("Error while setting users contacts: %s", objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void setMemberRole(String roomId, String senderId, String recipientId, boolean isOwner) {
    GraphQlResponse result = sendStanza(roomId, senderId, MessageType.MEMBER_ROLE_CHANGED,
      Map.of("recipient", userIdToUserDomain(recipientId), "role", isOwner ? "OWNER" : "MEMBER"), null);
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
          String.format("Error while sending update room name: %s", objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void sendAttachment(String roomId, String senderId, String attachmentId, String fileName, String description) {
    GraphQlResponse result = sendStanza(roomId, senderId, MessageType.ATTACHMENT_ADDED,
      Map.of("attachment-id", attachmentId, "filename", fileName), description);
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
          String.format("Error while sending attachment: %s", objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  private GraphQlResponse executeMutation(GraphQlBody body) {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpPost request = new HttpPost(mongooseimUrl);
    try {
      request.setEntity(new StringEntity(objectMapper.writeValueAsString(body)));
      request.addHeader("Authorization", authToken);
      request.addHeader("Accept", "application/json");
      request.addHeader("Content-Type", "application/json");
      CloseableHttpResponse response = client.execute(request);
      return objectMapper.readValue(
        response.getEntity().getContent(), GraphQlResponse.class);
    } catch (IOException e) {
      throw new MessageDispatcherException("MongooseIm GraphQL response error", e);
    }
  }

  private GraphQlResponse executeQuery(String query) {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    try {
      HttpGet request = new HttpGet(
        new URIBuilder(mongooseimUrl)
          .addParameter("query", query)
          .build());
      request.addHeader("Authorization", authToken);
      CloseableHttpResponse response = client.execute(request);
      return objectMapper.readValue(
        response.getEntity().getContent(), GraphQlResponse.class);
    } catch (URISyntaxException e) {
      throw new InternalErrorException(String.format("Unable to write the URI for query '%s'", query), e);
    } catch (IOException e) {
      throw new MessageDispatcherException("MongooseIm GraphQL response error", e);
    }
  }

  private GraphQlResponse sendStanza(String roomId, String senderId, MessageType type, Map<String, String> content,
    @Nullable String body) {
    return executeMutation(GraphQlBody.create(
      "mutation stanza { stanza { sendStanza (" +
        String.format("stanza: \"%s\") ", getStanzaMessage(roomId, senderId, type, content, body)) +
        "{ id } } }", "stanza", Map.of()));
  }

  private String getStanzaMessage(String roomId, String senderId, MessageType type, Map<String, String> elementsMap,
    @Nullable String body) {
    try {
      MessageBuilder messageBuilder = StanzaBuilder
        .buildMessage()
        .from(userIdToUserDomain(senderId))
        .to(roomIdToRoomDomain(roomId))
        .ofType(Type.groupchat)
        .addExtension(getStanzaElementX(type, elementsMap));
      if (body != null) {
        messageBuilder.addBody(null, body);
      }
      return messageBuilder
        .build()
        .toXML()
        .toString();
    } catch (XmppStringprepException e) {
      throw new InternalErrorException("Unable to build the stanza message", e);
    }
  }

  private StandardExtensionElement getStanzaElementX(MessageType type, @Nullable Map<String, String> elementsMap) {
    Builder x = StandardExtensionElement.builder("x", "urn:xmpp:muclight:0#configuration")
      .addElement("operation", type.getName());
    if (elementsMap != null) {
      elementsMap.keySet().forEach(k -> x.addElement(k, elementsMap.get(k)));
    }
    return x.build();
  }

  private String roomIdToRoomDomain(String roomId) {
    return String.join("@", roomId, "muclight.carbonio");
  }

  private String userIdToUserDomain(String userId) {
    return String.join("@", userId, "carbonio");
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class GraphQlResponse {

    private Object       data;
    private List<Object> errors;

    public Object getData() {
      return data;
    }

    public void setData(Object data) {
      this.data = data;
    }

    public List<Object> getErrors() {
      return errors;
    }

    public void setErrors(List<Object> errors) {
      this.errors = errors;
    }
  }
}
