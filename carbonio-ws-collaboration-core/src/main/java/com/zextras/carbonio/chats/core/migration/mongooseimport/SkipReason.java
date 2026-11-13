// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

enum SkipReason {
  ROW_NULL("row without a body"),
  ROW_OVERSIZE("row larger than the size limit"),
  ROW_BINARY("row that is not XML (eterm/binary)"),
  ROW_EMPTY("empty row"),
  ROW_INVALID_XML("row with invalid XML"),
  ROW_NOT_MESSAGE("row whose root is not a message"),
  ROW_PARSE_ERROR("row that could not be parsed"),
  ROW_IMPORT_ERROR("row that could not be imported"),
  INVALID_UTF8("row with invalid UTF-8 replaced"),
  MESSAGE_CORRECT("message-correct row ignored"),
  AFFILIATIONS("affiliations row ignored"),
  UNKNOWN_CONFIG_OP("unknown configuration operation ignored"),
  FORWARD_WITHOUT_BODY("forward without body ignored"),
  UNKNOWN_APPLY_TO("unknown apply-to ignored"),
  NO_BODY("row without body ignored"),
  INVALID_MAM_TIME("row with an invalid MAM time"),
  INVALID_SENDER("row with an invalid sender"),
  INVALID_MEMBER_ID("member event with an invalid user id"),
  MEMBER_EVENT_ONE_TO_ONE("member event in a 1:1 room"),
  MEETING_LINE_SKIPPED("meeting line skipped"),
  UNRESOLVED_PIN("pin or unpin line with an unresolved reference"),
  UNRESOLVED_EDIT("edit with an unresolved reference"),
  UNRESOLVED_REACTION("reaction with an unresolved reference"),
  UNRESOLVED_REPLY("reply with an unresolved reference"),
  EDIT_DROPPED("edit dropped in the fold"),
  REACTION_DROPPED("reaction dropped in the fold"),
  MARKER_BAD_USER("read marker with a non-UUID user"),
  LAST_SEEN_BAD_USER("last seen with a non-UUID user"),
  PIN_BAD_USER("pin with a non-UUID user"),
  PIN_UNRESOLVED("pin that does not resolve to a message"),
  ORPHAN_ARCHIVE("source archive without a target room"),
  TIME_CLAMPED("MAM time later than the import start, clamped"),
  STAMP_CLAMPED("stamp later than the import start, clamped"),
  RETRACT_STAMP_RAISED("retraction stamp earlier than the message, raised"),
  LAST_SEEN_NONPOSITIVE("last seen with a non-positive time skipped"),
  LAST_SEEN_FUTURE("last seen later than the import start, clamped"),
  PIN_TIME_OUT_OF_RANGE("pin time out of range ignored"),
  ROOM_CREATED_AT_OUT_OF_RANGE("room created_at out of range, replaced"),
  MEETING_DURATION_DROPPED("meeting duration out of range dropped"),
  MEETING_STARTED_AT_DROPPED("meeting startedAt out of range dropped"),
  FORWARD_AS_PLAIN("forward imported as a plain message"),
  FORWARD_STAMP_MISSING("forward without a usable stamp");

  private final String label;

  SkipReason(String label) {
    this.label = label;
  }

  String label() {
    return label;
  }
}
