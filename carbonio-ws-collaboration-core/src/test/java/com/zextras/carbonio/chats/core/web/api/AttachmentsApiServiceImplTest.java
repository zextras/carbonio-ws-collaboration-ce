// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.api.AttachmentsApiService;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadataBuilder;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.web.api.mapper.impl.AttachmentMapperImpl;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentDto;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class AttachmentsApiServiceImplTest {

  private AttachmentsApiService attachmentsApiService;
  private SecurityContext securityContext;
  private AttachmentService attachmentService;
  private UUID guestId;
  private UserPrincipal guest;
  private UUID userId;
  private UserPrincipal authenticatedUser;

  @BeforeEach
  void init() {
    this.securityContext = mock(SecurityContext.class);
    this.attachmentService = mock(AttachmentService.class);
    this.attachmentsApiService =
        new AttachmentsApiServiceImpl(attachmentService, new AttachmentMapperImpl());

    guestId = UUID.randomUUID();
    guest = UserPrincipal.create(guestId).userType(UserType.GUEST);
    userId = UUID.randomUUID();
    authenticatedUser = UserPrincipal.create(userId);
  }

  @Test
  void guestCannotDelete() {
    when(securityContext.getUserPrincipal()).thenReturn(guest);
    var fileId = UUID.randomUUID();

    assertThrows(
        ForbiddenException.class,
        () -> attachmentsApiService.deleteAttachment(fileId, securityContext));
  }

  @Test
  void getAttachmentInfo_mapsTheMetadata() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(authenticatedUser);
    UUID fileId = UUID.randomUUID();
    UUID roomId = UUID.randomUUID();
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
    when(attachmentService.getAttachmentInfoById(fileId, authenticatedUser))
        .thenReturn(
            FileMetadataBuilder.create()
                .id(fileId.toString())
                .name("test.pdf")
                .originalSize(2048L)
                .mimeType("application/pdf")
                .type(FileMetadataType.ATTACHMENT)
                .userId(userId.toString())
                .roomId(roomId.toString())
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build());

    Response response = attachmentsApiService.getAttachmentInfo(fileId, securityContext);

    verify(attachmentService, times(1)).getAttachmentInfoById(fileId, authenticatedUser);
    AttachmentDto dto = (AttachmentDto) response.getEntity();
    assertEquals(fileId, dto.getId());
    assertEquals("test.pdf", dto.getName());
    assertEquals(2048L, dto.getSize());
    assertEquals("application/pdf", dto.getMimeType());
    assertEquals(userId, dto.getUserId());
    assertEquals(roomId, dto.getRoomId());
    assertEquals(createdAt, dto.getCreatedAt());
  }
}
