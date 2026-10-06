// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

/**
 * A plugin owning database tables exposes a CDI bean implementing this. Core runs one Flyway per
 * plugin in schema {@code chats} with history table {@code flyway_history_<pluginId>}.
 */
public interface PluginMigrations {
  String pluginId();

  /** Classpath location of the plugin's SQL migrations, e.g. {@code db/migration/recording}. */
  String location();
}
