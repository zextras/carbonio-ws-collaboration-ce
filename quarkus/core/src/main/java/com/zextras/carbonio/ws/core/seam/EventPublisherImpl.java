// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.seam;

import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.ws.plugin.api.EventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.UUID;

/** {@link EventPublisher} over the CE {@link EventDispatcher} (per-user RabbitMQ exchanges). */
@ApplicationScoped
public class EventPublisherImpl implements EventPublisher {

  private final EventDispatcher eventDispatcher;

  @Inject
  public EventPublisherImpl(EventDispatcher eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  public void sendToUsers(Collection<UUID> userIds, DomainEvent event) {
    eventDispatcher.sendToUserExchange(
        userIds.stream().map(UUID::toString).distinct().toList(), event);
  }
}
