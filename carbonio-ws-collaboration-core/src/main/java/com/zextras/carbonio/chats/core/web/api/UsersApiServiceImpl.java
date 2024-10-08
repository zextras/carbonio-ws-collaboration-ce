// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.api.UsersApiService;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class UsersApiServiceImpl implements UsersApiService {

  private final UserService userService;
  private final CapabilityService capabilityService;

  @Inject
  public UsersApiServiceImpl(UserService userService, CapabilityService capabilityService) {
    this.userService = userService;
    this.capabilityService = capabilityService;
  }

  @Override
  @TimedCall
  public Response getUser(UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK).entity(userService.getUserById(userId, currentUser)).build();
  }

  @Override
  public Response getUserPicture(UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    FileContentAndMetadata picture = userService.getUserPicture(userId, currentUser);
    return Response.status(Status.OK)
        .entity(picture)
        .header("Content-Type", picture.getMetadata().getMimeType())
        .header("Content-Length", picture.getMetadata().getOriginalSize())
        .header(
            "Content-Disposition",
            String.format("inline; filename=\"%s\"", picture.getMetadata().getName()))
        .build();
  }

  @Override
  public Response getUsers(List<String> userIds, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);

    return (userIds.isEmpty() || userIds.size() > 10)
        ? Response.status(Status.BAD_REQUEST).build()
        : Response.status(Status.OK)
            .entity(userService.getUsersByIds(userIds, currentUser))
            .build();
  }

  @Override
  public Response updateUserPicture(
      UUID userId,
      String headerFileName,
      String headerMimeType,
      Long contentLength,
      InputStream body,
      SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    String fileName;
    try {
      fileName =
          StringFormatUtils.decodeFromUtf8(
              Optional.of(headerFileName)
                  .orElseThrow(() -> new BadRequestException("File name not found")));
    } catch (UnsupportedEncodingException e) {
      throw new BadRequestException("Unable to decode the file name", e);
    }
    userService.setUserPicture(
        userId,
        body,
        Optional.of(headerMimeType)
            .orElseThrow(() -> new BadRequestException("Mime type not found")),
        contentLength,
        fileName,
        currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response deleteUserPicture(UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    userService.deleteUserPicture(userId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response getCapabilities(SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
        .entity(capabilityService.getCapabilities(currentUser))
        .build();
  }
}
