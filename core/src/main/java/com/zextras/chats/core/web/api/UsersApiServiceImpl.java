package com.zextras.chats.core.web.api;


import com.zextras.chats.core.api.UsersApiService;
import com.zextras.chats.core.exception.UnauthorizedException;
import com.zextras.chats.core.service.UserService;
import com.zextras.chats.core.web.security.MockSecurityContext;
import com.zextras.chats.core.web.security.MockUserPrincipal;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class UsersApiServiceImpl implements UsersApiService {

  private final UserService         userService;
  private final MockSecurityContext mockSecurityContext;

  @Inject
  public UsersApiServiceImpl(UserService userService, MockSecurityContext mockSecurityContext) {
    this.userService = userService;
    this.mockSecurityContext = mockSecurityContext;
  }

  public Response getUserById(UUID userId, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.OK)
      .entity(userService.getUserById(userId, currentUser))
      .build();
  }
}
