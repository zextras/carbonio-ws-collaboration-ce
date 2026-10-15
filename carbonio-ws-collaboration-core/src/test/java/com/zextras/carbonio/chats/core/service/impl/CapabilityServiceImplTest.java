// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.impl.MockAppConfig;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.CapabilitiesDto;
import com.zextras.carbonio.chats.model.SimulcastTierDto;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class CapabilityServiceImplTest {

  private final CapabilityService capabilityService;

  public CapabilityServiceImplTest() {
    this.capabilityService = new CapabilityServiceImpl(MockAppConfig.create());
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

  @Test
  @DisplayName("Returns default simulcast tiers when KV is unset")
  void getCapabilities_simulcastTiersTestOk() {
    CapabilitiesDto capabilities =
        capabilityService.getCapabilities(createUserWithStandardAttributes());

    assertNotNull(capabilities.getVideoSimulcastTiers());
    assertEquals(3, capabilities.getVideoSimulcastTiers().size());
    assertEquals(
        SimulcastTierDto.NameEnum.HIGH, capabilities.getVideoSimulcastTiers().get(0).getName());
    assertEquals(720, capabilities.getVideoSimulcastTiers().get(0).getHeight());
    assertEquals(
        SimulcastTierDto.NameEnum.MEDIUM, capabilities.getVideoSimulcastTiers().get(1).getName());
    assertEquals(360, capabilities.getVideoSimulcastTiers().get(1).getHeight());
    assertEquals(
        SimulcastTierDto.NameEnum.LOW, capabilities.getVideoSimulcastTiers().get(2).getName());
    assertEquals(144, capabilities.getVideoSimulcastTiers().get(2).getHeight());
  }

  @Test
  @DisplayName("Returns simulcast tiers overridden from Consul KV")
  void getCapabilities_simulcastTiersConfiguredTestOk() {
    CapabilityService service =
        new CapabilityServiceImpl(
            MockAppConfig.create()
                .set(ConfigName.VIDEO_SIMULCAST_HIGH_HEIGHT, "1080")
                .set(ConfigName.VIDEO_SIMULCAST_MEDIUM_HEIGHT, "540")
                .set(ConfigName.VIDEO_SIMULCAST_LOW_HEIGHT, "180"));

    CapabilitiesDto capabilities = service.getCapabilities(createUserWithStandardAttributes());

    assertEquals(3, capabilities.getVideoSimulcastTiers().size());
    assertEquals(1080, capabilities.getVideoSimulcastTiers().get(0).getHeight());
    assertEquals(540, capabilities.getVideoSimulcastTiers().get(1).getHeight());
    assertEquals(180, capabilities.getVideoSimulcastTiers().get(2).getHeight());
  }

  private UserPrincipal createUserWithStandardAttributes() {
    return UserPrincipal.create(UUID.randomUUID())
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
  }
}
