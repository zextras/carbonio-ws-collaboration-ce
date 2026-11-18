// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import jakarta.annotation.Nullable;
import java.util.List;

public record AttachmentsPage(
    long total, List<FileMetadata> attachments, @Nullable String cursor) {}
