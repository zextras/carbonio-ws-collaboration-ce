package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.model.UserDto;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.MockUserPrincipal;
import java.util.UUID;
import javax.inject.Singleton;

@Singleton
public class UserServiceImpl implements UserService {

  @Override
  public UserDto getUserById(UUID userId, MockUserPrincipal currentUser) {
    return null;
  }
}
