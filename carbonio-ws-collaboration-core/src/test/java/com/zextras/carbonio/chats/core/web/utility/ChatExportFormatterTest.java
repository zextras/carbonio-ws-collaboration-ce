// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only
package com.zextras.carbonio.chats.core.web.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ChatExportFormatterTest {

  private final ChatExportFormatter formatter = new ChatExportFormatter();
  private final OffsetDateTime utc = OffsetDateTime.of(2026, 6, 15, 8, 30, 5, 0, ZoneOffset.UTC);

  @Test
  void plainText_rendersInGivenZone() {
    String line =
        formatter.formatLine(
            utc, ZoneId.of("Europe/Rome"), "Mario Rossi", "ciao", false, null, "Deleted message");
    assertEquals("[2026-06-15 10:30:05] Mario Rossi: ciao\n", line);
  }

  @Test
  void deleted_usesPlaceholder() {
    String line =
        formatter.formatLine(
            utc, ZoneOffset.UTC, "Anna", "ignored", true, null, "Messaggio eliminato");
    assertEquals("[2026-06-15 08:30:05] Anna: [Messaggio eliminato]\n", line);
  }

  @Test
  void attachment_prefixesName() {
    String line =
        formatter.formatLine(
            utc, ZoneOffset.UTC, "Mario", "guarda", false, "foto.jpg", "Deleted message");
    assertEquals("[2026-06-15 08:30:05] Mario: [foto.jpg] guarda\n", line);
  }

  @Test
  void nullText_rendersEmpty() {
    String line =
        formatter.formatLine(utc, ZoneOffset.UTC, "Mario", null, false, null, "Deleted message");
    assertEquals("[2026-06-15 08:30:05] Mario: \n", line);
  }
}
