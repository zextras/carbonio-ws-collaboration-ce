package com.zextras.carbonio.chats.core.web.api;


import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.web.security.MockSecurityContext;
import com.zextras.carbonio.chats.core.web.security.MockUserPrincipal;
import com.zextras.carbonio.chats.api.AttachmentsApiService;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class AttachmentsApiServiceImpl implements AttachmentsApiService {

  private final AttachmentService   attachmentService;
  private final MockSecurityContext mockSecurityContext;

  @Inject
  public AttachmentsApiServiceImpl(AttachmentService attachmentService, MockSecurityContext mockSecurityContext) {
    this.attachmentService = attachmentService;
    this.mockSecurityContext = mockSecurityContext;
  }

  @Override
  public Response getAttachment(UUID fileId, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
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
  public Response getAttachmentPreview(UUID fileId, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    FileContentAndMetadata attachment = attachmentService.getAttachmentPreviewById(fileId, currentUser);
    return Response
      .status(Status.OK)
      .entity(attachment.getFile())
      .header("Content-Type", attachment.getMetadata().getMimeType())
      .header("Content-Length", attachment.getMetadata().getOriginalSize())
      .header("Content-Disposition", String.format("inline; filename=\"%s\"", attachment.getMetadata().getName()))
      .build();
  }

  @Override
  public Response getAttachmentInfo(UUID fileId, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    return Response.ok().entity(attachmentService.getAttachmentInfoById(fileId, currentUser)).build();
  }

  @Override
  public Response deleteAttachment(UUID fileId, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    attachmentService.deleteAttachment(fileId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }
}
