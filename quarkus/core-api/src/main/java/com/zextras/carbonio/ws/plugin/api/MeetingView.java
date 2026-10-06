// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

import java.util.List;
import java.util.UUID;

public record MeetingView(
    UUID id, String name, UUID roomId, boolean active, List<UUID> participantUserIds) {}
