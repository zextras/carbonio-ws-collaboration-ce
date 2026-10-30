// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import java.util.UUID;

/**
 * Called after a message, text or attachment, sent through the REST API has been stored and
 * broadcast to the room members: by {@code POST /rooms/{roomId}/messages} with the message text,
 * and by {@code POST /rooms/{roomId}/attachments} once per uploaded attachment with its caption
 * ({@code ""} when it has none). Edits, reactions, forwards and deletes do not call it.
 * Implementations are registered with a Guice {@code Multibinder}; none is registered by default.
 * Listeners run on the request thread and must not block; their failures are logged and do not
 * affect the response.
 */
public interface MessageSentListener {

  void onMessageSent(UserPrincipal sender, UUID roomId, String messageId, String text);
}
