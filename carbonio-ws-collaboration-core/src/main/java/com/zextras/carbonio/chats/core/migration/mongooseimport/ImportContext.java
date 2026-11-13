// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ImportContext {

  private static final int SAMPLES = 3;

  private final Instant t0;
  private final Map<SkipReason, Integer> counts = new EnumMap<>(SkipReason.class);
  private final Map<SkipReason, List<String>> samples = new EnumMap<>(SkipReason.class);
  private long rowsRead;

  public ImportContext(Instant t0) {
    this.t0 = t0.truncatedTo(ChronoUnit.MICROS);
  }

  public static ImportContext start() {
    return new ImportContext(Instant.now());
  }

  public Instant t0() {
    return t0;
  }

  synchronized void count(SkipReason reason) {
    counts.merge(reason, 1, Integer::sum);
  }

  synchronized void count(SkipReason reason, String room, long mam) {
    count(reason);
    List<String> list = samples.computeIfAbsent(reason, r -> new ArrayList<>());
    if (list.size() < SAMPLES) {
      list.add(mam > 0 ? room + "/" + mam : String.valueOf(room));
    }
  }

  synchronized void count(SkipReason reason, int amount) {
    if (amount > 0) {
      counts.merge(reason, amount, Integer::sum);
    }
  }

  synchronized int of(SkipReason reason) {
    return counts.getOrDefault(reason, 0);
  }

  synchronized void rowRead() {
    rowsRead++;
  }

  synchronized long rowsRead() {
    return rowsRead;
  }

  synchronized String summary() {
    StringBuilder out = new StringBuilder();
    for (Map.Entry<SkipReason, Integer> entry : counts.entrySet()) {
      if (entry.getValue() == 0) {
        continue;
      }
      if (!out.isEmpty()) {
        out.append("; ");
      }
      out.append(entry.getKey().name().toLowerCase(java.util.Locale.ROOT))
          .append('=')
          .append(entry.getValue());
      List<String> sample = samples.get(entry.getKey());
      if (sample != null) {
        out.append(" (e.g. ").append(String.join(", ", sample)).append(')');
      }
    }
    return out.toString();
  }
}
