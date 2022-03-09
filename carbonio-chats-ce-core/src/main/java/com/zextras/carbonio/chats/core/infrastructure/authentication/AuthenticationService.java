// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication;

import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AuthenticationService {

  /**
   * Validates the user's credentials
   *
   * @param credentials a map of possible credentials related to the user
   * @return the user's identifier if the token is valid
   */
  Optional<String> validateToken(Map<AuthenticationMethod, String> credentials);

  /**
   * Gets the account by user identifier
   *
   * @param userId user identifier
   * @return the requested account {@link UserProfile}
   */
  Optional<UserProfile> getByUUID(UUID userId, UserPrincipal currentUser);

  /**
   * Returns whether we can communicate with the component or not
   *
   * @return a {@link Boolean} which indicates if we can communicate with the component or not
   */
  boolean isAlive();
}
