// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.versioning;

import com.vdurmont.semver4j.Semver;
import com.zextras.carbonio.async.model.DomainEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class VersionMigrationsRegistry {

  public static final VersionMigrationsRegistry REGISTRY = new VersionMigrationsRegistry();

  private final Set<ChangeSet> migrations = new TreeSet<>(Comparator.reverseOrder());

  public void register(ChangeSet changeSet) {
    this.migrations.add(changeSet);
  }

  public void clear() {
    this.migrations.clear();
  }

  public List<ApiVersionMigration> getMigrationsAfter(
      Semver requestedVersion, Class<?> migrationClass) {
    return migrations.stream()
        .filter(changeSet -> changeSet.appliesToClass(migrationClass))
        .filter(changeSet -> changeSet.isNewerThan(requestedVersion))
        .flatMap(changeSet -> changeSet.migrations().stream())
        .toList();
  }

  public boolean hasMigrationsAfter(Semver requestedVersion) {
    return migrations.stream().anyMatch(c -> c.isNewerThan(requestedVersion));
  }

  public ApiVersionMigrator migratorFor(Semver version, Class<?> responseClass) {
    return new ApiVersionMigrator(getMigrationsAfter(version, responseClass));
  }

  static {
    REGISTRY.register(
        new ChangeSet(
            new Semver("1.6.2"), DomainEvent.class, List.of(new ChangeEvenTypeNameMigration())));
  }
}
