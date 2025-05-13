// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.UserDto;
import java.util.List;
import java.util.UUID;

public interface UserService {

  /**
   * Retrieves info about a user
   *
   * @param userId the requested user's {@link UUID}
   * @param currentUser the currently authenticated {@link UserPrincipal}
   * @return the requested {@link UserDto}
   */
  UserDto getUserById(UUID userId, UserPrincipal currentUser);

  /**
   * Retrieves info about a list of user
   *
   * @param userIds list of the requested users' {@link UUID}
   * @param currentUser the currently authenticated {@link UserPrincipal}
   * @return the {@link List} of the requested {@link UserDto}
   */
  List<UserDto> getUsersByIds(List<String> userIds, UserPrincipal currentUser);

  /**
   * Checks if a user exists. Current implementations checks the {@link
   * com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService} to check if the user
   * exists.
   *
   * @param userId the user whose existence we need to check
   * @param currentUser the current authenticated user
   * @return a {@link Boolean} which indicates if the user exists or not
   */
  boolean userExists(UUID userId, UserPrincipal currentUser);
}
