// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class ExpectedIds {

  public static final String MESSAGE_SLOT = "m";
  public static final String SYSTEM_EVENT_SLOT = "s";

  private ExpectedIds() {}

  public static String message(String roomId, long mamId) {
    return of(MESSAGE_SLOT, roomId, mamId);
  }

  public static String systemEvent(String roomId, long mamId) {
    return of(SYSTEM_EVENT_SLOT, roomId, mamId);
  }

  public static String of(String slot, String roomId, long mamId) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(MongooseImport.RUN_KEY, "HmacSHA256"));
      byte[] h = mac.doFinal((slot + ":" + roomId + ":" + mamId).getBytes(StandardCharsets.UTF_8));
      h[6] = (byte) ((h[6] & 0x0F) | 0x40);
      h[8] = (byte) ((h[8] & 0x3F) | 0x80);
      ByteBuffer buffer = ByteBuffer.wrap(h, 0, 16);
      return new UUID(buffer.getLong(), buffer.getLong()).toString();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
