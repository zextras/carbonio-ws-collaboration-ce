// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

  /**
   * Returns the user associated with the given id.
   *
   * @param id the user id to retrieve
   * @return an {@link Optional} which contains the {@link User}, or empty if it was not found
   */
  Optional<User> getById(String id);

  /**
   * Returns the user associated with the given id.
   *
   * @param ids the {@link List} of the users id to retrieve
   * @return a {@link List} which contains the {@link User}s, or empty if they are not found
   */
  List<User> getByIds(List<String> ids);

  /**
   * Inserts the given user
   *
   * @param user the {@link User} to insert
   * @return the inserted {@link User}
   */
  User save(User user);

}
