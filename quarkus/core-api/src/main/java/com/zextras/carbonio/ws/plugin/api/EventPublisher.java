// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

import com.zextras.carbonio.async.model.DomainEvent;
import java.util.Collection;
import java.util.UUID;

public interface EventPublisher {
  void sendToUsers(Collection<UUID> userIds, DomainEvent event);
}
