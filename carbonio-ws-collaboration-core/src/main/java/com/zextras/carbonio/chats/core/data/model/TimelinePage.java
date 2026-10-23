// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import java.util.List;

/**
 * A merged, sorted and trimmed page of {@link TimelineItem}s in ascending chronological order, with
 * the frontier flags derived from the merged item set.
 */
public record TimelinePage(List<TimelineItem> items, boolean hasMoreBefore, boolean hasMoreAfter) {}
