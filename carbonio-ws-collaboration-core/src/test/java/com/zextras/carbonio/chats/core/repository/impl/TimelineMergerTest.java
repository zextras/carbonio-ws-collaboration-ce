// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.TimelineItem;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class TimelineMergerTest {

  private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-01-01T00:00:00Z");

  private static TimelineItem message(String id, long secondsFromT0) {
    return new TimelineItem.MessageItem(
        Message.create().id(id).createdAt(T0.plusSeconds(secondsFromT0)));
  }

  private static TimelineItem event(String id, long secondsFromT0) {
    return new TimelineItem.SystemEventItem(
        SystemEvent.create().id(id).createdAt(T0.plusSeconds(secondsFromT0)));
  }

  private static List<String> ids(List<TimelineItem> items) {
    return items.stream().map(TimelineItem::id).toList();
  }

  @Test
  @DisplayName("Ascending merge interleaves two already-asc lists by (createdAt, id)")
  void ascendingMerge_interleavesByKey() {
    List<TimelineItem> messages = List.of(message("m1", 1), message("m3", 3), message("m5", 5));
    List<TimelineItem> events = List.of(event("e2", 2), event("e4", 4));

    List<TimelineItem> merged =
        TimelineMerger.merge(messages, events, TimelineMerger.ASCENDING, 10);

    assertEquals(List.of("m1", "e2", "m3", "e4", "m5"), ids(merged));
  }

  @Test
  @DisplayName("Descending merge interleaves two already-desc lists by (createdAt, id)")
  void descendingMerge_interleavesByKey() {
    List<TimelineItem> messages = List.of(message("m5", 5), message("m3", 3), message("m1", 1));
    List<TimelineItem> events = List.of(event("e4", 4), event("e2", 2));

    List<TimelineItem> merged =
        TimelineMerger.merge(messages, events, TimelineMerger.DESCENDING, 10);

    assertEquals(List.of("m5", "e4", "m3", "e2", "m1"), ids(merged));
  }

  @Test
  @DisplayName("cap stops the merge as soon as enough items are produced")
  void cap_stopsEarly() {
    List<TimelineItem> messages = List.of(message("m1", 1), message("m3", 3), message("m5", 5));
    List<TimelineItem> events = List.of(event("e2", 2), event("e4", 4));

    List<TimelineItem> merged = TimelineMerger.merge(messages, events, TimelineMerger.ASCENDING, 3);

    assertEquals(List.of("m1", "e2", "m3"), ids(merged));
  }

  @Test
  @DisplayName("Items sharing a timestamp are ordered by id ascending across the two sources")
  void sameTimestamp_orderedById() {
    List<TimelineItem> messages = List.of(message("m-zzz", 1));
    List<TimelineItem> events = List.of(event("e-aaa", 1));

    List<TimelineItem> merged =
        TimelineMerger.merge(messages, events, TimelineMerger.ASCENDING, 10);

    assertEquals(List.of("e-aaa", "m-zzz"), ids(merged));
  }

  @Test
  @DisplayName("On a full (createdAt, id) tie the first list wins")
  void fullTie_firstListWins() {
    List<TimelineItem> first = List.of(message("dup", 1));
    List<TimelineItem> second = List.of(event("dup", 1));

    List<TimelineItem> merged = TimelineMerger.merge(first, second, TimelineMerger.ASCENDING, 10);

    assertEquals(2, merged.size());
    assertTrue(
        merged.get(0) instanceof TimelineItem.MessageItem,
        "first list (messages) must win the tie");
  }

  @Test
  @DisplayName("Empty inputs produce an empty merge")
  void emptyInputs() {
    assertEquals(
        List.of(), TimelineMerger.merge(List.of(), List.of(), TimelineMerger.ASCENDING, 10));
  }
}
