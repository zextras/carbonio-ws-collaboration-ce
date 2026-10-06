// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import java.util.UUID;

public interface VideoServerGateway {

  Optional<VideoServerRooms> roomsOf(UUID meetingId);

  /** Sends {@code body} to the meeting's videoroom handle; returns the raw Janus response. */
  JsonNode sendVideoRoomMessage(UUID meetingId, Object body);

  /** Sends {@code body} to the meeting's audiobridge handle; returns the raw Janus response. */
  JsonNode sendAudioBridgeMessage(UUID meetingId, Object body);
}
