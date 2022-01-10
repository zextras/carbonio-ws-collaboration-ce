package com.zextras.chats.core.service.impl;

import com.zextras.chats.core.model.UserDto;
import com.zextras.chats.core.service.UserService;
import com.zextras.chats.core.web.security.MockUserPrincipal;
import java.util.UUID;
import javax.inject.Singleton;

@Singleton
public class UserServiceImpl implements UserService {

  @Override
  public UserDto getUserById(UUID userId, MockUserPrincipal currentUser) {
    return null;
  }
}
