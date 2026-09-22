// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.ConfigContribution;
import com.zextras.carbonio.chats.core.config.ConfigName;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** CE's own configuration keys, edition-free. Enterprise keys are contributed by Advanced. */
public class CoreConfigContribution implements ConfigContribution {

  private static final String LOCAL_SERVICE_ADDRESS = "127.78.0.4";

  @Override
  public Map<String, String> consulKvMappings() {
    Map<ConfigName, String> mappings = new EnumMap<>(ConfigName.class);
    mappings.put(ConfigName.DATABASE_USERNAME, "carbonio-ws-collaboration-db/db-username");
    mappings.put(ConfigName.DATABASE_PASSWORD, "carbonio-ws-collaboration-db/db-password");
    mappings.put(ConfigName.HIKARI_IDLE_TIMEOUT, "carbonio-ws-collaboration/hikari/idle-timeout");
    mappings.put(ConfigName.HIKARI_MIN_POOL_SIZE, "carbonio-ws-collaboration/hikari/min-pool-size");
    mappings.put(ConfigName.HIKARI_MAX_POOL_SIZE, "carbonio-ws-collaboration/hikari/max-pool-size");
    mappings.put(
        ConfigName.HIKARI_LEAK_DETECTION_THRESHOLD,
        "carbonio-ws-collaboration/hikari/leak-detection-threshold");
    mappings.put(ConfigName.HIKARI_MAX_LIFETIME, "carbonio-ws-collaboration/hikari/max-lifetime");
    mappings.put(ConfigName.XMPP_SERVER_USERNAME, "carbonio-message-dispatcher/api/username");
    mappings.put(ConfigName.XMPP_SERVER_PASSWORD, "carbonio-message-dispatcher/api/password");
    mappings.put(
        ConfigName.EVENT_DISPATCHER_USER_USERNAME, "carbonio-message-broker/default/username");
    mappings.put(
        ConfigName.EVENT_DISPATCHER_USER_PASSWORD, "carbonio-message-broker/default/password");
    mappings.put(ConfigName.CAN_VIDEO_CALL, "carbonio-ws-collaboration/configs/can-video-call");
    mappings.put(
        ConfigName.CAN_USE_VIRTUAL_BACKGROUND,
        "carbonio-ws-collaboration/configs/can-use-virtual-background");
    mappings.put(
        ConfigName.CAN_SEE_MESSAGE_READS,
        "carbonio-ws-collaboration/configs/can-see-message-reads");
    mappings.put(
        ConfigName.CAN_SEE_USERS_PRESENCE,
        "carbonio-ws-collaboration/configs/can-see-users-presence");
    mappings.put(
        ConfigName.MAX_USER_IMAGE_SIZE_IN_KB,
        "carbonio-ws-collaboration/configs/max-user-image-size-in-kb");
    mappings.put(
        ConfigName.MAX_ROOM_IMAGE_SIZE_IN_KB,
        "carbonio-ws-collaboration/configs/max-room-image-size-in-kb");
    mappings.put(
        ConfigName.EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES,
        "carbonio-ws-collaboration/configs/edit-message-time-limit-in-minutes");
    mappings.put(
        ConfigName.DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES,
        "carbonio-ws-collaboration/configs/delete-message-time-limit-in-minutes");
    mappings.put(
        ConfigName.MAX_GROUP_MEMBERS, "carbonio-ws-collaboration/configs/max-group-members");
    mappings.put(
        ConfigName.MAX_VIDEO_SIZE_PREVIEW_IN_MB,
        "carbonio-ws-collaboration/preview/max-video-size-preview-in-mb");
    mappings.put(ConfigName.VIRTUAL_HOST, "carbonio-ws-collaboration/broker/virtual-host");
    mappings.put(
        ConfigName.REQUESTED_HEARTBEAT_IN_SEC,
        "carbonio-ws-collaboration/broker/requested-heartbeat-in-sec");
    mappings.put(
        ConfigName.NETWORK_RECOVERY_INTERVAL_IN_MILLI,
        "carbonio-ws-collaboration/broker/network-recovery-interval-in-milli");
    mappings.put(
        ConfigName.CONNECTION_TIMEOUT_IN_MILLI,
        "carbonio-ws-collaboration/broker/connection-timeout-in-milli");
    mappings.put(
        ConfigName.AUTOMATIC_RECOVERY_ENABLED,
        "carbonio-ws-collaboration/broker/automatic-recovery-enabled");
    mappings.put(
        ConfigName.TOPOLOGY_RECOVERY_ENABLED,
        "carbonio-ws-collaboration/broker/topology-recovery-enabled");
    mappings.put(
        ConfigName.EVENT_DISPATCHER_POOL_SIZE,
        "carbonio-ws-collaboration/broker/connection-pool-size");
    mappings.put(ConfigName.VIDEO_SERVER_TOKEN, "carbonio-videoserver/api-secret");
    mappings.put(
        ConfigName.MESSAGE_DISPATCHER_DATABASE_HOST, "carbonio-message-dispatcher-db/db-host");
    mappings.put(
        ConfigName.MESSAGE_DISPATCHER_DATABASE_PORT, "carbonio-message-dispatcher-db/db-port");
    mappings.put(
        ConfigName.MESSAGE_DISPATCHER_DATABASE_NAME, "carbonio-message-dispatcher-db/db-name");
    mappings.put(
        ConfigName.MESSAGE_DISPATCHER_DATABASE_USERNAME,
        "carbonio-message-dispatcher-db/db-username");
    mappings.put(
        ConfigName.MESSAGE_DISPATCHER_DATABASE_PASSWORD,
        "carbonio-message-dispatcher-db/db-password");
    mappings.put(
        ConfigName.VIDEO_ROOM_BITRATE, "carbonio-ws-collaboration/meeting/videoroom-bitrate");
    mappings.put(
        ConfigName.VIDEO_ROOM_BITRATE_CAP,
        "carbonio-ws-collaboration/meeting/videoroom-bitrate-cap");
    mappings.put(ConfigName.MAX_THREADS, "carbonio-ws-collaboration/server/max-threads");
    mappings.put(ConfigName.MIN_THREADS, "carbonio-ws-collaboration/server/min-threads");
    mappings.put(
        ConfigName.MAX_QUEUE_REQUESTS, "carbonio-ws-collaboration/server/max-queue-requests");
    return toNames(mappings);
  }

  @Override
  public Set<String> environmentKeys() {
    return toNames(
        EnumSet.of(
            ConfigName.DATABASE_JDBC_URL,
            ConfigName.CONSUL_HOST,
            ConfigName.CONSUL_PORT,
            ConfigName.USER_MANAGEMENT_HOST,
            ConfigName.USER_MANAGEMENT_PORT,
            ConfigName.PREVIEWER_HOST,
            ConfigName.PREVIEWER_PORT,
            ConfigName.XMPP_SERVER_HOST,
            ConfigName.XMPP_SERVER_HTTP_PORT,
            ConfigName.XMPP_SERVER_USERNAME,
            ConfigName.XMPP_SERVER_PASSWORD,
            ConfigName.EVENT_DISPATCHER_HOST,
            ConfigName.EVENT_DISPATCHER_PORT,
            ConfigName.EVENT_DISPATCHER_USER_USERNAME,
            ConfigName.EVENT_DISPATCHER_USER_PASSWORD,
            ConfigName.VIDEO_SERVER_HOST,
            ConfigName.VIDEO_SERVER_PORT,
            ConfigName.STORAGES_HOST,
            ConfigName.STORAGES_PORT,
            ConfigName.VIDEO_SERVER_TOKEN,
            ConfigName.VIDEO_ROOM_BITRATE,
            ConfigName.VIDEO_ROOM_BITRATE_CAP,
            ConfigName.MESSAGE_DISPATCHER_DATABASE_HOST,
            ConfigName.MESSAGE_DISPATCHER_DATABASE_PORT));
  }

  @Override
  public Map<String, String> infrastructureDefaults() {
    Map<ConfigName, String> defaults = new EnumMap<>(ConfigName.class);
    defaults.put(
        ConfigName.DATABASE_JDBC_URL,
        "jdbc:postgresql://127.78.0.4:20003/carbonio-ws-collaboration-db");
    defaults.put(ConfigName.CONSUL_HOST, "localhost");
    defaults.put(ConfigName.CONSUL_PORT, "8500");
    defaults.put(ConfigName.STORAGES_HOST, LOCAL_SERVICE_ADDRESS);
    defaults.put(ConfigName.STORAGES_PORT, "20000");
    defaults.put(ConfigName.USER_MANAGEMENT_HOST, LOCAL_SERVICE_ADDRESS);
    defaults.put(ConfigName.USER_MANAGEMENT_PORT, "20001");
    defaults.put(ConfigName.PREVIEWER_HOST, LOCAL_SERVICE_ADDRESS);
    defaults.put(ConfigName.PREVIEWER_PORT, "20002");
    defaults.put(ConfigName.XMPP_SERVER_HOST, LOCAL_SERVICE_ADDRESS);
    defaults.put(ConfigName.XMPP_SERVER_HTTP_PORT, "20004");
    defaults.put(ConfigName.EVENT_DISPATCHER_HOST, LOCAL_SERVICE_ADDRESS);
    defaults.put(ConfigName.EVENT_DISPATCHER_PORT, "20005");
    defaults.put(ConfigName.VIDEO_SERVER_HOST, LOCAL_SERVICE_ADDRESS);
    defaults.put(ConfigName.VIDEO_SERVER_PORT, "20006");
    defaults.put(ConfigName.MESSAGE_DISPATCHER_DATABASE_HOST, LOCAL_SERVICE_ADDRESS);
    defaults.put(ConfigName.MESSAGE_DISPATCHER_DATABASE_PORT, "20012");
    return toNames(defaults);
  }

  private static Map<String, String> toNames(Map<ConfigName, String> map) {
    Map<String, String> result = new HashMap<>();
    map.forEach((key, value) -> result.put(key.name(), value));
    return result;
  }

  private static Set<String> toNames(Set<ConfigName> keys) {
    return keys.stream().map(ConfigName::name).collect(Collectors.toSet());
  }
}
