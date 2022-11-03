package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.CapabilitiesDto;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CapabilityServiceImpl implements CapabilityService {

  private final AppConfig appConfig;

  @Inject
  public CapabilityServiceImpl(AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  public CapabilitiesDto getCapabilities(UserPrincipal currentUser) {
    return CapabilitiesDto.create()
      .canVideoCall(false)
      .canVideoCallRecord(false)
      .canUseVirtualBackground(false)
      .canSeeMessageReads(appConfig.get(Boolean.class, ConfigName.CAN_SEE_MESSAGE_READS)
        .orElse(ChatsConstant.CONFIGURATIONS_DEFAULT_VALUES.CAN_SEE_MESSAGE_READS))
      .canSeeUsersPresence(appConfig.get(Boolean.class, ConfigName.CAN_SEE_USERS_PRESENCE)
        .orElse(ChatsConstant.CONFIGURATIONS_DEFAULT_VALUES.CAN_SEE_USERS_PRESENCE))
      .editMessageTimeLimitInMinutes(appConfig.get(Integer.class, ConfigName.EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES)
        .orElse(ChatsConstant.CONFIGURATIONS_DEFAULT_VALUES.EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES))
      .deleteMessageTimeLimitInMinutes(appConfig.get(Integer.class, ConfigName.DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES)
        .orElse(ChatsConstant.CONFIGURATIONS_DEFAULT_VALUES.DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES))
      .maxGroupMembers(appConfig.get(Integer.class, ConfigName.MAX_GROUP_MEMBERS)
        .orElse(ChatsConstant.CONFIGURATIONS_DEFAULT_VALUES.MAX_GROUP_MEMBERS))
      .maxRoomImageSizeInKb(appConfig.get(Integer.class, ConfigName.MAX_ROOM_IMAGE_SIZE_IN_KB)
        .orElse(ChatsConstant.CONFIGURATIONS_DEFAULT_VALUES.MAX_ROOM_IMAGE_SIZE_IN_KB))
      .maxUserImageSizeInKb(appConfig.get(Integer.class, ConfigName.MAX_USER_IMAGE_SIZE_IN_KB)
        .orElse(ChatsConstant.CONFIGURATIONS_DEFAULT_VALUES.MAX_USER_IMAGE_SIZE_IN_KB));
  }
}