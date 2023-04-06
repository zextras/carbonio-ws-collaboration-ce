// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import com.zextras.carbonio.chats.core.config.impl.AppConfigType;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ConfigName {

  // infrastructure configurations
  ENV("ws-collaboration.env", "WS_COLLABORATION_ENV", "carbonio-ws-collaboration/ws-collaboration-env"),
  JDBC_DRIVER("database.jdbc.driver", "DATABASE_JDBC_DRIVER", "carbonio-ws-collaboration-db/jdbc-driver"),
  DATABASE_JDBC_URL("database.jdbc.url", "DATABASE_JDBC_URL", "carbonio-ws-collaboration-db/jdbc-url"),
  DATABASE_USERNAME("database.username", "DATABASE_USERNAME", "carbonio-ws-collaboration-db/db-username"),
  DATABASE_PASSWORD("database.password", "DATABASE_PASSWORD", "carbonio-ws-collaboration-db/db-password"),
  HIKARI_IDLE_TIMEOUT("datasource.hikari.idleTimeout", "HIKARI_IDLE_TIMEOUT", "carbonio-ws-collaboration/hikari-idle-timeout"),
  HIKARI_MIN_POOL_SIZE("datasource.hikari.minPoolSize", "HIKARI_MIN_POOL_SIZE", "carbonio-ws-collaboration/hikari-min-pool-size"),
  HIKARI_MAX_POOL_SIZE("datasource.hikari.maxPoolSize", "HIKARI_MAX_POOL_SIZE", "carbonio-ws-collaboration/hikari-max-pool-size"),
  HIKARI_LEAK_DETECTION_THRESHOLD("datasource.hikari.leakDetectionThreshold", "HIKARI_LEAK_DETECTION_THRESHOLD",
    "carbonio-ws-collaboration/hikari-leak-detection-threshold"),
  XMPP_SERVER_HOST("xmpp.host", "XMPP_HOST", "carbonio-ws-collaboration/xmpp-host"),
  XMPP_SERVER_HTTP_PORT("xmpp.http.port", "XMPP_HTTP_PORT", "carbonio-ws-collaboration/xmpp-http-port"),
  XMPP_SERVER_USERNAME("xmpp.username", "XMPP_USERNAME", "carbonio-message-dispatcher/api/username"),
  XMPP_SERVER_PASSWORD("xmpp.password", "XMPP_PASSWORD", "carbonio-message-dispatcher/api/password"),
  STORAGES_HOST("carbonio.storages.host", "STORAGES_HOST", "carbonio-ws-collaboration/storages-host"),
  STORAGES_PORT("carbonio.storages.port", "STORAGES_PORT", "carbonio-ws-collaboration/storages-port"),
  USER_MANAGEMENT_HOST("carbonio.usermanagement.host", "USER_MANAGEMENT_HOST", "carbonio-ws-collaboration/user-management-host"),
  USER_MANAGEMENT_PORT("carbonio.usermanagement.port", "USER_MANAGEMENT_PORT", "carbonio-ws-collaboration/user-management-port"),
  PREVIEWER_HOST("carbonio.preview.host", "PREVIEWER_HOST", "carbonio-ws-collaboration/previewer-host"),
  PREVIEWER_PORT("carbonio.preview.port", "PREVIEWER_PORT", "carbonio-ws-collaboration/previewer-port"),
  CONSUL_TOKEN("consul.agent.token", "CONSUL_HTTP_TOKEN", "carbonio-ws-collaboration/consul-http-token"),
  CONSUL_HOST("consul.host", "CONSUL_HOST", "carbonio-ws-collaboration/consul-host"),
  CONSUL_PORT("consul.port", "CONSUL_PORT", "carbonio-ws-collaboration/consul-port"),
  EVENT_DISPATCHER_HOST("event.dispatcher.host", "EVENT_DISPATCHER_HOST", "carbonio-ws-collaboration/event-dispatcher-host"),
  EVENT_DISPATCHER_PORT("event.dispatcher.port", "EVENT_DISPATCHER_PORT", "carbonio-ws-collaboration/event-dispatcher-port"),
  EVENT_DISPATCHER_USER_USERNAME("event.dispatcher.user.username", "EVENT_DISPATCHER_USER_USERNAME",
    "carbonio-message-broker/username"),
  EVENT_DISPATCHER_USER_PASSWORD("event.dispatcher.user.password", "EVENT_DISPATCHER_USER_PASSWORD",
    "carbonio-message-broker/password"),
  VIDEO_SERVER_HOST("video.server.host", "VIDEO_SERVER_HOST", "carbonio-ws-collaboration/video-server-host"),
  VIDEO_SERVER_PORT("video.server.port", "VIDEO_SERVER_PORT", "carbonio-ws-collaboration/video-server-port"),

  // behavioral configurations
  CAN_SEE_MESSAGE_READS("can.see.message.reads", "CAN_SEE_MESSAGE_READS",
    "carbonio-ws-collaboration/configs/can-see-message-reads"),
  CAN_SEE_USERS_PRESENCE("can.see.users.presence", "CAN_SEE_USERS_PRESENCE",
    "carbonio-ws-collaboration/configs/can-see-users-presence"),
  MAX_USER_IMAGE_SIZE_IN_KB("max.user.image.size.in.kb", "MAX_USER_IMAGE_SIZE_IN_KB",
    "carbonio-ws-collaboration/configs/max-user-image-size-in-kb"),
  MAX_ROOM_IMAGE_SIZE_IN_KB("max.room.image.size.in.kb", "MAX_ROOM_IMAGE_SIZE_IN_KB",
    "carbonio-ws-collaboration/configs/max-room-image-size-in-kb"),
  EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES("edit.message.time.limit.in.minutes", "EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES",
    "carbonio-ws-collaboration/configs/edit-message-time-limit-in-minutes"),
  DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES("delete.message.time.limit.in.minutes", "DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES",
    "carbonio-ws-collaboration/configs/delete-message-time-limit-in-minutes"),
  MAX_GROUP_MEMBERS("max.group.members", "MAX_GROUP_MEMBERS", "carbonio-ws-collaboration/configs/max-group-members");


  private final String propertyName;
  private final String envName;
  private final String consulName;

  ConfigName(String propertyName, String envName, String consulName) {
    this.propertyName = propertyName;
    this.envName = envName;
    this.consulName = consulName;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public String getEnvName() {
    return envName;
  }

  public String getConsulName() {
    return consulName;
  }

  public static Optional<ConfigName> getByName(String name, AppConfigType configType) {
    Map<AppConfigType, Function<ConfigName, String>> typeMap = Map.of(
      AppConfigType.ENVIRONMENT, configName -> configName.envName,
      AppConfigType.PROPERTY, configName -> configName.propertyName,
      AppConfigType.CONSUL, configName -> configName.consulName
    );
    return Arrays.stream(ConfigName.values())
      .filter(item -> typeMap.get(configType).apply(item).equals(name)
      ).findAny();
  }

  public static Set<String> getConsulPrefixes() {
    return Arrays.stream(ConfigName.values())
      .map(name -> name.consulName.substring(0, name.consulName.indexOf("/") + 1))
      .collect(Collectors.toSet());
  }
}