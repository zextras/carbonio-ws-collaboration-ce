// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.model.AttachmentFilter;
import com.zextras.carbonio.chats.core.data.model.PaginationFilter;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.MimeTypeCategory;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Hibernate port of CE's Ebean repository. Every method runs in its own transaction unless the
 * caller already has one, mirroring Ebean's implicit per-statement transactions.
 */
@ApplicationScoped
@Transactional
public class JpaFileMetadataRepository implements FileMetadataRepository {

  private final EntityManager em;

  @Inject
  public JpaFileMetadataRepository(EntityManager em) {
    this.em = em;
  }

  @Override
  public Optional<FileMetadata> getById(String fileId) {
    return Optional.ofNullable(em.find(FileMetadata.class, fileId));
  }

  @Override
  public Optional<FileMetadata> find(String userId, String roomId, FileMetadataType type) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<FileMetadata> q = cb.createQuery(FileMetadata.class);
    Root<FileMetadata> r = q.from(FileMetadata.class);
    List<Predicate> where = new ArrayList<>();
    if (userId != null) where.add(cb.equal(r.get("userId"), userId));
    if (roomId != null) where.add(cb.equal(r.get("roomId"), roomId));
    if (type != null) where.add(cb.equal(r.get("type"), type));
    q.select(r).where(where.toArray(Predicate[]::new));
    return em.createQuery(q).setMaxResults(1).getResultStream().findFirst();
  }

  @Override
  public List<String> getIdsByRoomIdAndType(String roomId, FileMetadataType type) {
    return em.createQuery(
            "SELECT f.id FROM FileMetadata f WHERE f.roomId = :roomId AND f.type = :type",
            String.class)
        .setParameter("roomId", roomId)
        .setParameter("type", type)
        .getResultList();
  }

  @Override
  public List<FileMetadata> getByRoomIdAndType(
      String roomId,
      FileMetadataType type,
      int itemsNumber,
      @Nullable PaginationFilter paginationFilter,
      @Nullable AttachmentFilter attachmentFilter) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<FileMetadata> q = cb.createQuery(FileMetadata.class);
    Root<FileMetadata> r = q.from(FileMetadata.class);
    List<Predicate> where = roomTypePredicates(cb, r, roomId, type, attachmentFilter);

    boolean sortByCreatedAt = attachmentFilter == null || attachmentFilter.isSortByCreatedAt();
    boolean ascending = attachmentFilter != null && attachmentFilter.isAscending();
    String sortField = sortByCreatedAt ? "createdAt" : "originalSize";

    if (paginationFilter != null) {
      if (sortByCreatedAt) {
        where.add(
            cursor(cb, r.get(sortField), paginationFilter.getCreatedAt(), r, paginationFilter.getId(), ascending));
      } else {
        where.add(
            cursor(cb, r.get(sortField), paginationFilter.getSize(), r, paginationFilter.getId(), ascending));
      }
    }

    q.select(r)
        .where(where.toArray(Predicate[]::new))
        .orderBy(
            ascending ? cb.asc(r.get(sortField)) : cb.desc(r.get(sortField)),
            ascending ? cb.asc(r.get("id")) : cb.desc(r.get("id")));
    return em.createQuery(q).setMaxResults(itemsNumber).getResultList();
  }

  @Override
  public long countByRoomIdAndType(
      String roomId, FileMetadataType type, @Nullable AttachmentFilter attachmentFilter) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> q = cb.createQuery(Long.class);
    Root<FileMetadata> r = q.from(FileMetadata.class);
    q.select(cb.count(r))
        .where(roomTypePredicates(cb, r, roomId, type, attachmentFilter).toArray(Predicate[]::new));
    return em.createQuery(q).getSingleResult();
  }

  private static List<Predicate> roomTypePredicates(
      CriteriaBuilder cb,
      Root<FileMetadata> r,
      String roomId,
      FileMetadataType type,
      @Nullable AttachmentFilter attachmentFilter) {
    List<Predicate> where = new ArrayList<>();
    where.add(cb.equal(r.get("roomId"), roomId));
    where.add(cb.equal(r.get("type"), type));
    if (attachmentFilter != null) {
      if (attachmentFilter.getUserId() != null) {
        where.add(cb.equal(r.get("userId"), attachmentFilter.getUserId()));
      }
      Path<java.time.OffsetDateTime> createdAt = r.get("createdAt");
      if (attachmentFilter.getCreatedAfter() != null) {
        where.add(cb.greaterThan(createdAt, attachmentFilter.getCreatedAfter()));
      }
      if (attachmentFilter.getCreatedBefore() != null) {
        where.add(cb.lessThan(createdAt, attachmentFilter.getCreatedBefore()));
      }
      Path<Long> size = r.get("originalSize");
      if (attachmentFilter.getMinSize() != null) {
        where.add(cb.greaterThanOrEqualTo(size, attachmentFilter.getMinSize()));
      }
      if (attachmentFilter.getMaxSize() != null) {
        where.add(cb.lessThanOrEqualTo(size, attachmentFilter.getMaxSize()));
      }
      String mimeType = attachmentFilter.getMimeType();
      if (mimeType != null) {
        where.add(
            mimeType.endsWith("/")
                ? startsWith(cb, r.get("mimeType"), mimeType)
                : cb.equal(r.get("mimeType"), mimeType));
      }
      MimeTypeCategory category = attachmentFilter.getMimeTypeCategory();
      if (category != null) {
        // a prefixed category (IMAGES, VIDEOS) matches its prefix; DOCUMENTS is everything else
        String prefix = category.mimeTypePrefix();
        if (prefix != null) {
          where.add(startsWith(cb, r.get("mimeType"), prefix));
        } else {
          where.add(
              cb.not(
                  cb.or(
                      MimeTypeCategory.prefixedCategoryPrefixes().stream()
                          .map(p -> startsWith(cb, r.get("mimeType"), p))
                          .toArray(Predicate[]::new))));
        }
      }
    }
    return where;
  }

  private static Predicate startsWith(CriteriaBuilder cb, Path<String> path, String prefix) {
    String escaped = prefix.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return cb.like(path, escaped + "%", '\\');
  }

  private static <T extends Comparable<? super T>> Predicate cursor(
      CriteriaBuilder cb,
      Path<T> sortPath,
      T cursorValue,
      Root<FileMetadata> r,
      String cursorId,
      boolean ascending) {
    Path<String> id = r.get("id");
    return ascending
        ? cb.or(
            cb.greaterThan(sortPath, cursorValue),
            cb.and(cb.equal(sortPath, cursorValue), cb.greaterThan(id, cursorId)))
        : cb.or(
            cb.lessThan(sortPath, cursorValue),
            cb.and(cb.equal(sortPath, cursorValue), cb.lessThan(id, cursorId)));
  }

  @Override
  public List<String> getIdsByIdsAndRoomIdAndUserId(
      List<String> ids, String roomId, String userId) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return em.createQuery(
            "SELECT f.id FROM FileMetadata f WHERE f.id IN :ids AND f.roomId = :roomId"
                + " AND f.userId = :userId",
            String.class)
        .setParameter("ids", ids)
        .setParameter("roomId", roomId)
        .setParameter("userId", userId)
        .getResultList();
  }

  @Override
  public FileMetadata save(FileMetadata metadata) {
    if (metadata.getId() != null && em.find(FileMetadata.class, metadata.getId()) == null) {
      em.persist(metadata);
      return metadata;
    }
    return em.merge(metadata);
  }

  @Override
  public void delete(FileMetadata metadata) {
    FileMetadata managed = em.find(FileMetadata.class, metadata.getId());
    if (managed != null) {
      em.remove(managed);
    }
  }

  @Override
  public void deleteByIds(List<String> ids) {
    if (!ids.isEmpty()) {
      em.createQuery("DELETE FROM FileMetadata f WHERE f.id IN :ids")
          .setParameter("ids", ids)
          .executeUpdate();
    }
  }
}
