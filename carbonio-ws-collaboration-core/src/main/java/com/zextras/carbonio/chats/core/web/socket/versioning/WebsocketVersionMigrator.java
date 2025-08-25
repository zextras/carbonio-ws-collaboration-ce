// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket.versioning;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.vdurmont.semver4j.Semver;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.web.api.versioning.VersionMigrationsRegistry;
import java.util.EnumMap;
import java.util.Map;

public class WebsocketVersionMigrator {

  private final ObjectMapper mapper;
  private final VersionMigrationsRegistry registry = VersionMigrationsRegistry.REGISTRY;

  private static final Map<EventType, Class<? extends DomainEvent>> eventTypeClassRegistry =
      new EnumMap<>(EventType.class);

  static {
    initializeEventTypeClassRegistry();
  }

  @Inject
  public WebsocketVersionMigrator(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  private String downgrade(DomainEvent event, Semver apiVersion) {
    Class<?> eventClass = eventTypeClassRegistry.get(event.getType());

    var apiVersionMigrator = registry.migratorFor(apiVersion, eventClass);
    try {
      var downgradedMessage = apiVersionMigrator.downgrade(mapper.readTree(toStringValue(event)));
      return toStringValue(downgradedMessage);
    } catch (JsonProcessingException e) {
      throw new InternalErrorException("Error serializing event: %s", e);
    }
  }

  public String downgradeIfNeeded(DomainEvent event, String version)
      throws JsonProcessingException {
    Semver apiVersion = new Semver(version);
    if (needsDowngrade(apiVersion)) {
      return downgrade(event, apiVersion);
    }

    return toStringValue(event);
  }

  public String downgradeIfNeeded(String message, String version) throws JsonProcessingException {
    Semver apiVersion = new Semver(version);

    if (needsDowngrade(apiVersion)) {
      return downgrade(parseDomainEvent(message), apiVersion);
    }

    return message;
  }

  private boolean needsDowngrade(Semver apiVersion) {
    return registry.hasMigrationsAfter(apiVersion);
  }

  private DomainEvent parseDomainEvent(String message) throws JsonProcessingException {
    return mapper.readValue(message, DomainEvent.class);
  }

  private static void initializeEventTypeClassRegistry() {
    JsonSubTypes annotation = DomainEvent.class.getAnnotation(JsonSubTypes.class);
    if (annotation == null) return;

    for (JsonSubTypes.Type subType : annotation.value()) {
      EventType eventType = findEventTypeByName(subType.name());
      if (eventType != null && DomainEvent.class.isAssignableFrom(subType.value())) {
        eventTypeClassRegistry.put(eventType, (Class<? extends DomainEvent>) subType.value());
      }
    }
  }

  private static EventType findEventTypeByName(String name) {
    for (EventType eventType : EventType.values()) {
      if (eventType.toString().equals(name)) {
        return eventType;
      }
    }
    return null;
  }

  private String toStringValue(Object o) throws JsonProcessingException {
    return mapper.writeValueAsString(o);
  }
}
