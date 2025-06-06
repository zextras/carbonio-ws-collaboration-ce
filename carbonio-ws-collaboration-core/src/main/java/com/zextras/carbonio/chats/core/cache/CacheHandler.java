// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import java.time.Duration;

@Singleton
public class CacheHandler {

  private final Cache<String, UserProfile> userProfileCache;

  @Inject
  public CacheHandler() {
    this.userProfileCache =
        Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(60)).maximumSize(100).build();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(this.userProfileCache::invalidateAll, "Cache handler shutdown hook"));
  }

  public Cache<String, UserProfile> getUserProfileCache() {
    return userProfileCache;
  }
}
