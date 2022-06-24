package com.zextras.carbonio.chats.core.infrastructure.messaging.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp.MessageDispatcherMongooseIm;
import com.zextras.carbonio.chats.mongooseim.admin.api.CommandsApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.ContactsApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.MucLightManagementApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.OneToOneMessagesApi;
import com.zextras.carbonio.chats.mongooseim.admin.model.Message1Dto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class MessageDispatcherMongooseImTest {

  private final MessageDispatcherMongooseIm messageDispatcher;
  private final MucLightManagementApi       mucLightManagementApi;
  private final CommandsApi                 commandsApi;
  private final ContactsApi                 contactsApi;
  private final OneToOneMessagesApi         oneToOneMessagesApi;

  public MessageDispatcherMongooseImTest() {
    mucLightManagementApi = mock(MucLightManagementApi.class);
    commandsApi = mock(CommandsApi.class);
    contactsApi = mock(ContactsApi.class);
    oneToOneMessagesApi = mock(OneToOneMessagesApi.class);
    messageDispatcher = new MessageDispatcherMongooseIm(
      mucLightManagementApi,
      commandsApi,
      contactsApi,
      oneToOneMessagesApi
    );
  }

  @Nested
  @DisplayName("Remove room member tests")
  class RemoveRoomMemberTests {

    @Test
    @DisplayName("Correctly sends the iq")
    public void removeRoomMember_testOk() {
      messageDispatcher.removeRoomMember("roomtest", "testuser", "otheruser");
      verify(oneToOneMessagesApi, times(1)).stanzasPost(new Message1Dto().stanza(
        "<iq xmlns='jabber:client' to='roomtest@muclight.carbonio' from='testuser@carbonio' id='remove-member' type='set'>"
          + "<query xmlns='urn:xmpp:muclight:0#affiliations'>"
          + "<user affiliation='none'>otheruser@carbonio</user>"
          + "</query>"
          + "</iq>"
      ));
    }

  }

}