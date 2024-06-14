// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.AuthApiService;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.TokenDto;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Optional;

@Singleton
public class AuthApiServiceImpl implements AuthApiService {

  @Override
  public Response getTokens(SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
        .entity(
            TokenDto.create()
                .zmToken(currentUser.getAuthToken().orElseThrow(NotFoundException::new)))
        .build();
  }
}
