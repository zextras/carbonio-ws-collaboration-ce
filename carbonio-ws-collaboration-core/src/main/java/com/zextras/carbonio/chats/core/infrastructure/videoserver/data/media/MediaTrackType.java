// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media;

public enum MediaTrackType {
  AUDIO("a"),
  VIDEO_OUT("vo"),
  VIDEO_IN("vi"),
  SCREEN("s");

  private final String type;

  MediaTrackType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public static MediaTrackType fromString(String type) {
    for (MediaTrackType mediaTrackType : MediaTrackType.values()) {
      if (mediaTrackType.getType().equalsIgnoreCase(type)) {
        return mediaTrackType;
      }
    }
    throw new IllegalArgumentException("Unexpected value: " + type);
  }
}
