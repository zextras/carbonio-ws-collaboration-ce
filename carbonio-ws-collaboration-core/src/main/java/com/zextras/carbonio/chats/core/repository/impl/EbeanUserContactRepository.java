// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.UserContact;
import com.zextras.carbonio.chats.core.data.entity.UserContactId;
import com.zextras.carbonio.chats.core.repository.UserContactRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanUserContactRepository implements UserContactRepository {

  private final Database db;

  @Inject
  public EbeanUserContactRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<UserContact> get(String userId, String contactId) {
    UserContactId id = new UserContactId(userId, contactId);
    return Optional.ofNullable(db.find(UserContact.class, id));
  }

  @Override
  public List<UserContact> getByUserId(String userId) {
    return db.find(UserContact.class).where().eq("id.userId", userId).findList();
  }

  @Override
  public List<UserContact> getByContactId(String contactId) {
    return db.find(UserContact.class).where().eq("id.contactId", contactId).findList();
  }

  @Override
  public UserContact insert(UserContact contact) {
    db.insert(contact);
    return contact;
  }

  @Override
  @Transactional
  public void addMutualContact(String user1Id, String user2Id) {
    // Check if relationships already exist
    if (get(user1Id, user2Id).isEmpty()) {
      insert(UserContact.create(user1Id, user2Id));
    }
    if (get(user2Id, user1Id).isEmpty()) {
      insert(UserContact.create(user2Id, user1Id));
    }
  }

  @Override
  public void delete(String userId, String contactId) {
    UserContactId id = new UserContactId(userId, contactId);
    db.delete(UserContact.class, id);
  }

  @Override
  @Transactional
  public void deleteMutualContact(String user1Id, String user2Id) {
    delete(user1Id, user2Id);
    delete(user2Id, user1Id);
  }

  @Override
  public boolean areContacts(String userId, String contactId) {
    return get(userId, contactId).isPresent();
  }
}
