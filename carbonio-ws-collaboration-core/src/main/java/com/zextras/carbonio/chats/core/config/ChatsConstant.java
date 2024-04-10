// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

public class ChatsConstant {

  public static final String SERVER_HOST = "127.78.0.4";
  public static final int SERVER_PORT = 10000;
  public static final String LOGGER_CONFIG_PATH = "/etc/carbonio/ws-collaboration/logback.xml";
  public static final String MONGOOSEIM_GRAPHQL_ENDPOINT = "api/graphql";
  public static final String IS_UNICODE_FORMAT_REGEX = "[\\\\[uU][a-fA-F0-9]{4}]*";

  public static class CONFIGURATIONS_DEFAULT_VALUES {

    public static final boolean CAN_VIDEO_CALL = true;
    public static final boolean CAN_VIDEO_CALL_RECORD = true;
    public static final boolean CAN_USE_VIRTUAL_BACKGROUND = false;
    public static final boolean CAN_SEE_MESSAGE_READS = true;
    public static final boolean CAN_SEE_USERS_PRESENCE = true;
    public static final int MAX_USER_IMAGE_SIZE_IN_KB = 512;
    public static final int MAX_ROOM_IMAGE_SIZE_IN_KB = 512;
    public static final int EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES = 10;
    public static final int DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES = 10;
    public static final int MAX_GROUP_MEMBERS = 128;
  }
}
