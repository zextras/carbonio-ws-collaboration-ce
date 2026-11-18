// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.FileMetadataBuilder;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.web.api.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.model.AttachmentDto;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class AttachmentMapperImplTest {

  private final AttachmentMapper attachmentMapper;

  AttachmentMapperImplTest(AttachmentMapper attachmentMapper) {
    this.attachmentMapper = attachmentMapper;
  }

  private FileMetadata metadata(UUID fileId, UUID userId, UUID roomId, String mimeType, long size) {
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
    return FileMetadataBuilder.create()
        .id(fileId.toString())
        .name("test.pdf")
        .originalSize(size)
        .mimeType(mimeType)
        .type(FileMetadataType.ATTACHMENT)
        .userId(userId.toString())
        .roomId(roomId.toString())
        .createdAt(createdAt)
        .updatedAt(createdAt)
        .build();
  }

  @Test
  @DisplayName("The metadata of a file maps to the attachment info")
  void ent2dto_metadata() {
    UUID fileId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID roomId = UUID.randomUUID();

    AttachmentDto dto =
        attachmentMapper.ent2dto(metadata(fileId, userId, roomId, "application/pdf", 2048L));

    assertEquals(fileId, dto.getId());
    assertEquals("test.pdf", dto.getName());
    assertEquals(2048L, dto.getSize());
    assertEquals("application/pdf", dto.getMimeType());
    assertEquals(userId, dto.getUserId());
    assertEquals(roomId, dto.getRoomId());
    assertEquals(OffsetDateTime.parse("2026-01-01T10:00:00Z"), dto.getCreatedAt());
  }

  @Test
  @DisplayName("A list of metadata maps in order, a missing list maps to an empty list")
  void ent2dto_list() {
    UUID file1Id = UUID.randomUUID();
    UUID file2Id = UUID.randomUUID();
    UUID roomId = UUID.randomUUID();

    List<AttachmentDto> dtos =
        attachmentMapper.ent2dto(
            List.of(
                metadata(file1Id, UUID.randomUUID(), roomId, "image/png", 1L),
                metadata(file2Id, UUID.randomUUID(), roomId, "image/jpeg", 2L)));

    assertEquals(2, dtos.size());
    assertEquals(file1Id, dtos.get(0).getId());
    assertEquals(file2Id, dtos.get(1).getId());
    assertEquals("image/png", dtos.get(0).getMimeType());
    assertEquals(List.of(), attachmentMapper.ent2dto((List<FileMetadata>) null));
    assertNull(attachmentMapper.ent2dto((FileMetadata) null));
  }
}
