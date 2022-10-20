package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.CapabilitiesDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
public class CapabilityServiceImplTest {

  private final AppConfig         appConfig;
  private final CapabilityService capabilityService;

  public CapabilityServiceImplTest() {
    this.appConfig = mock(AppConfig.class);
    this.capabilityService = new CapabilityServiceImpl(appConfig);
  }

  @Test
  @DisplayName("Returns default user capability")
  public void getCapabilities_defaultValuesTestOk() {
    CapabilitiesDto capabilities = capabilityService.getCapabilities(UserPrincipal.create(UUID.randomUUID()));

    assertNotNull(capabilities);
    assertEquals(false, capabilities.isCanVideoCall());
    assertEquals(false, capabilities.isCanVideoCallRecord());
    assertEquals(false, capabilities.isCanUseVirtualBackground());
    assertEquals(true, capabilities.isCanSeeMessageReads());
    assertEquals(true, capabilities.isCanSeeUsersPresence());
    assertEquals(10, capabilities.getEditMessageTimeLimitInMinutes());
    assertEquals(10, capabilities.getDeleteMessageTimeLimitInMinutes());
    assertEquals(128, capabilities.getMaxGroupMembers());
    assertEquals(256, capabilities.getMaxRoomImageSizeInKb());
    assertEquals(256, capabilities.getMaxUserImageSizeInKb());

    verify(appConfig, times(1)).get(Boolean.class, ConfigName.CAN_SEE_MESSAGE_READS);
    verify(appConfig, times(1)).get(Boolean.class, ConfigName.CAN_SEE_USERS_PRESENCE);
    verify(appConfig, times(1)).get(Integer.class, ConfigName.MAX_USER_IMAGE_SIZE_IN_KB);
    verify(appConfig, times(1)).get(Integer.class, ConfigName.MAX_ROOM_IMAGE_SIZE_IN_KB);
    verify(appConfig, times(1)).get(Integer.class, ConfigName.EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES);
    verify(appConfig, times(1)).get(Integer.class, ConfigName.DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES);
    verify(appConfig, times(1)).get(Integer.class, ConfigName.MAX_GROUP_MEMBERS);
    verifyNoMoreInteractions(appConfig);
  }

  @Test
  @DisplayName("Returns configured user capability ")
  public void getCapabilities_configuredValuesTestOk() {
    when(appConfig.get(Boolean.class, ConfigName.CAN_SEE_MESSAGE_READS)).thenReturn(Optional.of(false));
    when(appConfig.get(Boolean.class, ConfigName.CAN_SEE_USERS_PRESENCE)).thenReturn(Optional.of(false));
    when(appConfig.get(Integer.class, ConfigName.MAX_USER_IMAGE_SIZE_IN_KB)).thenReturn(Optional.of(512));
    when(appConfig.get(Integer.class, ConfigName.MAX_ROOM_IMAGE_SIZE_IN_KB)).thenReturn(Optional.of(512));
    when(appConfig.get(Integer.class, ConfigName.EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES)).thenReturn(Optional.of(15));
    when(appConfig.get(Integer.class, ConfigName.DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES)).thenReturn(Optional.of(15));
    when(appConfig.get(Integer.class, ConfigName.MAX_GROUP_MEMBERS)).thenReturn(Optional.of(20));

    CapabilitiesDto capabilities = capabilityService.getCapabilities(UserPrincipal.create(UUID.randomUUID()));

    assertNotNull(capabilities);
    assertEquals(false, capabilities.isCanVideoCall());
    assertEquals(false, capabilities.isCanVideoCallRecord());
    assertEquals(false, capabilities.isCanUseVirtualBackground());
    assertEquals(false, capabilities.isCanSeeMessageReads());
    assertEquals(false, capabilities.isCanSeeUsersPresence());
    assertEquals(15, capabilities.getEditMessageTimeLimitInMinutes());
    assertEquals(15, capabilities.getDeleteMessageTimeLimitInMinutes());
    assertEquals(20, capabilities.getMaxGroupMembers());
    assertEquals(512, capabilities.getMaxRoomImageSizeInKb());
    assertEquals(512, capabilities.getMaxUserImageSizeInKb());

    verify(appConfig, times(1)).get(Boolean.class, ConfigName.CAN_SEE_MESSAGE_READS);
    verify(appConfig, times(1)).get(Boolean.class, ConfigName.CAN_SEE_USERS_PRESENCE);
    verify(appConfig, times(1)).get(Integer.class, ConfigName.MAX_USER_IMAGE_SIZE_IN_KB);
    verify(appConfig, times(1)).get(Integer.class, ConfigName.MAX_ROOM_IMAGE_SIZE_IN_KB);
    verify(appConfig, times(1)).get(Integer.class, ConfigName.EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES);
    verify(appConfig, times(1)).get(Integer.class, ConfigName.DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES);
    verify(appConfig, times(1)).get(Integer.class, ConfigName.MAX_GROUP_MEMBERS);
    verifyNoMoreInteractions(appConfig);
  }
}
