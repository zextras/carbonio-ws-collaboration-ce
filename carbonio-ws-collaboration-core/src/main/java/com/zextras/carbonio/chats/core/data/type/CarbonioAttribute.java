// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.type;

import java.time.Duration;
import java.util.Map;

public enum CarbonioAttribute {
  FEATURE_WSC_ENABLED("carbonioFeatureWscEnabled"),
  WSC_VIDEO_CALL_ENABLED("carbonioWscVideoCallEnabled"),
  WSC_RECORDING_ENABLED("carbonioWscRecordingEnabled"),
  WSC_VIRTUAL_BACKGROUND_ENABLED("carbonioWscVirtualBackgroundEnabled"),
  WSC_PRIVATE_CHAT_CREATION("carbonioWscPrivateChatCreation"),
  WSC_GROUP_CHAT_CREATION("carbonioWscGroupChatCreation"),
  WSC_ATTACHMENT_UPLOAD("carbonioWscAttachmentUpload"),
  WSC_MAX_GROUP_MEMBERS("carbonioWscMaxGroupMembers"),
  WSC_MAX_ATTACHMENT_SIZE("carbonioWscMaxAttachmentSize"),
  WSC_MAX_ROOM_PICTURE_SIZE("carbonioWscMaxRoomPictureSize"),
  WSC_MESSAGE_EDIT_TIME_LIMIT("carbonioWscMessageEditTimeLimit"),
  WSC_MESSAGE_DELETE_TIME_LIMIT("carbonioWscMessageDeleteTimeLimit"),
  WSC_SHOW_USERS_PRESENCE("carbonioWscShowUsersPresence"),
  WSC_SHOW_MESSAGE_READS("carbonioWscShowMessageReads");

  final String value;

  CarbonioAttribute(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public boolean asBoolean(Map<String, String> attributes) {
    return Boolean.TRUE
        .toString()
        .equalsIgnoreCase(attributes.getOrDefault(value, Boolean.FALSE.toString()));
  }

  public int asInt(Map<String, String> attributes, int defaultValue) {
    String raw = attributes.get(value);
    if (raw == null || raw.isBlank()) return defaultValue;
    try {
      long parsed = Long.parseLong(raw.trim());
      return parsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) parsed;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public Size getSize(Map<String, String> attributes, long defaultValueMb) {
    String raw = attributes.get(value);
    if (raw == null || raw.isBlank()) {
      return Size.of(defaultValueMb, SizeUnit.MB);
    }
    try {
      long value = Long.parseLong(raw.trim());
      return Size.of(value, SizeUnit.MB);
    } catch (NumberFormatException e) {
      return Size.of(defaultValueMb, SizeUnit.MB);
    }
  }

  public Duration asDuration(Map<String, String> attributes, Duration defaultValue) {
    String raw = attributes.get(value);
    if (raw == null || raw.isBlank()) return defaultValue;
    try {
      if (raw.trim().endsWith("s")) return Duration.ofSeconds(Long.parseLong(raw.replace("s", "")));
      if (raw.trim().endsWith("m")) return Duration.ofMinutes(Long.parseLong(raw.replace("m", "")));
      if (raw.trim().endsWith("h")) return Duration.ofHours(Long.parseLong(raw.replace("h", "")));
      return defaultValue;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
