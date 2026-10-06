// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerConfig;

public class VideoServerConfigImpl implements VideoServerConfig {

  private String apiSecret;
  private Integer bitrate;
  private Boolean bitrateCap;

  @Override
  public String getApiSecret() {
    return apiSecret;
  }

  @Override
  public int getBitrate() {
    return bitrate;
  }

  @Override
  public boolean getBitrateCap() {
    return bitrateCap;
  }

  public VideoServerConfigImpl apiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
    return this;
  }

  public VideoServerConfigImpl bitrate(Integer bitrate) {
    this.bitrate = bitrate;
    return this;
  }

  public VideoServerConfigImpl bitrateCap(Boolean bitrateCap) {
    this.bitrateCap = bitrateCap;
    return this;
  }
}
