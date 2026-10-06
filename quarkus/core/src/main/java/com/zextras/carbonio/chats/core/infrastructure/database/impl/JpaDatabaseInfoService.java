// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.database.impl;

import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** Replaces CE's Ebean implementation: a plain {@code SELECT 1} on the Quarkus datasource. */
@ApplicationScoped
public class JpaDatabaseInfoService implements DatabaseInfoService {

  private final AgroalDataSource dataSource;

  @Inject
  public JpaDatabaseInfoService(AgroalDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public boolean isAlive() {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT 1 AS health_check")) {
      return resultSet.next();
    } catch (Exception e) {
      ChatsLogger.warn("Database health check failed: " + e.getMessage());
      return false;
    }
  }
}
