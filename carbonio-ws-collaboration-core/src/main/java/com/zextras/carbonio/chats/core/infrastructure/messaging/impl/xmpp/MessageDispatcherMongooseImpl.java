// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.exception.MessageDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.model.ForwardMessageDto;
import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MessageDispatcherMongooseImpl implements MessageDispatcher {

  private static final String DOMAIN = "muclight.carbonio";

  private final String mongooseimUrl;
  private final ObjectMapper objectMapper;
  private final String authToken;

  public MessageDispatcherMongooseImpl(
      String mongooseimUrl, String username, String password, ObjectMapper objectMapper) {
    this.mongooseimUrl = mongooseimUrl;
    this.authToken =
        String.format(
            "Basic %s",
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
    String query =
        "mutation muc_light { muc_light { createRoom ("
            + String.format("mucDomain: \"%s\", ", DOMAIN)
            + String.format("id: \"%s\", ", room.getId())
            + String.format("owner: \"%s\"", userIdToUserDomain(senderId))
            + ") { jid } } }";
    GraphQlResponse result = executeMutation(GraphQlBody.create(query, "muc_light", Map.of()));

    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while creating a room: %s", objectMapper.writeValueAsString(result.errors)));
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
    GraphQlResponse result =
        executeMutation(
            GraphQlBody.create(
                "mutation muc_light { muc_light { deleteRoom ("
                    + String.format("room: \"%s\") ", roomIdToRoomDomain(roomId))
                    + "} }",
                "muc_light",
                Map.of()));
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while deleting a room: %s", objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void updateRoomName(String roomId, String senderId, String name) {
    GraphQlResponse result =
        sendStanza(
            XmppMessageBuilder.create(roomIdToRoomDomain(roomId), userIdToUserDomain(senderId))
                .type(MessageType.ROOM_NAME_CHANGED)
                .addConfig("value", StringFormatUtils.encodeToUtf8(name), true)
                .build());
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending update room name: %s",
                objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void updateRoomDescription(String roomId, String senderId, String description) {
    GraphQlResponse result =
        sendStanza(
            XmppMessageBuilder.create(roomIdToRoomDomain(roomId), userIdToUserDomain(senderId))
                .type(MessageType.ROOM_DESCRIPTION_CHANGED)
                .addConfig("value", StringFormatUtils.encodeToUtf8(description), true)
                .build());
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending update room description: %s",
                objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void updateRoomPicture(
      String roomId, String senderId, String pictureId, String pictureName) {
    GraphQlResponse result =
        sendStanza(
            XmppMessageBuilder.create(roomIdToRoomDomain(roomId), userIdToUserDomain(senderId))
                .type(MessageType.ROOM_PICTURE_UPDATED)
                .addConfig("picture-id", pictureId)
                .addConfig("picture-name", StringFormatUtils.encodeToUtf8(pictureName), true)
                .build());
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending update room picture: %s",
                objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void deleteRoomPicture(String roomId, String senderId) {
    GraphQlResponse result =
        sendStanza(
            XmppMessageBuilder.create(roomIdToRoomDomain(roomId), userIdToUserDomain(senderId))
                .type(MessageType.ROOM_PICTURE_DELETED)
                .build());
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending update room picture: %s",
                objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void addRoomMember(String roomId, String senderId, String recipientId) {
    GraphQlResponse result =
        executeMutation(
            GraphQlBody.create(
                "mutation muc_light { muc_light { inviteUser ("
                    + String.format("room: \"%s\", ", roomIdToRoomDomain(roomId))
                    + String.format("sender: \"%s\", ", userIdToUserDomain(senderId))
                    + String.format("recipient: \"%s\") ", userIdToUserDomain(recipientId))
                    + "} }",
                "muc_light",
                Map.of()));
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while adding a room member: %s",
                objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
    sendAffiliationMessage(roomId, senderId, recipientId, true);
  }

  @Override
  public void removeRoomMember(String roomId, String senderId, String idToRemove) {
    GraphQlResponse result =
        executeMutation(
            GraphQlBody.create(
                "mutation muc_light { muc_light { kickUser ("
                    + String.format("room: \"%s\", ", roomIdToRoomDomain(roomId))
                    + String.format("user: \"%s\") ", userIdToUserDomain(idToRemove))
                    + "} }",
                "muc_light",
                Map.of()));
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while removing a room member: %s",
                objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
    sendAffiliationMessage(roomId, senderId, idToRemove, false);
  }

  private void sendAffiliationMessage(
      String roomId, String senderId, String memberId, boolean isAdded) {
    GraphQlResponse result =
        sendStanza(
            XmppMessageBuilder.create(roomIdToRoomDomain(roomId), userIdToUserDomain(senderId))
                .type(isAdded ? MessageType.MEMBER_ADDED : MessageType.MEMBER_REMOVED)
                .addConfig("user-id", memberId)
                .build());
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending affiliation message: %s",
                objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void addUsersToContacts(String user1id, String user2id) {
    GraphQlResponse result =
        executeMutation(
            GraphQlBody.create(
                "mutation roster { roster { setMutualSubscription ("
                    + String.format("userA: \"%s\", ", userIdToUserDomain(user1id))
                    + String.format("userB: \"%s\", ", userIdToUserDomain(user2id))
                    + "action: CONNECT) } }",
                "roster",
                Map.of()));
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while setting users contacts: %s",
                objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public void sendAttachment(
      String roomId,
      String senderId,
      FileMetadata metadata,
      String description,
      @Nullable String messageId,
      @Nullable String replyId,
      @Nullable String area) {
    XmppMessageBuilder xmppMsgBuilder =
        XmppMessageBuilder.create(roomIdToRoomDomain(roomId), userIdToUserDomain(senderId))
            .type(MessageType.ATTACHMENT_ADDED)
            .addConfig("attachment-id", metadata.getId())
            .addConfig("filename", StringFormatUtils.encodeToUtf8(metadata.getName()), true)
            .addConfig("mime-type", metadata.getMimeType())
            .addConfig("size", String.valueOf(metadata.getOriginalSize()))
            .body(description)
            .messageId(messageId)
            .replyId(replyId);
    if (area != null) {
      xmppMsgBuilder.addConfig("area", area);
    }
    GraphQlResponse result = sendStanza(xmppMsgBuilder.build());
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending attachment: %s",
                objectMapper.writeValueAsString(result.errors)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Error during parsing the json of response error ", e);
      }
    }
  }

  @Override
  public Optional<String> getAttachmentIdFromMessage(String message) {
    if (message.contains("<operation>attachmentAdded</operation>")) {
      try {
        Element node =
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(message.getBytes()))
                .getDocumentElement();
        Element x = (Element) node.getElementsByTagName("x").item(0);
        if (x != null) {
          Element op = (Element) x.getElementsByTagName("operation").item(0);
          if (op != null
              && MessageType.ATTACHMENT_ADDED.getName().equals(op.getFirstChild().getNodeValue())) {
            return Optional.ofNullable(x.getElementsByTagName("attachment-id").item(0))
                .map(Node::getFirstChild)
                .map(Node::getNodeValue);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return Optional.empty();
  }

  public void forwardMessage(
      String roomId,
      String senderId,
      ForwardMessageDto messageToForward,
      @Nullable FileMetadata fileMetadata) {
    XmppMessageBuilder xmppMessageBuilder =
        XmppMessageBuilder.create(roomIdToRoomDomain(roomId), userIdToUserDomain(senderId))
            .messageToForward(messageToForward.getOriginalMessage())
            .messageToForwardSentAt(messageToForward.getOriginalMessageSentAt())
            .body(messageToForward.getDescription());
    Optional.ofNullable(fileMetadata)
        .ifPresent(
            metadata ->
                xmppMessageBuilder
                    .type(MessageType.ATTACHMENT_ADDED)
                    .addConfig("attachment-id", metadata.getId())
                    .addConfig("filename", StringFormatUtils.encodeToUtf8(metadata.getName()), true)
                    .addConfig("mime-type", metadata.getMimeType())
                    .addConfig("size", String.valueOf(metadata.getOriginalSize())));
    String xmppMessage = xmppMessageBuilder.build();
    GraphQlResponse result = sendStanza(xmppMessage);
    if (result.errors != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending forward message: %s",
                objectMapper.writeValueAsString(result.errors)));
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
      return objectMapper.readValue(response.getEntity().getContent(), GraphQlResponse.class);
    } catch (IOException e) {
      throw new MessageDispatcherException("MongooseIm GraphQL response error", e);
    }
  }

  private GraphQlResponse executeQuery(String query) {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    try {
      HttpGet request =
          new HttpGet(new URIBuilder(mongooseimUrl).addParameter("query", query).build());
      request.addHeader("Authorization", authToken);
      CloseableHttpResponse response = client.execute(request);
      return objectMapper.readValue(response.getEntity().getContent(), GraphQlResponse.class);
    } catch (URISyntaxException e) {
      throw new InternalErrorException(
          String.format("Unable to write the URI for query '%s'", query), e);
    } catch (IOException e) {
      throw new MessageDispatcherException("MongooseIm GraphQL response error", e);
    }
  }

  private GraphQlResponse sendStanza(String message) {
    return executeMutation(
        GraphQlBody.create(
            "mutation stanza { stanza { sendStanza ("
                + String.format("stanza: \"\"\"%s\"\"\") ", message)
                + "{ id } } }",
            "stanza",
            Map.of()));
  }

  private String roomIdToRoomDomain(String roomId) {
    return String.join("@", roomId, "muclight.carbonio");
  }

  private String userIdToUserDomain(String userId) {
    return String.join("@", userId, "carbonio");
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class GraphQlResponse {

    private Object data;
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
  }
}
