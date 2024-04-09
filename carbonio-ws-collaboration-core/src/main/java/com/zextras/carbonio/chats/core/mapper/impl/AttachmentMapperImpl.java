// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper.impl;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.model.AttachmentDto;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class AttachmentMapperImpl implements AttachmentMapper {

  @Override
  @Nullable
  public AttachmentDto ent2dto(@Nullable FileMetadata metadata) {
    if (metadata == null) {
      return null;
    }
    return AttachmentDto.create()
        .id(UUID.fromString(metadata.getId()))
        .name(metadata.getName())
        .size(metadata.getOriginalSize())
        .mimeType(metadata.getMimeType())
        .userId(UUID.fromString(metadata.getUserId()))
        .roomId(UUID.fromString(metadata.getRoomId()))
        .createdAt(metadata.getCreatedAt());
  }

  @Override
  public List<AttachmentDto> ent2dto(List<FileMetadata> metadataList) {
    return metadataList == null
        ? List.of()
        : metadataList.stream()
            .map(fileMetadata -> ent2dto(fileMetadata))
            .collect(Collectors.toList());
  }
}
