// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.meeting.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ParticipantDto(
    UUID userId,
    UUID queueId,
    boolean audioStreamEnabled,
    boolean videoStreamEnabled,
    boolean screenStreamEnabled,
    OffsetDateTime joinedAt) {}
