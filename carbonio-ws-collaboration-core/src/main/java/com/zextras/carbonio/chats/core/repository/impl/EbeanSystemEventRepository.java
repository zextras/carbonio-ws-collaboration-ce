// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.repository.DatabaseClock;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import io.ebean.Database;
import io.ebean.Query;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanSystemEventRepository implements SystemEventRepository {

  private final Database db;
  private final DatabaseClock databaseClock;

  @Inject
  public EbeanSystemEventRepository(Database db, DatabaseClock databaseClock) {
    this.db = db;
    this.databaseClock = databaseClock;
  }

  @Override
  public Optional<SystemEvent> getById(String id) {
    return db.find(SystemEvent.class).where().eq("id", id).findOneOrEmpty();
  }

  @Override
  public List<SystemEvent> getByIds(Collection<String> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return db.find(SystemEvent.class).where().in("id", ids).findList();
  }

  @Override
  public SystemEvent insert(SystemEvent event) {
    if (event.getCreatedAt() == null) {
      event.createdAt(databaseClock.now());
    }
    db.insert(event);
    return event;
  }

  @Override
  public List<SystemEvent> findByRoomId(String roomId) {
    return db.find(SystemEvent.class)
        .where()
        .eq("roomId", roomId)
        .orderBy()
        .asc("createdAt")
        .findList();
  }

  @Override
  public List<SystemEvent> findByRoomIdBounded(
      String roomId, OffsetDateTime before, OffsetDateTime after, int limit) {
    Query<SystemEvent> query = db.find(SystemEvent.class).where().eq("roomId", roomId).query();

    if (before != null) {
      query = query.where().lt("createdAt", before).query();
    }
    if (after != null) {
      query = query.where().gt("createdAt", after).query();
    }

    return query.orderBy("createdAt desc, id desc").setMaxRows(limit).findList();
  }

  @Override
  public List<SystemEvent> findByRoomIdComposite(
      String roomId, OffsetDateTime before, String beforeId, OffsetDateTime after, int limit) {
    Query<SystemEvent> query = db.find(SystemEvent.class).where().eq("roomId", roomId).query();

    if (before != null && beforeId != null) {
      query = query.where().raw("(created_at, id) < (?,?)", before, beforeId).query();
    } else if (before != null) {
      query = query.where().lt("createdAt", before).query();
    }
    if (after != null) {
      query = query.where().gt("createdAt", after).query();
    }

    return query.orderBy("createdAt desc, id desc").setMaxRows(limit).findList();
  }

  @Override
  public List<SystemEvent> findByRoomIdForward(
      String roomId, OffsetDateTime after, String afterId, OffsetDateTime before, int limit) {
    Query<SystemEvent> query = db.find(SystemEvent.class).where().eq("roomId", roomId).query();

    if (after != null && afterId != null) {
      query = query.where().raw("(created_at, id) > (?,?)", after, afterId).query();
    } else if (after != null) {
      query = query.where().gt("createdAt", after).query();
    }
    if (before != null) {
      query = query.where().lt("createdAt", before).query();
    }

    return query.orderBy("createdAt asc, id asc").setMaxRows(limit).findList();
  }

  @Override
  public void deleteByRoomId(String roomId) {
    db.find(SystemEvent.class).where().eq("roomId", roomId).delete();
  }
}
