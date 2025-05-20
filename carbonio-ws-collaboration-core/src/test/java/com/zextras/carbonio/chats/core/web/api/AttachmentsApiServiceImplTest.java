// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.api.AttachmentsApiService;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import jakarta.ws.rs.core.SecurityContext;
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

  @BeforeEach
  void init() {
    this.securityContext = mock(SecurityContext.class);
    this.attachmentService = mock(AttachmentService.class);
    this.attachmentsApiService = new AttachmentsApiServiceImpl(attachmentService);

    guestId = UUID.randomUUID();
    guest = UserPrincipal.create(guestId).userType(UserType.GUEST);
  }

  @Test
  void guestCannotDelete() {
    when(securityContext.getUserPrincipal()).thenReturn(guest);
    var fileId = UUID.randomUUID();

    assertThrows(
        ForbiddenException.class,
        () -> attachmentsApiService.deleteAttachment(fileId, securityContext));
  }
}
