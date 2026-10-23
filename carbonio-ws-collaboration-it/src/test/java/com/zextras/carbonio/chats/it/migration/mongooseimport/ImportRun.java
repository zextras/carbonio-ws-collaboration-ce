// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.util.List;

public record ImportRun(
    String targetDatabase, Throwable failure, List<String> logLines, ZoneId jvmZone) {

  public boolean succeeded() {
    return failure == null;
  }

  public ImportedChats chats() {
    return new ImportedChats(targetDatabase);
  }

  public String failureText() {
    if (failure == null) {
      return "";
    }
    StringWriter writer = new StringWriter();
    failure.printStackTrace(new PrintWriter(writer));
    return writer.toString();
  }

  public String allOutput() {
    return String.join("\n", logLines) + "\n" + failureText();
  }
}
