// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.model.TimelinePage;
import java.time.OffsetDateTime;

/**
 * Read access to a room timeline as a single stream of {@link
 * com.zextras.carbonio.chats.core.data.model.TimelineItem}s. Each method fetches messages and
 * system events with the same {@code (created_at, id)} keyset, merges them and derives the frontier
 * flags from the merged set, so callers never re-implement the union. All methods return items in
 * ascending chronological order.
 *
 * <p>{@code after} is the effective lower bound already resolved by the caller (e.g. folding in
 * {@code clearedAt}); {@code limit} is the requested page size (the {@code limit + 1} over-fetch is
 * internal).
 */
public interface TimelineRepository {

  /** Newest page (no anchor). Sets {@code hasMoreBefore}. */
  TimelinePage findLatest(String roomId, OffsetDateTime after, int limit);

  /**
   * Page strictly older than the composite cursor {@code (before, beforeId)} (or {@code before}
   * alone when {@code beforeId} is null). Sets {@code hasMoreBefore}.
   */
  TimelinePage findBefore(
      String roomId, OffsetDateTime before, String beforeId, OffsetDateTime after, int limit);

  /** Page strictly newer than {@code after}. Sets {@code hasMoreAfter}. */
  TimelinePage findAfter(String roomId, OffsetDateTime after, int limit);

  /** Page centered on {@code around}: half older + half newer. Sets both frontier flags. */
  TimelinePage findAround(String roomId, OffsetDateTime around, OffsetDateTime after, int limit);
}
