// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication;

import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import com.zextras.carbonio.usermanagement.entities.UserMyself;
import io.vavr.control.Try;
import java.util.Optional;

public interface AuthenticationService extends HealthIndicator {

  /**
   * Validates the user's credentials
   *
   * @param authToken the token needed to be authenticated
   * @return the user's identifier if the token is valid
   */
  Optional<String> validateCredentials(String authToken);

  /**
   * Validates the user's credentials
   *
   * @param authToken the token needed to be authenticated
   * @return the user's info if the token is valid
   */
  Try<UserMyself> getUserMySelf(String authToken);
}
