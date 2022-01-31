// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.model.UserDto;
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
