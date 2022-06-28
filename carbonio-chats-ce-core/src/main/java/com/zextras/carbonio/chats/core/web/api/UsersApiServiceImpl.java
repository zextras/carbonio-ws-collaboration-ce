// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;


import com.zextras.carbonio.chats.api.UsersApiService;
import com.zextras.carbonio.chats.core.annotation.TimedCall;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
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
}
