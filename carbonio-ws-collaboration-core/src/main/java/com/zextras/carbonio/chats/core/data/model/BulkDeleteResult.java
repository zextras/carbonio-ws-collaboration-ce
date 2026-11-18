// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import java.util.List;
import java.util.UUID;

public record BulkDeleteResult(List<UUID> successIds, List<UUID> failedIds) {}
