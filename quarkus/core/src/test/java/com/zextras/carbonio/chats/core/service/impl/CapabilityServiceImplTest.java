// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.CapabilitiesDto;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class CapabilityServiceImplTest {

  private final CapabilityService capabilityService;

  public CapabilityServiceImplTest() {
    this.capabilityService = new CapabilityServiceImpl();
  }

  @Test
  @DisplayName("Returns default user capabilities")
  void getCapabilities_defaultValuesTestOk() {
    UserPrincipal user =
        UserPrincipal.create(UUID.randomUUID())
            .carbonioAttributes(
                Map.ofEntries(
                    Map.entry(CarbonioAttribute.WSC_VIDEO_CALL_ENABLED.getValue(), "TRUE"),
                    Map.entry(CarbonioAttribute.WSC_RECORDING_ENABLED.getValue(), "TRUE"),
                    Map.entry(CarbonioAttribute.WSC_VIRTUAL_BACKGROUND_ENABLED.getValue(), "TRUE"),
                    Map.entry(CarbonioAttribute.WSC_PRIVATE_CHAT_CREATION.getValue(), "TRUE"),
                    Map.entry(CarbonioAttribute.WSC_GROUP_CHAT_CREATION.getValue(), "TRUE"),
                    Map.entry(CarbonioAttribute.WSC_ATTACHMENT_UPLOAD.getValue(), "TRUE"),
                    Map.entry(CarbonioAttribute.WSC_MAX_GROUP_MEMBERS.getValue(), "128"),
                    Map.entry(CarbonioAttribute.WSC_MAX_ATTACHMENT_SIZE.getValue(), "128"),
                    Map.entry(CarbonioAttribute.WSC_MAX_ROOM_PICTURE_SIZE.getValue(), "2"),
                    Map.entry(CarbonioAttribute.WSC_MESSAGE_EDIT_TIME_LIMIT.getValue(), "10m"),
                    Map.entry(CarbonioAttribute.WSC_MESSAGE_DELETE_TIME_LIMIT.getValue(), "10m"),
                    Map.entry(CarbonioAttribute.WSC_SHOW_USERS_PRESENCE.getValue(), "TRUE"),
                    Map.entry(CarbonioAttribute.WSC_SHOW_MESSAGE_READS.getValue(), "TRUE")));

    CapabilitiesDto capabilities = capabilityService.getCapabilities(user);

    assertNotNull(capabilities);
    assertTrue(capabilities.isVideoCallEnabled());
    assertTrue(capabilities.isRecordingEnabled());
    assertTrue(capabilities.isVirtualBackgroundEnabled());
    assertTrue(capabilities.isPrivateChatCreationEnabled());
    assertTrue(capabilities.isGroupChatCreationEnabled());
    assertTrue(capabilities.isAttachmentUploadEnabled());
    assertEquals(128, capabilities.getMaxGroupMembers());
    assertEquals(128, capabilities.getMaxAttachmentSize());
    assertEquals(2, capabilities.getMaxRoomPictureSize());
    assertEquals(10, capabilities.getMessageEditTimeLimit());
    assertEquals(10, capabilities.getMessageDeleteTimeLimit());
    assertTrue(capabilities.isShowUsersPresence());
    assertTrue(capabilities.isShowMessageReads());
  }

  @Test
  @DisplayName("Returns configured user capabilities")
  void getCapabilities_configuredValuesTestOk() {
    UserPrincipal user =
        UserPrincipal.create(UUID.randomUUID())
            .carbonioAttributes(
                Map.ofEntries(
                    Map.entry(CarbonioAttribute.WSC_VIDEO_CALL_ENABLED.getValue(), "TRUE"),
                    Map.entry(CarbonioAttribute.WSC_RECORDING_ENABLED.getValue(), "FALSE"),
                    Map.entry(CarbonioAttribute.WSC_VIRTUAL_BACKGROUND_ENABLED.getValue(), "FALSE"),
                    Map.entry(CarbonioAttribute.WSC_PRIVATE_CHAT_CREATION.getValue(), "TRUE"),
                    Map.entry(CarbonioAttribute.WSC_GROUP_CHAT_CREATION.getValue(), "TRUE"),
                    Map.entry(CarbonioAttribute.WSC_ATTACHMENT_UPLOAD.getValue(), "TRUE"),
                    Map.entry(CarbonioAttribute.WSC_MAX_GROUP_MEMBERS.getValue(), "64"),
                    Map.entry(CarbonioAttribute.WSC_MAX_ATTACHMENT_SIZE.getValue(), "512"),
                    Map.entry(CarbonioAttribute.WSC_MAX_ROOM_PICTURE_SIZE.getValue(), "5"),
                    Map.entry(CarbonioAttribute.WSC_MESSAGE_EDIT_TIME_LIMIT.getValue(), "5m"),
                    Map.entry(CarbonioAttribute.WSC_MESSAGE_DELETE_TIME_LIMIT.getValue(), "15m"),
                    Map.entry(CarbonioAttribute.WSC_SHOW_USERS_PRESENCE.getValue(), "FALSE"),
                    Map.entry(CarbonioAttribute.WSC_SHOW_MESSAGE_READS.getValue(), "TRUE")));

    CapabilitiesDto capabilities = capabilityService.getCapabilities(user);

    assertNotNull(capabilities);
    assertTrue(capabilities.isVideoCallEnabled());
    assertFalse(capabilities.isRecordingEnabled());
    assertFalse(capabilities.isVirtualBackgroundEnabled());
    assertTrue(capabilities.isPrivateChatCreationEnabled());
    assertTrue(capabilities.isGroupChatCreationEnabled());
    assertTrue(capabilities.isAttachmentUploadEnabled());
    assertEquals(64, capabilities.getMaxGroupMembers());
    assertEquals(512, capabilities.getMaxAttachmentSize());
    assertEquals(5, capabilities.getMaxRoomPictureSize());
    assertEquals(5, capabilities.getMessageEditTimeLimit());
    assertEquals(15, capabilities.getMessageDeleteTimeLimit());
    assertFalse(capabilities.isShowUsersPresence());
    assertTrue(capabilities.isShowMessageReads());
  }
}
