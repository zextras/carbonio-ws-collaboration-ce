// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import io.ebean.Database;
import io.ebean.UpdateQuery;
import io.ebean.ValuePair;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.Property;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;

final class TimedUpdate {

  static final String UPDATED_AT_NOW = "updatedAt = clock_timestamp()";

  private TimedUpdate() {}

  static <T> void update(Database db, T bean, String... rawAssignments) {
    if (db.beanState(bean).isNew()) {
      throw new IllegalArgumentException("A new bean has no dirty tracking: insert it instead");
    }
    @SuppressWarnings("unchecked")
    Class<T> type = (Class<T>) bean.getClass();
    BeanType<T> beanType = db.pluginApi().beanType(type);
    Object id = db.beanId(bean);
    UpdateQuery<T> query = db.update(type);
    Map<String, ValuePair> dirty = db.beanState(bean).dirtyValues();
    dirty.forEach(
        (name, values) -> {
          Property property = beanType.property(name);
          if (property == null || property.isMany() || isEntity(db, property.type())) {
            throw new IllegalArgumentException(
                "Property '" + name + "' is not a plain column of " + type.getSimpleName());
          }
          if (values.getNewValue() == null) {
            query.setNull(name);
          } else {
            query.set(name, values.getNewValue());
          }
        });
    for (String raw : rawAssignments) {
      query.setRaw(raw);
    }
    int updated = query.where().idEq(id).update();
    if (updated != 1) {
      throw new EntityNotFoundException(
          type.getSimpleName() + " " + id + " was not updated: " + updated + " rows matched");
    }
  }

  private static boolean isEntity(Database db, Class<?> type) {
    return db.pluginApi().beanTypes().stream().anyMatch(candidate -> candidate.type() == type);
  }
}
