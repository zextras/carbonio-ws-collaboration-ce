package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.exception.MessageDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import com.zextras.carbonio.chats.mongooseim.admin.api.ContactsApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.MucLightManagementApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.OneToOneMessagesApi;
import com.zextras.carbonio.chats.mongooseim.admin.model.AffiliationDetailsDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.AffiliationDetailsDto.AffiliationEnum;
import com.zextras.carbonio.chats.mongooseim.admin.model.ChatMessageDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.Message1Dto;
import com.zextras.carbonio.chats.mongooseim.admin.model.SubscriptionActionDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.SubscriptionActionDto.ActionEnum;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.StandardExtensionElement.Builder;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jxmpp.stringprep.XmppStringprepException;

public class MessageDispatcherGraphQl implements MessageDispatcher {

  private static final String GRAPHQL_ENDPOINT = "http://127.78.0.10:10000/api/graphql";

  private final MucLightManagementApi mucLightManagementApi;
  private final ContactsApi           contactsApi;
  private final OneToOneMessagesApi   oneToOneMessagesApi;

  @Inject
  public MessageDispatcherGraphQl(
    MucLightManagementApi mucLightManagementApi,
    ContactsApi contactsApi,
    OneToOneMessagesApi oneToOneMessagesApi
  ) {
    this.oneToOneMessagesApi = oneToOneMessagesApi;
    this.contactsApi = contactsApi;
    this.mucLightManagementApi = mucLightManagementApi;
  }

  @Override
  public boolean isAlive() {
    return true;
  }

  @Override
  public void createRoom(Room room, String senderId) {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpGet request = new HttpGet(GRAPHQL_ENDPOINT);
    URI uri;
    try {
      uri = new URIBuilder(request.getURI())
        .addParameter("mutation", String.format(
          "mutation muc_light {\n"
            + "  muc_light {\n"
            + "    createRoom (mucDomain: \"muclight.carbonio\", name: \"%s\", owner: \"%s\", subject: \"%s\"){\n"
            + "      jid,\n"
            + "      name,\n"
            + "      subject,\n"
            + "      participants{\n"
            + "        jid,\n"
            + "        affiliation\n"
            + "      },\n"
            + "      options{\n"
            + "        key,\n"
            + "        value\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}  ",
          room.getName(),
          userId2userDomain(senderId),
          room.getDescription()
        ))
        .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    request.setURI(uri);
    try {
      client.execute(request);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteRoom(String roomId, String userId) {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpGet request = new HttpGet(GRAPHQL_ENDPOINT);
    URI uri;
    try {
      uri = new URIBuilder(request.getURI())
        .addParameter("mutation", String.format(
          "mutation muc_light {\n"
            + "  muc_light {\n"
            + "    deleteRoom (room: \"%s\") \n"
            + "  }\n"
            + "}",
          roomId2roomDomain(roomId)
        ))
        .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    request.setURI(uri);
    try {
      client.execute(request);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void updateRoomName(String roomId, String senderId, String name) {
    try {
      oneToOneMessagesApi.stanzasPost(new Message1Dto().stanza(
        getStanzaMessage(roomId, senderId, MessageType.CHANGED_ROOM_NAME, Map.of("value", name))));
    } catch (XmppStringprepException e) {
      throw new MessageDispatcherException(
        "An error occurred when sending a message for room name changed to a MongooseIm room", e);
    }
  }

  @Override
  public void updateRoomDescription(String roomId, String senderId, String description) {
    try {
      oneToOneMessagesApi.stanzasPost(new Message1Dto().stanza(
        getStanzaMessage(roomId, senderId, MessageType.CHANGED_ROOM_DESCRIPTION, Map.of("value", description))));
    } catch (XmppStringprepException e) {
      throw new MessageDispatcherException(
        "An error occurred when sending a message for room description changed to a MongooseIm room", e);
    }
  }

  @Override
  public void updateRoomPicture(String roomId, String senderId, String pictureId, String pictureName) {
    try {
      oneToOneMessagesApi.stanzasPost(new Message1Dto().stanza(
        getStanzaMessage(roomId, senderId, MessageType.UPDATED_ROOM_PICTURE,
          Map.of("picture-id", pictureId, "picture-name", pictureName))));
    } catch (XmppStringprepException e) {
      throw new MessageDispatcherException(
        "An error occurred when sending a message for room picture updated to a MongooseIm room", e);
    }
  }

  @Override
  public void deleteRoomPicture(String roomId, String senderId) {
    try {
      oneToOneMessagesApi.stanzasPost(new Message1Dto().stanza(
        getStanzaMessage(roomId, senderId, MessageType.DELETED_ROOM_PICTURE, null)));
    } catch (XmppStringprepException e) {
      throw new MessageDispatcherException(
        "An error occurred when sending a message for room picture updated to a MongooseIm room", e);
    }
  }

  @Override
  public void addRoomMember(String roomId, String senderId, String recipientId) {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpGet request = new HttpGet(GRAPHQL_ENDPOINT);
    URI uri;
    try {
      uri = new URIBuilder(request.getURI())
        .addParameter("mutation", String.format(
          "mutation muc_light {\n"
            + "  muc_light {\n"
            + "    inviteUser (room: \"%s\", sender: \"%s\", recipient: \"%s\") \n"
            + "  }\n"
            + "}",
          roomId2roomDomain(roomId),
          userId2userDomain(senderId),
          userId2userDomain(recipientId)
        ))
        .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    request.setURI(uri);
    try {
      client.execute(request);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void removeRoomMember(String roomId, String senderId, String idToRemove) {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpGet request = new HttpGet(GRAPHQL_ENDPOINT);
    URI uri;
    try {
      uri = new URIBuilder(request.getURI())
        .addParameter("mutation", String.format(
          "mutation muc_light {\n"
            + "  muc_light {\n"
            + "    kickUser(room: \"%s\", user: \"%s\")\n"
            + "  }\n"
            + "}",
          roomId2roomDomain(roomId),
          userId2userDomain(idToRemove)
        ))
        .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    request.setURI(uri);
    try {
      client.execute(request);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void addUsersToContacts(String user1id, String user2id) {
    if (user1id.equals(user2id)) {
      return;
    }
    String user1jid = userId2userDomain(user1id);
    String user2jid = userId2userDomain(user2id);
    contactsApi.contactsUserContactManagePut(user1jid, user2jid,
      new SubscriptionActionDto().action(ActionEnum.CONNECT));
  }

  @Override
  public void setMemberRole(String roomId, String senderId, String recipientId, boolean isOwner) {
    mucLightManagementApi.mucLightsXMPPMUCHostRoomNameUserAffiliationPut("muclight.carbonio", roomId,
      userId2userDomain(senderId),
      new AffiliationDetailsDto().target(userId2userDomain(recipientId))
        .affiliation(isOwner ? AffiliationEnum.OWNER : AffiliationEnum.MEMBER));
  }

  @Override
  public void sendMessageToRoom(String roomId, String senderId, String message) {
    mucLightManagementApi.mucLightsXMPPMUCHostRoomNameMessagesPost(
      "muclight.carbonio", roomId, new ChatMessageDto()
        .from(userId2userDomain(senderId))
        .body(message)
    );
  }

  private StandardExtensionElement getStanzasElementX(MessageType type, @Nullable Map<String, String> elementsMap) {
    Builder x = StandardExtensionElement.builder("x", "urn:xmpp:muclight:0#configuration")
      .addElement("operation", type.getName());
    if (elementsMap != null) {
      elementsMap.keySet().forEach(k -> x.addElement(k, elementsMap.get(k)));
    }
    return x.build();
  }

  private String getStanzaMessage(String roomId, String senderId, MessageType type, Map<String, String> elementsMap)
    throws XmppStringprepException {
    return StanzaBuilder
      .buildMessage()
      .from(userId2userDomain(senderId))
      .to(roomId2roomDomain(roomId))
      .ofType(Type.groupchat)
      .addExtension(getStanzasElementX(type, elementsMap))
      .build()
      .toXML()
      .toString();
  }

  private String roomId2roomDomain(String roomId) {
    return String.join("@", roomId, "muclight.carbonio");
  }

  private String userId2userDomain(String userId) {
    return String.join("@", userId, "carbonio");
  }
}
