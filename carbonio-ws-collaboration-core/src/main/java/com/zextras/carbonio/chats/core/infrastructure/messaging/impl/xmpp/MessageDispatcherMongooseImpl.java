// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.exception.MessageDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import com.zextras.carbonio.chats.model.ForwardMessageDto;
import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Singleton
public class MessageDispatcherMongooseImpl implements MessageDispatcher {

  private static final String MONGOOSEIM_GRAPHQL_ENDPOINT = "/api/graphql";

  private static final String PARSING_ERROR = "Error during parsing the json of response error ";
  private static final String ATTACHMENT_ID = "attachment-id";

  private static final String DOMAIN = "carbonio";
  private static final String MUC_LIGHT = "muc_light";
  private static final String MUC_DOMAIN = "muclight.carbonio";

  private final HttpClient httpClient;
  private final String mongooseimURL;
  private final ObjectMapper objectMapper;
  private final String authToken;

  @Inject
  public MessageDispatcherMongooseImpl(
      HttpClient httpClient, String mongooseimURL, String authToken, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.mongooseimURL = mongooseimURL;
    this.authToken = authToken;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean isAlive() {
    try {
      return executeHealthCheckQuery().getErrors() == null;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public void createRoom(String roomId, String senderId, List<String> memberIds) {
    String query =
        "mutation muc_light { muc_light { createRoom ("
            + String.format("mucDomain: \"%s\", ", MUC_DOMAIN)
            + String.format("id: \"%s\", ", roomId)
            + String.format("owner: \"%s\"", userIdToUserDomain(senderId))
            + ") { jid } } }";
    GraphQlResponse result = executeMutation(GraphQlBody.create(query, MUC_LIGHT, Map.of()));

    if (result.getErrors() != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while creating a room: %s",
                objectMapper.writeValueAsString(result.getErrors())));
      } catch (JsonProcessingException e) {
        throw new MessageDispatcherException(PARSING_ERROR, e);
      }
    }
    memberIds.forEach(
        member -> {
          addRoomMember(roomId, senderId, member);
          sendAffiliationMessage(roomId, senderId, member, MessageType.MEMBER_ADDED);
        });
  }

  @Override
  public void updateRoomName(String roomId, String senderId, String name) {
    GraphQlResponse result =
        sendStanza(
            XmppMessageBuilder.create(roomIdToRoomDomain(roomId), userIdToUserDomain(senderId))
                .type(MessageType.ROOM_NAME_CHANGED)
                .addConfig("value", name, true)
                .build());
    if (result.getErrors() != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending update room name: %s",
                objectMapper.writeValueAsString(result.getErrors())));
      } catch (JsonProcessingException e) {
        throw new MessageDispatcherException(PARSING_ERROR, e);
      }
    }
  }

  @Override
  public void updateRoomDescription(String roomId, String senderId, String description) {
    GraphQlResponse result =
        sendStanza(
            XmppMessageBuilder.create(roomIdToRoomDomain(roomId), userIdToUserDomain(senderId))
                .type(MessageType.ROOM_DESCRIPTION_CHANGED)
                .addConfig("value", description, true)
                .build());
    if (result.getErrors() != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending update room description: %s",
                objectMapper.writeValueAsString(result.getErrors())));
      } catch (JsonProcessingException e) {
        throw new MessageDispatcherException(PARSING_ERROR, e);
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
                .addConfig("picture-name", pictureName, true)
                .build());
    if (result.getErrors() != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending update room picture: %s",
                objectMapper.writeValueAsString(result.getErrors())));
      } catch (JsonProcessingException e) {
        throw new MessageDispatcherException(PARSING_ERROR, e);
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
    if (result.getErrors() != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending update room picture: %s",
                objectMapper.writeValueAsString(result.getErrors())));
      } catch (JsonProcessingException e) {
        throw new MessageDispatcherException(PARSING_ERROR, e);
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
                MUC_LIGHT,
                Map.of()));
    if (result.getErrors() != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while adding a room member: %s",
                objectMapper.writeValueAsString(result.getErrors())));
      } catch (JsonProcessingException e) {
        throw new MessageDispatcherException(PARSING_ERROR, e);
      }
    }
  }

  @Override
  public void removeRoomMember(String roomId, String userIdToRemove) {
    GraphQlResponse result =
        executeMutation(
            GraphQlBody.create(
                "mutation muc_light { muc_light { kickUser ("
                    + String.format("room: \"%s\", ", roomIdToRoomDomain(roomId))
                    + String.format("user: \"%s\") ", userIdToUserDomain(userIdToRemove))
                    + "} }",
                MUC_LIGHT,
                Map.of()));
    if (result.getErrors() != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while removing a room member: %s",
                objectMapper.writeValueAsString(result.getErrors())));
      } catch (JsonProcessingException e) {
        throw new MessageDispatcherException(PARSING_ERROR, e);
      }
    }
  }

  @Override
  public void sendAffiliationMessage(
      String roomId, String senderId, String memberId, MessageType messageType) {
    GraphQlResponse result =
        sendStanza(
            XmppMessageBuilder.create(roomIdToRoomDomain(roomId), userIdToUserDomain(senderId))
                .type(messageType)
                .addConfig("user-id", memberId)
                .build());
    if (result.getErrors() != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending affiliation message: %s",
                objectMapper.writeValueAsString(result.getErrors())));
      } catch (JsonProcessingException e) {
        throw new MessageDispatcherException(PARSING_ERROR, e);
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
    if (result.getErrors() != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while setting users contacts: %s",
                objectMapper.writeValueAsString(result.getErrors())));
      } catch (JsonProcessingException e) {
        throw new MessageDispatcherException(PARSING_ERROR, e);
      }
    }
  }

  @Override
  public void sendAttachment(
      String roomId,
      String senderId,
      String fileId,
      String fileName,
      String mimeType,
      long originalSize,
      String description,
      @Nullable String messageId,
      @Nullable String replyId,
      @Nullable String area) {
    XmppMessageBuilder xmppMsgBuilder =
        XmppMessageBuilder.create(roomIdToRoomDomain(roomId), userIdToUserDomain(senderId))
            .type(MessageType.ATTACHMENT_ADDED)
            .addConfig(ATTACHMENT_ID, fileId)
            .addConfig("filename", fileName, true)
            .addConfig("mime-type", mimeType)
            .addConfig("size", String.valueOf(originalSize))
            .body(description)
            .messageId(messageId)
            .replyId(replyId);
    if (area != null) {
      xmppMsgBuilder.addConfig("area", area);
    }
    GraphQlResponse result = sendStanza(xmppMsgBuilder.build());
    if (result.getErrors() != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending attachment: %s",
                objectMapper.writeValueAsString(result.getErrors())));
      } catch (JsonProcessingException e) {
        throw new MessageDispatcherException(PARSING_ERROR, e);
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
            return Optional.ofNullable(x.getElementsByTagName(ATTACHMENT_ID).item(0))
                .map(Node::getFirstChild)
                .map(Node::getNodeValue);
          }
        }
      } catch (Exception e) {
        throw new MessageDispatcherException("Something went wrong while parsing the message: ", e);
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
                    .addConfig(ATTACHMENT_ID, metadata.getId())
                    .addConfig("filename", metadata.getName(), true)
                    .addConfig("mime-type", metadata.getMimeType())
                    .addConfig("size", String.valueOf(metadata.getOriginalSize())));
    String xmppMessage = xmppMessageBuilder.build();
    GraphQlResponse result = sendStanza(xmppMessage);
    if (result.getErrors() != null) {
      try {
        throw new MessageDispatcherException(
            String.format(
                "Error while sending forward message: %s",
                objectMapper.writeValueAsString(result.getErrors())));
      } catch (JsonProcessingException e) {
        throw new MessageDispatcherException(PARSING_ERROR, e);
      }
    }
  }

  private GraphQlResponse executeMutation(GraphQlBody body) {
    try (CloseableHttpResponse response =
        httpClient.sendPost(
            mongooseimURL + MONGOOSEIM_GRAPHQL_ENDPOINT,
            Map.of(
                "Authorization",
                String.format("Basic %s", authToken),
                "Accept",
                "application/json",
                "Content-Type",
                "application/json"),
            objectMapper.writeValueAsString(body))) {

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new MessageDispatcherException(
            "MongooseIm returns error on GraphQL mutation request: " + statusCode);
      }

      return objectMapper.readValue(
          IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
          GraphQlResponse.class);
    } catch (IOException e) {
      throw new MessageDispatcherException(
          "Error occurred executing MongooseIm GraphQL mutation: ", e);
    }
  }

  private GraphQlResponse executeHealthCheckQuery() {
    try {
      String urlWithQuery =
          new URIBuilder(mongooseimURL + MONGOOSEIM_GRAPHQL_ENDPOINT)
              .addParameter("query", "query checkAuth { checkAuth { authStatus } }")
              .build()
              .toString();

      try (CloseableHttpResponse response =
          httpClient.sendGet(
              urlWithQuery, Map.of("Authorization", String.format("Basic %s", authToken)))) {

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
          throw new MessageDispatcherException(
              "MongooseIm returns error on GraphQL health check request: " + statusCode);
        }

        return objectMapper.readValue(
            IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
            GraphQlResponse.class);
      }
    } catch (URISyntaxException e) {
      throw new MessageDispatcherException(
          "Unable to construct URI for GraphQL health check query", e);
    } catch (IOException e) {
      throw new MessageDispatcherException(
          "Error occurred executing MongooseIm GraphQL health check: ", e);
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
    return String.join("@", roomId, MUC_DOMAIN);
  }

  private String userIdToUserDomain(String userId) {
    return String.join("@", userId, DOMAIN);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class GraphQlResponse {

    private Object data;
    private List<Object> errors;

    public Object getData() {
      return data;
    }

    public List<Object> getErrors() {
      return errors;
    }
  }
}
