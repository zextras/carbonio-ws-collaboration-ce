// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only
package com.zextras.carbonio.chats.core.web.utility;

import com.google.inject.Singleton;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Singleton
public class ChatExportFormatter {

  private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * Builds a single export line, matching the legacy frontend formatter exactly. Always
   * newline-terminated. {@code attachmentName != null} selects the attachment branch (pass an empty
   * string for an attachment with no known name); deleted takes precedence over attachment.
   */
  public String formatLine(
      OffsetDateTime createdAt,
      ZoneId zone,
      String senderName,
      String text,
      boolean deleted,
      String attachmentName,
      String deletedPlaceholder) {
    String ts = createdAt.atZoneSameInstant(zone).format(TS);
    String header = "[" + ts + "] " + senderName + ": ";
    if (deleted) {
      return header + "[" + deletedPlaceholder + "]\n";
    }
    String body = text == null ? "" : text;
    if (attachmentName != null) {
      return header + "[" + attachmentName + "] " + body + "\n";
    }
    return header + body + "\n";
  }
}
