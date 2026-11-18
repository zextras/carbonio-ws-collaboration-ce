// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import com.zextras.carbonio.chats.core.data.entity.Message;
import java.util.List;
import java.util.Set;

/** A page of search hits; {@code visibleReplyIds} are the reply targets the requester can see. */
public record MessageSearchPage(
    List<Message> messages, boolean hasMore, Set<String> visibleReplyIds) {}
