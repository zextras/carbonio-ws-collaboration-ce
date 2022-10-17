package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.AuthApiService;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.TokenDto;
import java.util.Optional;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class AuthApiServiceImpl implements AuthApiService {

  @Override
  public Response getTokens(SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.OK)
      .entity(TokenDto.create().zmToken(
        currentUser.getAuthCredentialFor(AuthenticationMethod.ZM_AUTH_TOKEN).orElseThrow(NotFoundException::new)))
      .build();
  }
}
