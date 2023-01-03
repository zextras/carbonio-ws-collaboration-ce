// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import java.util.List;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
public class SupportedApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper              objectMapper;

  public SupportedApiIT(
    ResteasyRequestDispatcher dispatcher,
    ObjectMapper objectMapper
  ) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
  }

  @Test
  @DisplayName("Gets latest project version")
  public void getSupportedVersions_testOk() throws Exception {
    MockHttpResponse response = dispatcher.get("/supported", null);
    assertEquals(200, response.getStatus());
    List<String> versions = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
    });
    assertNotNull(versions);
    assertEquals(1, versions.size());
    assertEquals("1.0.0", versions.get(0));
  }
}
