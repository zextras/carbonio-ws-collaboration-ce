// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import java.time.Duration;

@Singleton
public class CacheHandler {

  private final Cache<String, VideoServerSession> videoServerSessionCache;

  @Inject
  public CacheHandler() {
    this.videoServerSessionCache =
        Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(5)).build();
  }

  public Cache<String, VideoServerSession> getVideoServerSessionCache() {
    return videoServerSessionCache;
  }
}
