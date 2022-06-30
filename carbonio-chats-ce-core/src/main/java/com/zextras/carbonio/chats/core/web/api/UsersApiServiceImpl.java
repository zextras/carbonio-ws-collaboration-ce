// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;


import com.zextras.carbonio.chats.api.UsersApiService;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.utils.Utils;
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
public class UsersApiServiceImpl implements UsersApiService {

  private final UserService userService;

  @Inject
  public UsersApiServiceImpl(UserService userService) {
    this.userService = userService;
  }

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
    FileContentAndMetadata roomPicture = userService.getUserPicture(userId, currentUser);
    return Response
      .status(Status.OK)
      .entity(roomPicture.getFile())
      .header("Content-Type", roomPicture.getMetadata().getMimeType())
      .header("Content-Length", roomPicture.getMetadata().getOriginalSize())
      .header("Content-Disposition", String.format("inline; filename=\"%s\"", roomPicture.getMetadata().getName()))
      .build();
  }

  @Override
  public Response updateUserPicture(String xContentDisposition, File body, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    userService.setUserPicture(currentUser.getUUID(), body,
      Utils.getFilePropertyFromContentDisposition(xContentDisposition, "mimeType")
        .orElseThrow(() -> new BadRequestException("Mime type not found in X-Content-Disposition header")),
      Utils.getFilePropertyFromContentDisposition(xContentDisposition, "fileName")
        .orElseThrow(() -> new BadRequestException("File name not found in X-Content-Disposition header")),
      currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

}
