// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;


import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.UserDto;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Generated;

public interface UserService {

  /**
   * Retrieves info about a user
   *
   * @param userId      the requested user's {@link UUID}
   * @param currentUser the currently authenticated {@link UserPrincipal}
   * @return an {@link Optional} containing the requested {@link UserDto} or empty if it was not found
   **/
  Optional<UserDto> getUserById(UUID userId, UserPrincipal currentUser);

}
