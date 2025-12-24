// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.mongoosent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessageAttachment;
import io.ebean.Database;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Singleton
public class MNTMessageAttachmentRepository {

  private final Database db;

  @Inject
  public MNTMessageAttachmentRepository(Database db) {
    this.db = db;
  }

  public void insert(MNTMessageAttachment attachment) {
    db.insert(attachment);
  }

  public void update(MNTMessageAttachment attachment) {
    db.update(attachment);
  }

  public Optional<MNTMessageAttachment> getById(String id) {
    return Optional.ofNullable(db.find(MNTMessageAttachment.class, id));
  }

  public List<MNTMessageAttachment> getByMessageId(String messageId) {
    return db.find(MNTMessageAttachment.class)
        .where()
        .eq("messageId", messageId)
        .eq("deleted", false)
        .orderBy()
        .asc("createdAt")
        .findList();
  }

  public List<MNTMessageAttachment> getByMessageIdIncludeDeleted(String messageId) {
    return db.find(MNTMessageAttachment.class)
        .where()
        .eq("messageId", messageId)
        .orderBy()
        .asc("createdAt")
        .findList();
  }

  public List<String> getActiveAttachmentIdsByMessageId(String messageId) {
    return db.find(MNTMessageAttachment.class)
        .select("id")
        .where()
        .eq("messageId", messageId)
        .eq("deleted", false)
        .findSingleAttributeList();
  }

  /**
   * Gets a pending (not yet linked) attachment by ID and userId. Returns empty if attachment
   * doesn't exist, is already linked, belongs to different user, or is deleted.
   */
  public Optional<MNTMessageAttachment> getPendingByIdAndUserId(String id, String userId) {
    return db.find(MNTMessageAttachment.class)
        .where()
        .eq("id", id)
        .eq("userId", userId)
        .isNull("messageId")
        .eq("deleted", false)
        .findOneOrEmpty();
  }

  /**
   * Links a pending attachment to a message. Returns number of rows updated (0 or 1). Only links if
   * attachment exists, belongs to userId, is not yet linked, and is not deleted.
   */
  public int linkToMessage(String attachmentId, String messageId, String userId) {
    return db.find(MNTMessageAttachment.class)
        .where()
        .eq("id", attachmentId)
        .eq("userId", userId)
        .isNull("messageId")
        .eq("deleted", false)
        .asUpdate()
        .set("messageId", messageId)
        .update();
  }

  /**
   * Finds orphan attachments: pending (not linked to message), older than threshold, not deleted.
   * Uses partial index MONGOOSENT_ATTACH_ORPHAN_IDX for efficient lookup.
   */
  public List<MNTMessageAttachment> findOrphans(OffsetDateTime olderThan) {
    return db.find(MNTMessageAttachment.class)
        .where()
        .isNull("messageId")
        .eq("deleted", false)
        .lt("createdAt", olderThan)
        .findList();
  }

  /**
   * Hard deletes orphan attachments older than threshold. Returns deleted records for blob cleanup.
   */
  public List<MNTMessageAttachment> deleteOrphans(OffsetDateTime olderThan) {
    List<MNTMessageAttachment> orphans = findOrphans(olderThan);
    if (!orphans.isEmpty()) {
      db.deleteAll(orphans);
    }
    return orphans;
  }

  /**
   * Soft delete: marks as deleted but keeps metadata for audit trail. The blob should be deleted
   * from storages separately.
   */
  public void markAsDeleted(String id) {
    db.find(MNTMessageAttachment.class)
        .where()
        .eq("id", id)
        .asUpdate()
        .set("deleted", true)
        .update();
  }

  /**
   * Soft delete all attachments for a message.
   */
  public void markAllAsDeletedByMessageId(String messageId) {
    db.find(MNTMessageAttachment.class)
        .where()
        .eq("messageId", messageId)
        .eq("deleted", false)
        .asUpdate()
        .set("deleted", true)
        .update();
  }
}
