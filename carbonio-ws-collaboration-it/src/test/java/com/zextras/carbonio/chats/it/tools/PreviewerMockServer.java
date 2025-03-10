// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;

public class PreviewerMockServer extends ClientAndServer implements CloseableResource {

  public PreviewerMockServer(Integer... ports) {
    super(ports);
  }

  public PreviewerMockServer(String remoteHost, Integer remotePort, Integer... ports) {
    super(remoteHost, remotePort, ports);
  }

  public void setIsAliveResponse(boolean success) {
    HttpRequest request = request().withMethod("GET").withPath("/health/ready/");
    clear(request);
    when(request).respond(response().withStatusCode(success ? 200 : 500));
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping previewer mock...");
    super.close();
  }
}
