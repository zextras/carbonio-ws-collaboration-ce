// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerConfig;

public class VideoServerConfigImpl implements VideoServerConfig {

  private final String apiSecret;

  public VideoServerConfigImpl(String apiSecret) {
    this.apiSecret = apiSecret;
  }

  @Override
  public String getApiSecret() {
    return apiSecret;
  }
}
