// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.Recording;
import com.zextras.carbonio.chats.core.repository.RecordingRepository;
import io.ebean.Database;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class EbeanRecordingRepository implements RecordingRepository {

  private final Database db;

  @Inject
  public EbeanRecordingRepository(Database db) {
    this.db = db;
  }

  @Override
  public Recording insert(Recording recording) {
    db.insert(recording);
    return recording;
  }

  @Override
  public Recording update(Recording recording) {
    db.update(recording);
    return recording;
  }

  @Override
  public void delete(Recording recording) {
    db.delete(recording);
  }
}
