// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.usermanagement.entities.UserInfo;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;
import org.mockserver.model.Cookie;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.mockserver.model.Parameter;
import org.mockserver.verify.VerificationTimes;

public class UserManagementMockServer extends ClientAndServer implements CloseableResource {

  public UserManagementMockServer(Integer... ports) {
    super(ports);
  }

  public UserManagementMockServer(String remoteHost, Integer remotePort, Integer... ports) {
    super(remoteHost, remotePort, ports);
  }

  public void verify(String method, String path, int iterationsNumber) {
    HttpRequest request = request().withMethod(method).withPath(path);
    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.LOG);
  }

  public void verify(String method, String path, @Nullable String cookies, int iterationsNumber) {
    HttpRequest request = request().withMethod(method).withPath(path);
    Optional.ofNullable(cookies)
        .ifPresent(c -> request.withCookies(new Cookie("ZM_AUTH_TOKEN", c)));

    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.LOG);
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping user management mock...");
    super.close();
  }

  public HttpRequest getUsersBulkRequest(List<String> usersIds) {
    return request()
        .withMethod("GET")
        .withPath("/users/?")
        .withQueryStringParameters(
            usersIds.stream().map(p -> Parameter.param("userIds", p)).toList());
  }

  public void mockUsersBulk(List<String> usersIds, List<UserInfo> usersInfo, boolean success) {
    HttpRequest request = getUsersBulkRequest(usersIds);
    clear(request);
    when(request)
        .respond(HttpResponse.response().withStatusCode(200).withBody(JsonBody.json(usersInfo)));
  }
}
