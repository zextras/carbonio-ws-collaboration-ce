// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.UserContact;
import java.util.List;
import java.util.Optional;

public interface UserContactRepository {

  /**
   * Gets a specific contact relationship
   *
   * @param userId user identifier
   * @param contactId contact identifier
   * @return The {@link UserContact} wrapped in an {@link Optional}
   */
  Optional<UserContact> get(String userId, String contactId);

  /**
   * Gets all contacts (roster) for a user
   *
   * @param userId user identifier
   * @return {@link List} of {@link UserContact}
   */
  List<UserContact> getByUserId(String userId);

  /**
   * Gets all users who have this contact in their roster (for presence broadcast when contact goes
   * online/offline)
   *
   * @param contactId contact identifier
   * @return {@link List} of {@link UserContact}
   */
  List<UserContact> getByContactId(String contactId);

  /**
   * Adds a contact relationship
   *
   * @param contact {@link UserContact} to add
   * @return {@link UserContact} added
   */
  UserContact insert(UserContact contact);

  /**
   * Adds mutual contact relationship (both directions)
   *
   * @param user1Id first user identifier
   * @param user2Id second user identifier
   */
  void addMutualContact(String user1Id, String user2Id);

  /**
   * Removes a contact relationship
   *
   * @param userId user identifier
   * @param contactId contact identifier
   */
  void delete(String userId, String contactId);

  /**
   * Removes mutual contact relationship (both directions)
   *
   * @param user1Id first user identifier
   * @param user2Id second user identifier
   */
  void deleteMutualContact(String user1Id, String user2Id);

  /**
   * Checks if two users are contacts
   *
   * @param userId user identifier
   * @param contactId contact identifier
   * @return true if they are contacts
   */
  boolean areContacts(String userId, String contactId);
}
