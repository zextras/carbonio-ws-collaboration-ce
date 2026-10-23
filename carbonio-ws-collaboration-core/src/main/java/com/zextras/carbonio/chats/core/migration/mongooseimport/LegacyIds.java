// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class LegacyIds {

  public static final String MESSAGE = "m";
  public static final String SYSTEM_EVENT = "s";
  public static final String ROOM_CREATED = "r";

  private final Mac mac;

  public LegacyIds(byte[] runKey) {
    try {
      mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(runKey, "HmacSHA256"));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HmacSHA256 is not available", e);
    }
  }

  // Ids are computed, not random: MongooseIM references (replies, edits, reactions, pins, retracted
  // files) carry the target's stanza-id, so the new id of any target is recomputed from (room, MAM
  // id) without a lookup table. They are formatted as UUIDv4 for parity with every other id of the
  // app, and keyed by a per-run secret so they cannot be guessed.
  public String of(String slot, String roomId, long mamId) {
    byte[] h = mac.doFinal((slot + ":" + roomId + ":" + mamId).getBytes(StandardCharsets.UTF_8));
    h[6] = (byte) ((h[6] & 0x0F) | 0x40);
    h[8] = (byte) ((h[8] & 0x3F) | 0x80);
    ByteBuffer b = ByteBuffer.wrap(h, 0, 16);
    return new UUID(b.getLong(), b.getLong()).toString();
  }
}
