// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.api.AttachmentsApiService;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class AttachmentsApiServiceImpl implements AttachmentsApiService {

  private final AttachmentService attachmentService;

  @Inject
  public AttachmentsApiServiceImpl(AttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  private static UserPrincipal getCurrentUser(SecurityContext securityContext) {
    return Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new);
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response getAttachment(UUID fileId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    FileContentAndMetadata attachment = attachmentService.getAttachmentById(fileId, currentUser);
    return Response.status(Status.OK)
        .entity(attachment)
        .header("Content-Type", attachment.getMetadata().getMimeType())
        .header("Content-Length", attachment.getMetadata().getOriginalSize())
        .header(
            "Content-Disposition",
            String.format("inline; filename=\"%s\"", attachment.getMetadata().getName()))
        .build();
  }

  @Override
  @TimedCall
  public Response getAttachmentInfo(UUID fileId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    return Response.ok()
        .entity(attachmentService.getAttachmentInfoById(fileId, currentUser))
        .build();
  }

  @Override
  @TimedCall
  public Response deleteAttachment(UUID fileId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    attachmentService.deleteAttachment(fileId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }
}
