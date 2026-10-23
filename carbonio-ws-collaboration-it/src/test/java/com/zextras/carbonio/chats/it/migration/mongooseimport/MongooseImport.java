// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import com.zextras.carbonio.chats.core.migration.V2_0_1__migrate_mongoose_data;
import com.zextras.carbonio.chats.core.migration.scripts.V1_4_2__backfill_attachment_ids;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.TimeZone;
import org.flywaydb.core.Flyway;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.slf4j.LoggerFactory;

public final class MongooseImport {

  public static final byte[] RUN_KEY =
      HexFormat.of().parseHex("6d6f6e676f6f73652d696d706f72742d69742d72756e2d6b65792d3230323621");
  public static final ZoneId DUMP_ZONE = ZoneId.of("UTC");

  private static ImportRun dumpRun;

  private MongooseImport() {}

  public static synchronized ImportRun ofDump() {
    if (dumpRun == null) {
      MongooseImportDatabase database = MongooseImportDatabase.get();
      dumpRun = run(database.newTargetDatabase(), database.sourceCredentials(), DUMP_ZONE);
    }
    return dumpRun;
  }

  public static ImportRun run(String targetDatabase, MessageDispatcherCredentials source) {
    return run(targetDatabase, source, DUMP_ZONE);
  }

  public static ImportRun run(
      String targetDatabase, MessageDispatcherCredentials source, ZoneId jvmZone) {
    MongooseImportDatabase database = MongooseImportDatabase.get();
    Flyway flyway =
        Flyway.configure()
            .locations("classpath:migration/ce")
            .schemas("chats")
            .dataSource(database.jdbcUrl(targetDatabase), database.user(), database.password())
            .validateMigrationNaming(true)
            .lockRetryCount(-1)
            .group(true)
            .javaMigrations(
                new V1_4_2__backfill_attachment_ids(source),
                new V2_0_1__migrate_mongoose_data(source, RUN_KEY.clone()))
            .load();
    flyway
        .getConfigurationExtension(PostgreSQLConfigurationExtension.class)
        .setTransactionalLock(false);

    Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    root.addAppender(appender);
    TimeZone previousZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone(jvmZone));
    Throwable failure = null;
    try {
      flyway.migrate();
    } catch (Throwable t) {
      failure = t;
    } finally {
      TimeZone.setDefault(previousZone);
      root.detachAppender(appender);
    }
    return new ImportRun(targetDatabase, failure, render(appender.list), jvmZone);
  }

  private static List<String> render(List<ILoggingEvent> events) {
    List<String> lines = new ArrayList<>();
    for (ILoggingEvent event : events) {
      lines.add(event.getLevel() + " " + event.getLoggerName() + " " + event.getFormattedMessage());
      if (event.getThrowableProxy() != null) {
        lines.add(ThrowableProxyUtil.asString(event.getThrowableProxy()));
      }
    }
    return lines;
  }
}
