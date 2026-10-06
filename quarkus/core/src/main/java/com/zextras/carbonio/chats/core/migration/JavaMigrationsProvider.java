// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import com.zextras.carbonio.chats.core.migration.scripts.V1_4_2__backfill_attachment_ids;
import java.util.List;
import org.flywaydb.core.api.migration.JavaMigration;

@ApplicationScoped
public class JavaMigrationsProvider {

  private final List<JavaMigration> migrations;

  @Inject
  public JavaMigrationsProvider(MessageDispatcherCredentials credentials) {
    this.migrations = List.of(new V1_4_2__backfill_attachment_ids(credentials));
  }

  public List<JavaMigration> get() {
    return migrations;
  }
}
