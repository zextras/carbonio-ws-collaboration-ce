// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class FileLinker {

  public record AttachmentDecision(String attachmentId, boolean placeholder) {}

  private record Link(String fileId, String messageId, String area) {}

  private static final Pattern AREA_FORMAT = Pattern.compile("\\d{1,6}x\\d{1,6}");

  private static final String SELECT_FILES =
      "SELECT id, stanza_id FROM chats.file_metadata WHERE room_id = ? AND type = 'ATTACHMENT'";
  private static final String UPDATE_LINK =
      "UPDATE chats.file_metadata SET chat_message_id = ?, area = ? WHERE id = ?";

  private final Connection target;
  private final Set<String> fileIds = new HashSet<>();
  private final Map<String, String> byStanza = new HashMap<>();
  private final Set<String> claimed = new HashSet<>();
  private final List<Link> pending = new ArrayList<>();

  public FileLinker(Connection target) {
    this.target = target;
  }

  public void beginRoom(String roomId) throws SQLException {
    fileIds.clear();
    byStanza.clear();
    claimed.clear();
    pending.clear();
    try (PreparedStatement select = target.prepareStatement(SELECT_FILES)) {
      select.setString(1, roomId);
      try (ResultSet rs = select.executeQuery()) {
        while (rs.next()) {
          String id = rs.getString(1);
          fileIds.add(id);
          String stanza = rs.getString(2);
          if (stanza != null) {
            byStanza.putIfAbsent(stanza, id);
          }
        }
      }
    }
  }

  public AttachmentDecision onAttachment(String messageId, String attachmentId, String area) {
    if (attachmentId == null || !fileIds.contains(attachmentId) || !claimed.add(attachmentId)) {
      return new AttachmentDecision(null, true);
    }
    String storedArea = area != null && AREA_FORMAT.matcher(area).matches() ? area : null;
    pending.add(new Link(attachmentId, messageId, storedArea));
    return new AttachmentDecision(attachmentId, false);
  }

  public String onTombstone(long mam, String messageId) {
    String fileId = byStanza.get(StanzaIds.encode(mam));
    if (fileId == null || !claimed.add(fileId)) {
      return null;
    }
    pending.add(new Link(fileId, messageId, null));
    return fileId;
  }

  public void flush() throws SQLException {
    if (pending.isEmpty()) {
      return;
    }
    try (PreparedStatement update = target.prepareStatement(UPDATE_LINK)) {
      for (Link link : pending) {
        update.setString(1, link.messageId());
        update.setString(2, link.area());
        update.setString(3, link.fileId());
        update.addBatch();
      }
      update.executeBatch();
    }
    pending.clear();
  }
}
