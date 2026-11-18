// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Message;
import java.util.Map;

/**
 * The data needed to render messages: the visible reply targets and the attachment metadata of the
 * messages and of their reply targets, both keyed by id.
 */
public record MessageContext(
    Map<String, Message> repliedById, Map<String, FileMetadata> attachmentsById) {}
