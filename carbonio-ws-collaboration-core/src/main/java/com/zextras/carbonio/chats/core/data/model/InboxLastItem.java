// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import com.zextras.carbonio.chats.core.data.entity.Message;
import jakarta.annotation.Nullable;

public record InboxLastItem(TimelineItem lastItem, @Nullable Message repliedMessage) {}
