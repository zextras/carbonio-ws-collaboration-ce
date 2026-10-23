// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration;

import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import com.zextras.carbonio.chats.core.migration.mongooseimport.MongooseImporter;
import java.util.Arrays;
import org.flywaydb.core.api.migration.Context;

public class V2_0_1__migrate_mongoose_data extends AtomicJavaMigration {

  private final MessageDispatcherCredentials credentials;
  private final byte[] runKey;

  public V2_0_1__migrate_mongoose_data(MessageDispatcherCredentials credentials, byte[] runKey) {
    this.credentials = credentials;
    this.runKey = runKey.clone();
  }

  @Override
  protected void doMigrate(Context context) throws Exception {
    try {
      MongooseImporter.migrate(credentials, context.getConnection(), runKey);
    } finally {
      Arrays.fill(runKey, (byte) 0);
    }
  }
}
