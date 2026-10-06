// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api.event;

import java.util.UUID;

/**
 * Fired synchronously, inside the core transaction, when a meeting is about to be stopped or
 * deleted — before its videoserver rooms are destroyed. {@code userId} caused the stop.
 */
public record MeetingStopping(UUID meetingId, UUID userId) {}
