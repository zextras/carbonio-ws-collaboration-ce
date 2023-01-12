// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.authentication;

import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import java.util.Map;
import java.util.Optional;

public interface AuthenticationService extends HealthIndicator {

  /**
   * Validates the user's credentials
   *
   * @param credentials a map of possible credentials related to the user
   * @return the user's identifier if the token is valid
   */
  Optional<String> validateCredentials(Map<AuthenticationMethod, String> credentials);

}