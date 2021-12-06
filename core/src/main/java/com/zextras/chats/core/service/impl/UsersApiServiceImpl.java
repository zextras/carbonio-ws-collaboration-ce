package com.zextras.chats.core.service.impl;

import com.zextras.chats.core.api.UsersApiService;
import com.zextras.chats.core.model.UserDto;
import java.util.UUID;
import javax.inject.Singleton;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class UsersApiServiceImpl implements UsersApiService {

  @Override
  public UserDto getUserById(
    UUID userId, SecurityContext securityContext
  ) {
    return null;
  }
}
