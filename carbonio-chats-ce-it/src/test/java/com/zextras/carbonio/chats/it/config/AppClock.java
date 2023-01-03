// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class AppClock extends Clock {

  private Instant instant;
  private ZoneId  zone;

  public AppClock(ZoneId zone) {
    this.zone = zone;
  }

  public static AppClock create(ZoneId zone) {
    return new AppClock(zone);
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public AppClock withZone(ZoneId zone) {
    this.zone = zone;
    return this;
  }

  @Override
  public Instant instant() {
    if (instant == null) {
      return Clock.systemUTC().instant();
    } else {
      return instant;
    }
  }

  @Override
  public long millis() {
    if(instant == null) {
      return Clock.systemUTC().millis();
    } else {
      return instant.toEpochMilli();
    }
  }

  public AppClock fixTimeAt(Instant instant) {
    this.instant = instant;
    return this;
  }

  public void removeFixTime() {
    this.instant = null;
  }
}
