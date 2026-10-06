// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.persistence;

import java.io.Serializable;

public record ParticipantId(String meetingId, String userId) implements Serializable {}
