// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Called after a message was sent or edited, or a reaction was set, through the REST API and the
 * change has been stored and broadcast to the room members: by {@code POST
 * /rooms/{roomId}/messages} with the message text, by {@code POST /rooms/{roomId}/attachments} once
 * per uploaded attachment with its caption, by the message edit with the new text, and by the
 * reaction set with the emoji (also when the same emoji is set again; removals do not call it).
 * {@code text} is the text a notification shows (caption, new text, emoji) and may be blank.
 * Forwards and deletes do not call it. An edit or a reaction may target a message older than the
 * visibility floor of some room members; {@code createdAt} is the creation time of the message the
 * notification is about (the new message, the edited message, the reaction target) and is provided
 * for the floor check: an implementation that notifies room members must not reach members who
 * cannot see that message. Implementations are registered with a Guice {@code Multibinder}; none is
 * registered by default. Listeners run on the request thread and must not block; their failures are
 * logged and do not affect the response.
 */
public interface MessageSentListener {

  void onMessageSent(
      UserPrincipal sender, UUID roomId, String messageId, String text, OffsetDateTime createdAt);
}
