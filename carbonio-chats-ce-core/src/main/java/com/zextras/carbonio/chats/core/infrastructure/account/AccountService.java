// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.account;

import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import java.util.Optional;
import java.util.UUID;

public interface AccountService {

  /**
   * Validates the token
   *
   * @param token token to validate
   * @return the user's identifier if the token is valid
   */
  Optional<String> validateToken(String token);

  /**
   * Gets the account by user identifier
   *
   * @param userId user identifier
   * @return the requested account {@link Account}
   */
  Optional<Account> getByUUID(UUID userId, UserPrincipal currentUser);

  /**
   * Returns whether we can communicate with the component or not
   *
   * @return a {@link Boolean} which indicates if we can communicate with the component or not
   */
  boolean isAlive();
}
