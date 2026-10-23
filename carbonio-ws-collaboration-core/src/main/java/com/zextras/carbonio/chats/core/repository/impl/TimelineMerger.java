// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.model.TimelineItem;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class TimelineMerger {

  private TimelineMerger() {}

  static final Comparator<TimelineItem> ASCENDING =
      Comparator.comparing(
              (TimelineItem item) ->
                  item.createdAt() != null ? item.createdAt() : OffsetDateTime.MIN)
          .thenComparing(item -> item.id() != null ? item.id() : "");

  static final Comparator<TimelineItem> DESCENDING = ASCENDING.reversed();

  // Linear 2-way merge of two lists that are ALREADY sorted by `order`, stopping as soon as `cap`
  // items are produced. On a tie the first list wins, mirroring the stable order of the DB fetch.
  static List<TimelineItem> merge(
      List<TimelineItem> first,
      List<TimelineItem> second,
      Comparator<TimelineItem> order,
      int cap) {
    List<TimelineItem> merged = new ArrayList<>(Math.min(cap, first.size() + second.size()));
    int i = 0;
    int j = 0;
    while (merged.size() < cap && (i < first.size() || j < second.size())) {
      if (i >= first.size()) {
        merged.add(second.get(j++));
      } else if (j >= second.size()) {
        merged.add(first.get(i++));
      } else if (order.compare(first.get(i), second.get(j)) <= 0) {
        merged.add(first.get(i++));
      } else {
        merged.add(second.get(j++));
      }
    }
    return merged;
  }
}
