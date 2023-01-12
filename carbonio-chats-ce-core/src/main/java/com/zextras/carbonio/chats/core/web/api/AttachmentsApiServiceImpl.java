// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.AttachmentsApiService;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.exception.PreviewerException;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.previewer.PreviewerService;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import java.io.File;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class AttachmentsApiServiceImpl implements AttachmentsApiService {

  private final PreviewerService  previewService;
  private final StoragesService   storagesService;
  private final AttachmentService attachmentService;

  @Inject
  public AttachmentsApiServiceImpl(AttachmentService attachmentService, PreviewerService previewService,
    StoragesService storagesService) {
    this.attachmentService = attachmentService;
    this.previewService = previewService;
    this.storagesService = storagesService;
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response getAttachment(UUID fileId, SecurityContext securityContext) {
    if (!storagesService.isAlive()) {
      throw new StorageException("Storage service is not alive");
    }
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    FileContentAndMetadata attachment = attachmentService.getAttachmentById(fileId, currentUser);
    return Response
      .status(Status.OK)
      .entity(attachment.getFile())
      .header("Content-Type", attachment.getMetadata().getMimeType())
      .header("Content-Length", attachment.getMetadata().getOriginalSize())
      .header("Content-Disposition", String.format("inline; filename=\"%s\"", attachment.getMetadata().getName()))
      .build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response getAttachmentPreview(UUID fileId, SecurityContext securityContext) {
    if (!previewService.isAlive()) {
      throw new PreviewerException("Previewer service is not alive");
    }
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    File attachment = attachmentService.getAttachmentPreviewById(fileId, currentUser);
    return Response
      .status(Status.OK)
      .entity(attachment)
      .header("Content-Type", "image/jpeg")
      .header("Content-Length", attachment.length())
      .build();
  }

  @Override
  @TimedCall
  public Response getAttachmentInfo(UUID fileId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.ok().entity(attachmentService.getAttachmentInfoById(fileId, currentUser)).build();
  }

  @Override
  @TimedCall
  public Response deleteAttachment(UUID fileId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    attachmentService.deleteAttachment(fileId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }
}
