// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.NotFoundException;
import com.zextras.carbonio.chats.api.PreviewApiService;
import com.zextras.carbonio.chats.core.data.model.FileResponse;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.PreviewService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.ImageQualityEnumDto;
import com.zextras.carbonio.chats.model.ImageShapeEnumDto;
import com.zextras.carbonio.chats.model.ImageTypeEnumDto;
import com.zextras.carbonio.preview.queries.enums.Format;
import com.zextras.carbonio.preview.queries.enums.Quality;
import com.zextras.carbonio.preview.queries.enums.Shape;
import io.vavr.control.Option;
import org.apache.commons.lang3.ObjectUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class PreviewApiServiceImpl implements PreviewApiService {

  private final PreviewService previewService;


  @Inject
  public PreviewApiServiceImpl(PreviewService previewService)
  {
    this.previewService = previewService;
  }

  @Override
  public Response getImagePreview(UUID fileId, String area, ImageQualityEnumDto quality, ImageTypeEnumDto outputFormat, Boolean crop, SecurityContext securityContext) throws NotFoundException {
    if(ObjectUtils.anyNull(fileId,area)){
      return Response
        .status(Response.Status.BAD_REQUEST)
        .build();
    } else {
      UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new);
      FileResponse image = previewService.getImage(currentUser,
        fileId,
        area,
        Option.of(quality).map(q -> Quality.valueOf(q.toString())),
        Option.of(outputFormat).map(f -> Format.valueOf(f.toString())),
        Option.of(crop));
      return Response
        .status(Response.Status.OK)
        .entity(image.getContent())
        .header("Content-Type", image.getMimeType())
        .header("Content-Length", image.getLength())
        .build();
    }
  }

  @Override
  public Response getImageThumbnail(UUID fileId, String area, ImageQualityEnumDto quality, ImageTypeEnumDto outputFormat, ImageShapeEnumDto shape, SecurityContext securityContext) throws NotFoundException {
    if(ObjectUtils.anyNull(fileId,area)){
      return Response
        .status(Response.Status.BAD_REQUEST)
        .build();
    } else {
      UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new);
      FileResponse image = previewService.getImageThumbnail(currentUser,
        fileId,
        area,
        Option.of(quality).map(q -> Quality.valueOf(q.toString())),
        Option.of(outputFormat).map(f -> Format.valueOf(f.toString())),
        Option.of(shape).map(s -> Shape.valueOf(s.toString()))
      );
      return Response
        .status(Response.Status.OK)
        .entity(image.getContent())
        .header("Content-Type", image.getMimeType())
        .header("Content-Length", image.getLength())
        .build();
    }
  }

  @Override
  public Response getPdfPreview(UUID fileId, Integer firstPage, Integer lastPage, SecurityContext securityContext) throws NotFoundException {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    if(ObjectUtils.anyNull(fileId,firstPage,lastPage)){
      return Response
        .status(Response.Status.BAD_REQUEST)
        .build();
    } else {
      FileResponse image = previewService.getPDF(
        currentUser,
        fileId,
        firstPage,
        lastPage);
      return Response
        .status(Response.Status.OK)
        .entity(image.getContent())
        .header("Content-Type", image.getMimeType())
        .header("Content-Length", image.getLength())
        .build();
    }
  }

  @Override
  public Response getPdfThumbnail(UUID fileId, String area, ImageQualityEnumDto quality, ImageTypeEnumDto outputFormat, ImageShapeEnumDto shape, SecurityContext securityContext) throws NotFoundException {
    if(ObjectUtils.anyNull(fileId,area)){
      return Response
        .status(Response.Status.BAD_REQUEST)
        .build();
    } else {
      UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new);
      FileResponse image = previewService.getPDFThumbnail(
        currentUser,
        fileId,
        area,
        Option.of(quality).map(q -> Quality.valueOf(q.toString())),
        Option.of(outputFormat).map(f -> Format.valueOf(f.toString())),
        Option.of(shape).map(s -> Shape.valueOf(s.toString()))
      );
      return Response
        .status(Response.Status.OK)
        .entity(image.getContent())
        .header("Content-Type", image.getMimeType())
        .header("Content-Length", image.getLength())
        .build();
    }
  }
}
