// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import java.util.UUID;

/**
 * Plugin access to the meeting's Janus rooms.
 *
 * <p>Implementations must not run the Janus call inside a transactional interceptor: an exception
 * that leaves a {@code @Transactional} (REQUIRED) method marks the <em>caller's</em> transaction
 * rollback-only, so catching it afterwards cannot save the surrounding work (the commit fails).
 * Core's implementation only joins the transaction for the row lookup, which is why a plugin may
 * catch a failure of these methods and still let core's stop/leave/delete commit.
 *
 * @see MeetingAccess for the general rollback-only pitfall
 */
public interface VideoServerGateway {

  Optional<VideoServerRooms> roomsOf(UUID meetingId);

  /** Sends {@code body} to the meeting's videoroom handle; returns the raw Janus response. */
  JsonNode sendVideoRoomMessage(UUID meetingId, Object body);

  /** Sends {@code body} to the meeting's audiobridge handle; returns the raw Janus response. */
  JsonNode sendAudioBridgeMessage(UUID meetingId, Object body);
}
