// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp.MessageDispatcherMongooseIm;
import com.zextras.carbonio.chats.mongooseim.admin.api.CommandsApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.ContactsApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.MucLightManagementApi;
import com.zextras.carbonio.chats.mongooseim.admin.api.OneToOneMessagesApi;
import com.zextras.carbonio.chats.mongooseim.admin.invoker.ApiException;
import com.zextras.carbonio.chats.mongooseim.admin.model.AffiliationDetailsDto;
import com.zextras.carbonio.chats.mongooseim.admin.model.AffiliationDetailsDto.AffiliationEnum;
import org.junit.jupiter.api.Assertions;
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
    @DisplayName("Correctly removes a user from the room")
    public void removeRoomMember_testOk() {
      messageDispatcher.removeRoomMember("roomtest", "testuser", "otheruser");
      verify(mucLightManagementApi, times(1))
        .mucLightsXMPPMUCHostRoomNameUserAffiliationPut("carbonio", "roomtest", "testuser@carbonio",
          new AffiliationDetailsDto().target("otheruser@carbonio").affiliation(AffiliationEnum.NONE));
    }

    @Test
    @DisplayName("Throws exception if the request throws one")
    public void removeRoomMember_testThrowsException() {
      doThrow(new ApiException()).when(mucLightManagementApi)
        .mucLightsXMPPMUCHostRoomNameUserAffiliationPut("carbonio", "roomtest", "testuser@carbonio",
          new AffiliationDetailsDto().target("otheruser@carbonio").affiliation(AffiliationEnum.NONE));
      assertThrows(ApiException.class,
        () -> messageDispatcher.removeRoomMember("roomtest", "testuser", "otheruser"));
    }

  }

  @Nested
  @DisplayName("Set member role tests")
  class SetMemberRoleTests {

    @Test
    @DisplayName("Correctly sets a member as owner")
    public void setMemberRole_testAddOwnerOk() {
      messageDispatcher.setMemberRole("testRoom", "user", "testTarget", true);
      verify(mucLightManagementApi, times(1))
        .mucLightsXMPPMUCHostRoomNameUserAffiliationPut("carbonio", "testRoom", "user@carbonio",
          new AffiliationDetailsDto().target("testTarget@carbonio").affiliation(AffiliationEnum.OWNER));
    }

    @Test
    @DisplayName("Re-throws and exception if something went wrong")
    public void setMemberRole_testAddOwnerException() {
      doThrow(RuntimeException.class).when(mucLightManagementApi)
        .mucLightsXMPPMUCHostRoomNameUserAffiliationPut("carbonio", "testRoom", "user@carbonio",
          new AffiliationDetailsDto().target("testTarget@carbonio").affiliation(AffiliationEnum.OWNER));
      assertThrows(RuntimeException.class,
        () -> messageDispatcher.setMemberRole("testRoom", "user", "testTarget", true));
    }

    @Test
    @DisplayName("Correctly demotes an owner to member")
    public void setMemberRole_TestRemoveOwnerOk() {
      messageDispatcher.setMemberRole("testRoom", "user", "testTarget", false);
      verify(mucLightManagementApi, times(1))
        .mucLightsXMPPMUCHostRoomNameUserAffiliationPut("carbonio", "testRoom", "user@carbonio",
          new AffiliationDetailsDto().target("testTarget@carbonio").affiliation(AffiliationEnum.MEMBER));
    }

    @Test
    @DisplayName("Re-throws and exception if something went wrong")
    public void setMemberRole_testRemoveOwnerException() {
      doThrow(RuntimeException.class).when(mucLightManagementApi)
        .mucLightsXMPPMUCHostRoomNameUserAffiliationPut("carbonio", "testRoom", "user@carbonio",
          new AffiliationDetailsDto().target("testTarget@carbonio").affiliation(AffiliationEnum.MEMBER));
      assertThrows(RuntimeException.class,
        () -> messageDispatcher.setMemberRole("testRoom", "user", "testTarget", false));
    }

  }

}