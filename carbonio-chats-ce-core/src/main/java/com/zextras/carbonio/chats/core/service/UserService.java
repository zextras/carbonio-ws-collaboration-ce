// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;


import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.UserDto;
import java.util.UUID;
import javax.annotation.Generated;

@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public interface UserService {

  /**
   * Retrieves a user
   *
   * @param userId      user identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return Requested user {@link UserDto }
   **/
  UserDto getUserById(UUID userId, UserPrincipal currentUser);

}
