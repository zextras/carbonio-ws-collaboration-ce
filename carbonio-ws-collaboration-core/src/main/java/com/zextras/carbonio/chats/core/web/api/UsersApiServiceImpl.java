// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.api.UsersApiService;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
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

  private static UserPrincipal getCurrentUser(SecurityContext securityContext) {
    return Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new);
  }

  @Override
  @TimedCall
  public Response getUser(UUID userId, SecurityContext securityContext) {
    return Response.status(Status.OK)
        .entity(userService.getUserById(userId, getCurrentUser(securityContext)))
        .build();
  }

  @Override
  public Response getUsers(List<UUID> userIds, SecurityContext securityContext) {
    return (userIds.isEmpty() || userIds.size() > 10)
        ? Response.status(Status.BAD_REQUEST).build()
        : Response.status(Status.OK)
            .entity(userService.getUsersByIds(userIds, getCurrentUser(securityContext)))
            .build();
  }

  @Override
  public Response getCapabilities(SecurityContext securityContext) {
    return Response.status(Status.OK)
        .entity(capabilityService.getCapabilities(getCurrentUser(securityContext)))
        .build();
  }
}
