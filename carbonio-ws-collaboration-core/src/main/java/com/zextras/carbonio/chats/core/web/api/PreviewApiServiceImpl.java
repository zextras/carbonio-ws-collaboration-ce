// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.PreviewApiService;
import com.zextras.carbonio.chats.core.data.model.FileResponse;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.PreviewService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.ImageQualityEnumDto;
import com.zextras.carbonio.chats.model.ImageShapeEnumDto;
import com.zextras.carbonio.chats.model.ImageTypeEnumDto;
import io.vavr.control.Option;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class PreviewApiServiceImpl implements PreviewApiService {

  private final PreviewService previewService;

  @Inject
  public PreviewApiServiceImpl(PreviewService previewService) {
    this.previewService = previewService;
  }

  @Override
  public Response getImagePreview(
      UUID fileId,
      String area,
      ImageQualityEnumDto quality,
      ImageTypeEnumDto outputFormat,
      Boolean crop,
      SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    FileResponse image =
        previewService.getImage(
            currentUser,
            fileId,
            area,
            Option.of(quality),
            Option.of(outputFormat),
            Option.of(crop));
    return Response.status(Response.Status.OK)
        .entity(image.getContent())
        .header("Content-Type", image.getMimeType())
        .header("Content-Length", image.getLength())
        .build();
  }

  @Override
  public Response getImageThumbnail(
      UUID fileId,
      String area,
      ImageQualityEnumDto quality,
      ImageTypeEnumDto outputFormat,
      ImageShapeEnumDto shape,
      SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    FileResponse image =
        previewService.getImageThumbnail(
            currentUser,
            fileId,
            area,
            Option.of(quality),
            Option.of(outputFormat),
            Option.of(shape));
    return Response.status(Response.Status.OK)
        .entity(image.getContent())
        .header("Content-Type", image.getMimeType())
        .header("Content-Length", image.getLength())
        .build();
  }

  @Override
  public Response getPdfPreview(
      UUID fileId, Integer firstPage, Integer lastPage, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    FileResponse image = previewService.getPDF(currentUser, fileId, firstPage, lastPage);
    return Response.status(Response.Status.OK)
        .entity(image.getContent())
        .header("Content-Type", image.getMimeType())
        .header("Content-Length", image.getLength())
        .build();
  }

  @Override
  public Response getPdfThumbnail(
      UUID fileId,
      String area,
      ImageQualityEnumDto quality,
      ImageTypeEnumDto outputFormat,
      ImageShapeEnumDto shape,
      SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    FileResponse image =
        previewService.getPDFThumbnail(
            currentUser,
            fileId,
            area,
            Option.of(quality),
            Option.of(outputFormat),
            Option.of(shape));
    return Response.status(Response.Status.OK)
        .entity(image.getContent())
        .header("Content-Type", image.getMimeType())
        .header("Content-Length", image.getLength())
        .build();
  }
}
