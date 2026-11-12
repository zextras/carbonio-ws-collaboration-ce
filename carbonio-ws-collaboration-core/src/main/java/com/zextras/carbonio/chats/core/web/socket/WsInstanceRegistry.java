// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import java.util.Set;

public interface WsInstanceRegistry {

  String selfId();

  Set<String> liveInstanceIds();
}
