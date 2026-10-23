// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class SystemEventContentFactory {

  private final ObjectMapper objectMapper;

  @Inject
  public SystemEventContentFactory(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String memberAdded(String addedByUserId, List<String> addedUserIds) {
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("addedByUserId", addedByUserId);
    content.put("addedUserIds", addedUserIds);
    return write(content);
  }

  public String memberRemoved(String removedByUserId, String removedUserId) {
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("removedByUserId", removedByUserId);
    content.put("removedUserId", removedUserId);
    return write(content);
  }

  public String roomNameChanged(String changedByUserId, String name) {
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("changedBy", changedByUserId);
    content.put("name", name);
    return write(content);
  }

  public String roomDescriptionChanged(String changedByUserId, String description) {
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("changedBy", changedByUserId);
    content.put("description", description);
    return write(content);
  }

  public String roomPictureUpdated(String updatedByUserId) {
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("updatedBy", updatedByUserId);
    return write(content);
  }

  public String roomPictureDeleted(String deletedByUserId) {
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("deletedBy", deletedByUserId);
    return write(content);
  }

  public String roomHistoryCleared(String clearedByUserId) {
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("clearedBy", clearedByUserId);
    return write(content);
  }

  private String write(Map<String, Object> content) {
    try {
      return objectMapper.writeValueAsString(content);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize system event content", e);
    }
  }
}
