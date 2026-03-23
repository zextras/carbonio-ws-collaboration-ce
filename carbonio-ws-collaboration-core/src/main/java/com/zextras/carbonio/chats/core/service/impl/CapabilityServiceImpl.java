// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.data.type.SizeUnit;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.CapabilitiesDto;
import java.time.Duration;

@Singleton
public class CapabilityServiceImpl implements CapabilityService {

  @Override
  public CapabilitiesDto getCapabilities(UserPrincipal currentUser) {
    return CapabilitiesDto.create()
        .videoCallEnabled(currentUser.hasEnabled(CarbonioAttribute.WSC_VIDEO_CALL_ENABLED))
        .recordingEnabled(currentUser.hasEnabled(CarbonioAttribute.WSC_RECORDING_ENABLED))
        .virtualBackgroundEnabled(
            currentUser.hasEnabled(CarbonioAttribute.WSC_VIRTUAL_BACKGROUND_ENABLED))
        .privateChatCreationEnabled(
            currentUser.hasEnabled(CarbonioAttribute.WSC_PRIVATE_CHAT_CREATION))
        .groupChatCreationEnabled(currentUser.hasEnabled(CarbonioAttribute.WSC_GROUP_CHAT_CREATION))
        .attachmentUploadEnabled(currentUser.hasEnabled(CarbonioAttribute.WSC_ATTACHMENT_UPLOAD))
        .maxGroupMembers(currentUser.getCountLimit(CarbonioAttribute.WSC_MAX_GROUP_MEMBERS, 0))
        .maxAttachmentSize(
            currentUser.getSizeLimit(CarbonioAttribute.WSC_MAX_ATTACHMENT_SIZE, 0).to(SizeUnit.MB))
        .maxRoomPictureSize(
            currentUser
                .getSizeLimit(CarbonioAttribute.WSC_MAX_ROOM_PICTURE_SIZE, 0)
                .to(SizeUnit.MB))
        .messageEditTimeLimit(
            currentUser
                .getDurationLimit(CarbonioAttribute.WSC_MESSAGE_EDIT_TIME_LIMIT, Duration.ZERO)
                .toMinutes())
        .messageDeleteTimeLimit(
            currentUser
                .getDurationLimit(CarbonioAttribute.WSC_MESSAGE_DELETE_TIME_LIMIT, Duration.ZERO)
                .toMinutes())
        .showMessageReads(currentUser.hasEnabled(CarbonioAttribute.WSC_SHOW_MESSAGE_READS))
        .showUsersPresence(currentUser.hasEnabled(CarbonioAttribute.WSC_SHOW_USERS_PRESENCE));
  }
}
