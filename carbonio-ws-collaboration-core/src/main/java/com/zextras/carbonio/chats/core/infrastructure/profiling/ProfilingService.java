// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.profiling;

import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.UserDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfilingService extends HealthIndicator {

  /**
   * Returns the user identified by the given id.
   *
   * @param principal the authenticated user that's performing the request
   * @param userId    id of the user to retrieve
   * @return an {@link Optional} which contains {@link UserDto} if it was found, or empty otherwise
   */
  Optional<UserProfile> getById(UserPrincipal principal, UUID userId);

  /**
   * Returns the {@link List} of users identified by the given ids.
   *
   * @param principal the authenticated user that's performing the request
   * @param userIds   {@link List} of ids of the users to retrieve
   * @return a {@link List} which contains the of {@link UserDto} if they are found, or empty
   * otherwise
   */
  List<UserProfile> getByIds(UserPrincipal principal, List<String> userIds);

}
