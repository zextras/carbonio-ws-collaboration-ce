// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.model.AttachmentDto;
import jakarta.annotation.Nullable;
import java.util.List;

public interface AttachmentMapper {

  /**
   * Converts a {@link FileMetadata} to {@link AttachmentDto}
   *
   * @param metadata {@link FileMetadata} to convert
   * @return Conversation result {@link AttachmentDto}
   */
  @Nullable
  AttachmentDto ent2dto(@Nullable FileMetadata metadata);

  /**
   * Converts a {@link List} of {@link FileMetadata} to a {@link List} of {@link AttachmentDto}
   *
   * @param metadataList {@link List} of {@link FileMetadata} to convert
   * @return Conversation result ({@link List} of {@link AttachmentDto})
   */
  List<AttachmentDto> ent2dto(@Nullable List<FileMetadata> metadataList);
}
