// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.JavaMigration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class DatabaseModuleJavaMigrationsTest {

  private final DatabaseModule module = new DatabaseModule();
  private final MessageDispatcherCredentials credentials =
      new MessageDispatcherCredentials("localhost", 5432, null, null, null);

  @Test
  @DisplayName("Provides the Java migrations 1.4.2 and 2.0.1 in order")
  void versions() {
    List<String> versions =
        module.javaMigrations(credentials).stream().map(m -> m.getVersion().getVersion()).toList();
    assertEquals(List.of("1.4.2", "2.0.1"), versions);
  }

  @Test
  @DisplayName("Java migration versions do not collide with the SQL migrations of the location")
  void noDuplicateVersions() {
    String location = module.migrationsLocation().replaceFirst("^classpath:", "");
    File directory = new File(getClass().getClassLoader().getResource(location).getFile());
    File[] scripts = directory.listFiles((dir, name) -> name.endsWith(".sql"));
    assertNotNull(scripts);
    List<MigrationVersion> all = new ArrayList<>();
    for (File script : scripts) {
      String name = script.getName();
      all.add(
          MigrationVersion.fromVersion(name.substring(1, name.indexOf("__")).replace('_', '.')));
    }
    for (JavaMigration migration : module.javaMigrations(credentials)) {
      all.add(migration.getVersion());
    }
    Set<MigrationVersion> seen = new HashSet<>();
    for (MigrationVersion version : all) {
      assertTrue(seen.add(version), "duplicate version " + version);
    }
  }
}
