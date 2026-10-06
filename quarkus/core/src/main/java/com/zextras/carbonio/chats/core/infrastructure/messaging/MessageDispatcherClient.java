// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging;

import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.GraphQlBody;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.GraphQlResponse;

public interface MessageDispatcherClient {

  GraphQlResponse executeQuery(String query);

  GraphQlResponse executeMutation(GraphQlBody body);
}
