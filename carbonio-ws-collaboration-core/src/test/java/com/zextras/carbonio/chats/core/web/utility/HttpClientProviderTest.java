// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.utility;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class HttpClientProviderTest {

  @Test
  void connectionEvictor_doesNotKeepAFailedBootAlive() {
    HttpClientProvider.getHttpClient();

    Thread evictor =
        Thread.getAllStackTraces().keySet().stream()
            .filter(t -> t.getName().equals("Http client connection evictor"))
            .findFirst()
            .orElseThrow();

    assertTrue(evictor.isDaemon());
  }
}
