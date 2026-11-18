// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import java.util.List;

/** The pinned messages of a room with what is needed to render them for one room member. */
public record PinnedMessagesView(List<MessagePin> pins, MessageContext context) {}
