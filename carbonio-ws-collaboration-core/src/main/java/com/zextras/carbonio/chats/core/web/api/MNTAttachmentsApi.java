// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.model.message.MNTAttachmentDto;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.mongoosent.MNTChatService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.util.Optional;

/**
 * REST API for file attachments - handles file upload/download which is not suitable for
 * WebSocket.
 */
@Path("/mnt-attachments")
@Singleton
public class MNTAttachmentsApi {

  private final MNTChatService chatService;

  @Inject
  public MNTAttachmentsApi(MNTChatService chatService) {
    this.chatService = chatService;
  }

  /**
   * Upload a pending attachment (not yet linked to a message). Returns the attachment ID which
   * should then be passed in the SEND_MESSAGE WebSocket action to link it to a message.
   *
   * <p>Pending attachments not linked to a message within 1 hour will be automatically cleaned up.
   *
   * <p>File metadata should be passed as query parameters: fileName, mimeType, fileSize. The
   * request body should be the raw file bytes (application/octet-stream).
   *
   * @param file The raw file stream
   * @param fileName Original file name (query param)
   * @param mimeType MIME type (query param, optional)
   * @param fileSize File size in bytes (query param)
   * @param securityContext Security context for authentication
   * @return The created attachment DTO with id (messageId will be null)
   */
  @POST
  @Path("/upload")
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.APPLICATION_JSON)
  public Response uploadPendingAttachment(
      InputStream file,
      @QueryParam("fileName") String fileName,
      @QueryParam("mimeType") String mimeType,
      @QueryParam("fileSize") Long fileSize,
      @Context SecurityContext securityContext) {

    try {
      UserPrincipal currentUser =
          Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
              .orElseThrow(UnauthorizedException::new);
      String userId = currentUser.getUUID().toString();

      if (fileName == null || fileName.isEmpty()) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"error\": \"fileName query parameter is required\"}")
            .build();
      }

      if (fileSize == null || fileSize <= 0) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"error\": \"fileSize query parameter is required\"}")
            .build();
      }

      String effectiveMimeType = mimeType != null ? mimeType : determineMimeType(fileName);

      MNTAttachmentDto attachment =
          chatService.uploadPendingAttachment(
              userId, file, fileName, effectiveMimeType, fileSize);

      return Response.ok(attachment).build();

    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"error\": \"" + e.getMessage() + "\"}")
          .build();
    }
  }

  /**
   * Download an attachment file.
   */
  @GET
  @Path("/download/{attachmentId}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadAttachment(
      @PathParam("attachmentId") String attachmentId,
      @Context SecurityContext securityContext) {

    try {
      UserPrincipal currentUser =
          Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
              .orElseThrow(UnauthorizedException::new);
      String userId = currentUser.getUUID().toString();

      MNTAttachmentDto attachment = chatService.getAttachment(attachmentId, userId);
      InputStream fileStream = chatService.getAttachmentStream(attachmentId, userId);

      StreamingOutput output = outputStream -> {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = fileStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }
        fileStream.close();
      };

      return Response.ok(output)
          .header("Content-Disposition", "attachment; filename=\"" + attachment.getFileName() + "\"")
          .header("Content-Type", attachment.getMimeType())
          .header("Content-Length", attachment.getFileSize())
          .build();

    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("{\"error\": \"" + e.getMessage() + "\"}")
          .build();
    }
  }

  private String determineMimeType(String fileName) {
    if (fileName == null) return "application/octet-stream";
    String lower = fileName.toLowerCase();
    if (lower.endsWith(".pdf")) return "application/pdf";
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".gif")) return "image/gif";
    if (lower.endsWith(".txt")) return "text/plain";
    if (lower.endsWith(".html")) return "text/html";
    if (lower.endsWith(".json")) return "application/json";
    if (lower.endsWith(".xml")) return "application/xml";
    if (lower.endsWith(".zip")) return "application/zip";
    if (lower.endsWith(".doc")) return "application/msword";
    if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
    if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    if (lower.endsWith(".mp3")) return "audio/mpeg";
    if (lower.endsWith(".mp4")) return "video/mp4";
    if (lower.endsWith(".wav")) return "audio/wav";
    return "application/octet-stream";
  }
}
