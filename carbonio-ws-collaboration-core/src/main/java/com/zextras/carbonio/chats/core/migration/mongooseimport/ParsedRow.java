// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.time.Instant;

public record ParsedRow(
    Kind kind,
    String rootId,
    String text,
    String replyRef,
    String applyToRef,
    Instant retractedStamp,
    String attachmentId,
    String area,
    String forwardFrom,
    Instant forwardStamp,
    String configUserId,
    String pinRef,
    Long durationSec,
    Long startedAtMillis) {

  public enum Kind {
    TEXT,
    TOMBSTONE,
    EDIT,
    REACTION,
    RETRACT_REQUEST,
    FORWARD,
    ATTACHMENT,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    PINNED,
    UNPINNED,
    MEETING_STARTED,
    MEETING_ENDED,
    MEETING_DECLINED,
    ROOM_NAME_CHANGED,
    ROOM_DESCRIPTION_CHANGED,
    ROOM_PICTURE_UPDATED,
    ROOM_PICTURE_DELETED,
    ROOM_HISTORY_CLEARED,
    SKIP
  }

  public static ParsedRow skip() {
    return new ParsedRow(
        Kind.SKIP, null, "", null, null, null, null, null, null, null, null, null, null, null);
  }
}
