// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.UsersApiService;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class UsersApiServiceImpl implements UsersApiService {

  private final int               MAX_USER_IDS = 10;
  private final UserService       userService;
  private final CapabilityService capabilityService;

  @Inject
  public UsersApiServiceImpl(
    UserService userService, CapabilityService capabilityService
  ) {
    this.userService = userService;
    this.capabilityService = capabilityService;
  }

  @Override
  @TimedCall
  public Response getUser(UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.OK)
      .entity(userService.getUserById(userId, currentUser))
      .build();
  }

  @Override
  public Response getUserPicture(UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    FileContentAndMetadata picture = userService.getUserPicture(userId, currentUser);
    return Response
      .status(Status.OK)
      .entity(picture.getFile())
      .header("Content-Type", picture.getMetadata().getMimeType())
      .header("Content-Length", picture.getMetadata().getOriginalSize())
      .header("Content-Disposition", String.format("inline; filename=\"%s\"", picture.getMetadata().getName()))
      .build();
  }

  @Override
  public Response getUsers(List<String> userIds, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);

    return (userIds.isEmpty() || userIds.size() > MAX_USER_IDS) ? Response.status(Status.BAD_REQUEST).build() : Response
      .status(Status.OK)
      .entity(userService.getUsersByIds(userIds, currentUser))
      .build();
  }

  @Override
  public Response updateUserPicture(
    UUID userId, String headerFileName, String headerMimeType, File body, SecurityContext securityContext
  ) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    userService.setUserPicture(
      userId,
      body,
      Optional.of(headerMimeType).orElseThrow(() -> new BadRequestException("Mime type not found")),
      Optional.of(headerFileName).map(StringFormatUtils::decodeFromUtf8)
        .orElseThrow(() -> new BadRequestException("File name not found")),
      currentUser
    );
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response deleteUserPicture(UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    userService.deleteUserPicture(userId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response getCapabilities(SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK).entity(capabilityService.getCapabilities(currentUser)).build();
  }

}
