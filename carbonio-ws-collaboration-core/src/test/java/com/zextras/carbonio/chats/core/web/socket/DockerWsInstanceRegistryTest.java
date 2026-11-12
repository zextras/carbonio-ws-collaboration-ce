// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DockerWsInstanceRegistryTest {

  @Test
  @DisplayName("Without configuration the fixed default id is used and is the only live instance")
  void selfId_notConfigured_usesDefaultAndIsLive() {
    DockerWsInstanceRegistry registry = new DockerWsInstanceRegistry(null);

    assertEquals(DockerWsInstanceRegistry.DEFAULT_INSTANCE_ID, registry.selfId());
    assertEquals(Set.of(registry.selfId()), registry.liveInstanceIds());
  }

  @Test
  @DisplayName("A blank configured id falls back to the default")
  void selfId_blank_usesDefault() {
    assertEquals(
        DockerWsInstanceRegistry.DEFAULT_INSTANCE_ID, new DockerWsInstanceRegistry(" ").selfId());
  }

  @Test
  @DisplayName("A configured id overrides the default and is the only live instance")
  void selfId_configured_overridesDefault() {
    DockerWsInstanceRegistry registry =
        new DockerWsInstanceRegistry("4d1f5b8e-2f6a-4a3c-9a3e-7d6f5c0b1a11");

    assertEquals("4d1f5b8e-2f6a-4a3c-9a3e-7d6f5c0b1a11", registry.selfId());
    assertEquals(Set.of("4d1f5b8e-2f6a-4a3c-9a3e-7d6f5c0b1a11"), registry.liveInstanceIds());
  }

  @Test
  @DisplayName("A configured id that is not a UUID is rejected")
  void constructor_invalidUuid_throws() {
    assertThrows(IllegalArgumentException.class, () -> new DockerWsInstanceRegistry("not-a-uuid"));
  }
}
