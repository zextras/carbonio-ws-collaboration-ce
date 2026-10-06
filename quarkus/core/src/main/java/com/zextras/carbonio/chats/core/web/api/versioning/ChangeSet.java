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
    return this.migrationClass.equals(targetClass)
        || this.migrationClass.isAssignableFrom(targetClass);
  }

  /**
   * A changeset is taken only if its version is newer than the requested version.
   *
   * <p>Example: If the requested version is 1.6.0 and the changeset version is 1.6.1, then the
   * changeset is considered newer and will be applied.
   *
   * <p>If the version is 1.6.1 and the requested version is 1.6.1, then the changeset is not
   * considered newer and will not be applied.
   */
  public boolean isNewerThan(Semver requestedVersion) {
    return this.version().isGreaterThan(requestedVersion);
  }

  @Override
  public int compareTo(@NotNull ChangeSet o) {
    return this.version.compareTo(o.version);
  }
}
