// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.model.AttachmentFilter;
import com.zextras.carbonio.chats.core.data.model.PaginationFilter;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.MimeTypeCategory;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import io.ebean.Database;
import io.ebean.Expr;
import io.ebean.Expression;
import io.ebean.ExpressionList;
import io.ebean.Query;
import io.ebean.annotation.Transactional;
import io.vavr.control.Option;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanFileMetadataRepository implements FileMetadataRepository {

  private final Database db;

  @Inject
  public EbeanFileMetadataRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<FileMetadata> getById(String fileId) {
    return db.find(FileMetadata.class).where().eq("id", fileId).findOneOrEmpty();
  }

  @Override
  public Optional<FileMetadata> find(String userId, String roomId, FileMetadataType type) {
    Query<FileMetadata> query = db.find(FileMetadata.class);
    Option.of(userId).map(p -> query.where().eq("userId", p));
    Option.of(roomId).map(p -> query.where().eq("roomId", p));
    Option.of(type).map(p -> query.where().eq("type", p));
    return query.findOneOrEmpty();
  }

  @Override
  public List<String> getIdsByRoomIdAndType(String roomId, FileMetadataType type) {
    return db.find(FileMetadata.class)
        .where()
        .eq("roomId", roomId)
        .and()
        .eq("type", type)
        .select("id")
        .findSingleAttributeList();
  }

  @Override
  @Transactional
  public List<FileMetadata> getByRoomIdAndType(
      String roomId,
      FileMetadataType type,
      int itemsNumber,
      @Nullable PaginationFilter paginationFilter,
      @Nullable AttachmentFilter attachmentFilter) {
    ExpressionList<FileMetadata> query = createRoomTypeQuery(roomId, type, attachmentFilter);

    boolean sortByCreatedAt = attachmentFilter == null || attachmentFilter.isSortByCreatedAt();
    boolean ascending = attachmentFilter != null && attachmentFilter.isAscending();
    String sortField = sortByCreatedAt ? "createdAt" : "originalSize";

    if (paginationFilter != null) {
      if (sortByCreatedAt) {
        applyCursorFilter(
            query, sortField, paginationFilter.getCreatedAt(), paginationFilter.getId(), ascending);
      } else {
        applyCursorFilter(
            query, sortField, paginationFilter.getSize(), paginationFilter.getId(), ascending);
      }
    }

    String sortOrder = ascending ? sortField + " asc, id asc" : sortField + " desc, id desc";
    return query.orderBy(sortOrder).setMaxRows(itemsNumber).findList();
  }

  @Override
  public long countByRoomIdAndType(
      String roomId, FileMetadataType type, @Nullable AttachmentFilter attachmentFilter) {
    return createRoomTypeQuery(roomId, type, attachmentFilter).findCount();
  }

  private ExpressionList<FileMetadata> createRoomTypeQuery(
      String roomId, FileMetadataType type, @Nullable AttachmentFilter attachmentFilter) {
    ExpressionList<FileMetadata> query =
        db.find(FileMetadata.class).where().eq("roomId", roomId).eq("type", type);

    if (attachmentFilter != null) {
      eqIfNotNull(query, "userId", attachmentFilter.getUserId());
      gtIfNotNull(query, "createdAt", attachmentFilter.getCreatedAfter());
      ltIfNotNull(query, "createdAt", attachmentFilter.getCreatedBefore());
      geIfNotNull(query, "originalSize", attachmentFilter.getMinSize());
      leIfNotNull(query, "originalSize", attachmentFilter.getMaxSize());

      String mimeType = attachmentFilter.getMimeType();
      if (mimeType != null) {
        if (mimeType.endsWith("/")) {
          query.startsWith("mimeType", mimeType);
        } else {
          query.eq("mimeType", mimeType);
        }
      }

      applyMimeTypeCategoryFilter(query, attachmentFilter.getMimeTypeCategory());
    }
    return query;
  }

  /**
   * Restricts the query to a {@link MimeTypeCategory}. A prefixed category ({@code IMAGES}, {@code
   * VIDEOS}) matches any MIME type starting with its prefix (image/, video/); {@code DOCUMENTS}
   * matches everything else (including audio), i.e. the negation of the disjunction of all prefixed
   * categories.
   */
  private static void applyMimeTypeCategoryFilter(
      ExpressionList<FileMetadata> query, @Nullable MimeTypeCategory mimeTypeCategory) {
    if (mimeTypeCategory == null) {
      return;
    }
    String mimeTypePrefix = mimeTypeCategory.mimeTypePrefix();
    if (mimeTypePrefix != null) {
      query.startsWith("mimeType", mimeTypePrefix);
      return;
    }
    Expression prefixedPredicate =
        MimeTypeCategory.prefixedCategoryPrefixes().stream()
            .map(prefix -> Expr.startsWith("mimeType", prefix))
            .reduce(Expr::or)
            .orElseThrow();
    query.add(Expr.not(prefixedPredicate));
  }

  private static <T> ExpressionList<FileMetadata> eqIfNotNull(
      ExpressionList<FileMetadata> q, String field, T value) {
    return value == null ? q : q.eq(field, value);
  }

  private static <T> ExpressionList<FileMetadata> gtIfNotNull(
      ExpressionList<FileMetadata> q, String field, T value) {
    return value == null ? q : q.gt(field, value);
  }

  private static <T> ExpressionList<FileMetadata> ltIfNotNull(
      ExpressionList<FileMetadata> q, String field, T value) {
    return value == null ? q : q.lt(field, value);
  }

  private static <T> ExpressionList<FileMetadata> geIfNotNull(
      ExpressionList<FileMetadata> q, String field, T value) {
    return value == null ? q : q.ge(field, value);
  }

  private static <T> ExpressionList<FileMetadata> leIfNotNull(
      ExpressionList<FileMetadata> q, String field, T value) {
    return value == null ? q : q.le(field, value);
  }

  private <T> void applyCursorFilter(
      ExpressionList<FileMetadata> query,
      String sortField,
      T cursorValue,
      String cursorId,
      boolean ascending) {
    if (ascending) {
      query
          .or()
          .gt(sortField, cursorValue)
          .and()
          .eq(sortField, cursorValue)
          .gt("id", cursorId)
          .endAnd();
    } else {
      query
          .or()
          .lt(sortField, cursorValue)
          .and()
          .eq(sortField, cursorValue)
          .lt("id", cursorId)
          .endAnd();
    }
  }

  @Override
  public List<String> getIdsByIdsAndRoomIdAndUserId(
      List<String> ids, String roomId, String userId) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return db.find(FileMetadata.class)
        .where()
        .idIn(ids)
        .and()
        .eq("roomId", roomId)
        .and()
        .eq("userId", userId)
        .select("id")
        .findSingleAttributeList();
  }

  @Override
  public FileMetadata save(FileMetadata metadata) {
    db.save(metadata);
    return metadata;
  }

  @Override
  public void delete(FileMetadata metadata) {
    db.delete(metadata);
  }

  @Override
  public void deleteByIds(List<String> ids) {
    if (!ids.isEmpty()) {
      db.deleteAll(FileMetadata.class, ids);
    }
  }
}
