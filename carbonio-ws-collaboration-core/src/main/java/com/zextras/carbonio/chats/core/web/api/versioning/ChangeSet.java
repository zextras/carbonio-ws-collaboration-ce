// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.versioning;

import com.vdurmont.semver4j.Semver;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record ChangeSet(
    Semver version, Class<?> migrationClass, List<ApiVersionMigration> migrations)
    implements Comparable<ChangeSet> {

  public boolean appliesToClass(Class<?> targetClass) {
    return this.migrationClass.equals(targetClass);
  }

  public boolean isNewerThan(Semver version) {
    return this.version().isGreaterThan(version);
  }

  @Override
  public int compareTo(@NotNull ChangeSet o) {
    return this.version.compareTo(o.version);
  }
}
