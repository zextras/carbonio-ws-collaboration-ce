// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.meeting.dto;

import java.util.UUID;

public record CreateMeetingRequest(String name, String meetingType, UUID roomId) {}
