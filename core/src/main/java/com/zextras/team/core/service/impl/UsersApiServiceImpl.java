package com.zextras.team.core.service.impl;

import com.zextras.team.core.api.UsersApiService;
import com.zextras.team.core.model.UserDto;
import java.util.UUID;
import javax.ws.rs.core.SecurityContext;

public class UsersApiServiceImpl implements UsersApiService {

  @Override
  public UserDto getUserById(
    UUID userId, SecurityContext securityContext
  ) {
    return null;
  }
}
