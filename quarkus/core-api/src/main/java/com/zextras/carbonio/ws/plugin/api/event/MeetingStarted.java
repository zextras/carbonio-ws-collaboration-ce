// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api.event;

import java.util.UUID;

/** Fired synchronously after a meeting became active. */
public record MeetingStarted(UUID meetingId, UUID roomId, UUID userId) {}
