// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.versioning.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.openapi.versioning.VersionProvider;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class VersionedRequestFilterTest {

  private final VersionedRequestFilter filter = new VersionedRequestFilter();

  private static ContainerRequestContext requestWithVersion(String version) {
    ContainerRequestContext context = mock(ContainerRequestContext.class);
    when(context.getHeaderString(ChatsConstant.API_VERSION_HEADER)).thenReturn(version);
    return context;
  }

  private static Response abortedWith(ContainerRequestContext context) {
    ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
    verify(context).abortWith(response.capture());
    return response.getValue();
  }

  @Test
  void aSupportedVersionPasses() throws Exception {
    ContainerRequestContext context = requestWithVersion(VersionProvider.getVersion());

    filter.filter(context);

    verify(context, never()).abortWith(any());
  }

  @Test
  void aRequestWithoutTheHeaderPasses() throws Exception {
    ContainerRequestContext context = requestWithVersion(null);

    filter.filter(context);

    verify(context, never()).abortWith(any());
  }

  @Test
  void anOlderUnsupportedVersionIsRejectedWith422AndTheServerVersion() throws Exception {
    ContainerRequestContext context = requestWithVersion("1.6.14");

    filter.filter(context);

    Response response = abortedWith(context);
    assertEquals(422, response.getStatus());
    assertEquals(
        List.of(VersionProvider.getVersion()),
        response.getHeaders().get(ChatsConstant.API_VERSION_HEADER));
  }

  @Test
  void aNewerVersionIsRejectedWith422AndTheServerVersion() throws Exception {
    ContainerRequestContext context = requestWithVersion("3.0.0");

    filter.filter(context);

    Response response = abortedWith(context);
    assertEquals(422, response.getStatus());
    assertEquals(
        List.of(VersionProvider.getVersion()),
        response.getHeaders().get(ChatsConstant.API_VERSION_HEADER));
  }

  @Test
  void aMalformedVersionIsRejectedWith400() throws Exception {
    ContainerRequestContext context = requestWithVersion("not a version");

    filter.filter(context);

    assertEquals(400, abortedWith(context).getStatus());
  }
}
