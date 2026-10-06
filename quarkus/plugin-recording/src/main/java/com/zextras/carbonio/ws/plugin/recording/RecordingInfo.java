// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

/** {@code serverId} is null on a single-videoserver installation. */
public record RecordingInfo(
    String serverId, String meetingId, String meetingName, String accountId, String folderId) {}
