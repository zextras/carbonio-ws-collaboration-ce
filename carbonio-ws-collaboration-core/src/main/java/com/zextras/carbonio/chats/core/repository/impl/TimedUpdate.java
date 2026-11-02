// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import io.ebean.Database;
import io.ebean.UpdateQuery;
import io.ebean.ValuePair;
import java.util.Map;

final class TimedUpdate {

  static final String UPDATED_AT_NOW = "updatedAt = clock_timestamp()";

  private TimedUpdate() {}

  static <T> int update(Database db, T bean, Object id, String... rawAssignments) {
    @SuppressWarnings("unchecked")
    UpdateQuery<T> query = db.update((Class<T>) bean.getClass());
    Map<String, ValuePair> dirty = db.beanState(bean).dirtyValues();
    dirty.forEach(
        (property, values) -> {
          if (values.getNewValue() == null) {
            query.setNull(property);
          } else {
            query.set(property, values.getNewValue());
          }
        });
    for (String raw : rawAssignments) {
      query.setRaw(raw);
    }
    return query.where().idEq(id).update();
  }
}
