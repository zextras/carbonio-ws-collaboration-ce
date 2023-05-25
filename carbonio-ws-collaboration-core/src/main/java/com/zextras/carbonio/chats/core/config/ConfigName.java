// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

public enum ConfigName {

  // infrastructure configurations
  DATABASE_JDBC_DRIVER,
  DATABASE_JDBC_URL,
  DATABASE_USERNAME,
  DATABASE_PASSWORD,
  HIKARI_IDLE_TIMEOUT,
  HIKARI_MIN_POOL_SIZE,
  HIKARI_MAX_POOL_SIZE,
  HIKARI_LEAK_DETECTION_THRESHOLD,
  XMPP_SERVER_HOST,
  XMPP_SERVER_HTTP_PORT,
  XMPP_SERVER_USERNAME,
  XMPP_SERVER_PASSWORD,
  STORAGES_HOST,
  STORAGES_PORT,
  USER_MANAGEMENT_HOST,
  USER_MANAGEMENT_PORT,
  PREVIEWER_HOST,
  PREVIEWER_PORT,
  CONSUL_HOST,
  CONSUL_PORT,
  EVENT_DISPATCHER_HOST,
  EVENT_DISPATCHER_PORT,
  EVENT_DISPATCHER_USER_USERNAME,
  EVENT_DISPATCHER_USER_PASSWORD,
  VIDEO_SERVER_HOST,
  VIDEO_SERVER_PORT,

  // behavioral configurations
  CAN_SEE_MESSAGE_READS,
  CAN_SEE_USERS_PRESENCE,
  MAX_USER_IMAGE_SIZE_IN_KB,
  MAX_ROOM_IMAGE_SIZE_IN_KB,
  EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES,
  DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES,
  MAX_GROUP_MEMBERS,

  // broker configurations
  VIRTUAL_HOST,
  REQUESTED_HEARTBEAT_IN_SEC,
  NETWORK_RECOVERY_INTERVAL_IN_MILLI,
  CONNECTION_TIMEOUT_IN_MILLI,
  AUTOMATIC_RECOVERY_ENABLED,
  TOPOLOGY_RECOVERY_ENABLED
}