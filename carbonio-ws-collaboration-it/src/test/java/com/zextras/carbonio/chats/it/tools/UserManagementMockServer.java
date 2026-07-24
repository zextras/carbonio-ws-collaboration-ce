// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockUserProfile;
import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
import com.zextras.carbonio.user_management.sdk.rest.model.UserInfoDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;

/**
 * MockServer-backed stand-in for the User Management REST service. Replaces the previous
 * in-process gRPC mock server: it stubs the {@code /internal/users/*} REST endpoints instead of
 * implementing a gRPC service, but keeps the same in-memory-user seeding API
 * ({@link #registerToken(String, MyselfDto)}, {@link #registerUserById(String, UserInfoDto)}) so
 * existing tests don't need to change how they inject fixtures.
 */
public class UserManagementMockServer extends ClientAndServer implements CloseableResource {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Map<String, UserInfoDto> userIdToInfo = new HashMap<>();

  public UserManagementMockServer(Integer... ports) {
    super(ports);
    init();
  }

  public UserManagementMockServer(String remoteHost, Integer remotePort, Integer... ports) {
    super(remoteHost, remotePort, ports);
    init();
  }

  private void init() {
    registerMockedAccounts();
    mockBulkGetUsers();
    setIsAliveResponse(true);
  }

  /**
   * Stubs the {@code /q/health/live} liveness probe used by {@code isAlive()} in
   * UserManagementAuthenticationService/UserManagementProfilingService. Defaults to alive at
   * construction, mirroring the old in-process gRPC channel which always reported READY in tests.
   */
  public void setIsAliveResponse(boolean success) {
    HttpRequest request = request().withMethod("GET").withPath("/q/health/live");
    clear(request);
    when(request).respond(response().withStatusCode(success ? 200 : 500));
  }

  private void registerMockedAccounts() {
    for (MockUserProfile account : MockedAccount.getAccounts()) {
      String type = account.getType() == UserType.GUEST ? "GUEST" : "INTERNAL";

      UserInfoDto userInfo =
          new UserInfoDto()
              .userId(account.getId())
              .email(account.getEmail())
              .fullName(account.getName())
              .domain(deriveDomain(account))
              .status("active")
              .type(type);

      MyselfDto userMyself =
          new MyselfDto().info(userInfo).locale("en").capabilities(getDefaultCapabilities());
      userMyself.addFeaturesItem("carbonioFeatureWscEnabled");

      registerToken(account.getToken(), userMyself);
      registerUserById(account.getId(), userInfo);
    }
  }

  /** Stubs {@code GET /internal/users/myself} with the {@code ZM_AUTH_TOKEN} cookie for the given token. */
  public void registerToken(String token, MyselfDto userMyself) {
    HttpRequest request =
        request()
            .withMethod("GET")
            .withPath("/internal/users/myself")
            .withHeader(Header.header("Cookie", "ZM_AUTH_TOKEN=" + token));
    clear(request);
    when(request).respond(response().withStatusCode(200).withBody(JsonBody.json(userMyself)));
  }

  /**
   * Stubs {@code GET /internal/users/id/{userId}} and registers the user so it is also returned
   * by the bulk {@code POST /internal/users} endpoint.
   */
  public void registerUserById(String userId, UserInfoDto userInfo) {
    userIdToInfo.put(userId, userInfo);
    HttpRequest request = request().withMethod("GET").withPath("/internal/users/id/" + userId);
    clear(request);
    when(request).respond(response().withStatusCode(200).withBody(JsonBody.json(userInfo)));
  }

  /**
   * Stubs {@code POST /internal/users} once with a dynamic callback: it reads the requested user
   * ids from the JSON request body and returns whichever of them are currently registered, mirroring
   * the previous gRPC mock's {@code getUsers} behaviour.
   */
  private void mockBulkGetUsers() {
    when(request().withMethod("POST").withPath("/internal/users"))
        .respond(
            httpRequest -> {
              List<String> requestedIds =
                  OBJECT_MAPPER.readValue(
                      httpRequest.getBodyAsString(), new TypeReference<List<String>>() {});
              List<UserInfoDto> matched =
                  requestedIds.stream()
                      .map(userIdToInfo::get)
                      .filter(Objects::nonNull)
                      .collect(Collectors.toList());
              return response().withStatusCode(200).withBody(JsonBody.json(matched));
            });
  }

  private static Map<String, String> getDefaultCapabilities() {
    return Map.ofEntries(
        Map.entry(CarbonioAttribute.FEATURE_WSC_ENABLED.getValue(), "TRUE"),
        Map.entry(CarbonioAttribute.WSC_VIDEO_CALL_ENABLED.getValue(), "TRUE"),
        Map.entry(CarbonioAttribute.WSC_RECORDING_ENABLED.getValue(), "TRUE"),
        Map.entry(CarbonioAttribute.WSC_VIRTUAL_BACKGROUND_ENABLED.getValue(), "TRUE"),
        Map.entry(CarbonioAttribute.WSC_PRIVATE_CHAT_CREATION.getValue(), "TRUE"),
        Map.entry(CarbonioAttribute.WSC_GROUP_CHAT_CREATION.getValue(), "TRUE"),
        Map.entry(CarbonioAttribute.WSC_MAX_GROUP_MEMBERS.getValue(), "128"),
        Map.entry(CarbonioAttribute.WSC_ATTACHMENT_UPLOAD.getValue(), "TRUE"),
        Map.entry(CarbonioAttribute.WSC_MAX_ATTACHMENT_SIZE.getValue(), "128"),
        Map.entry(CarbonioAttribute.WSC_MAX_ROOM_PICTURE_SIZE.getValue(), "2"),
        Map.entry(CarbonioAttribute.WSC_MESSAGE_EDIT_TIME_LIMIT.getValue(), "10m"),
        Map.entry(CarbonioAttribute.WSC_MESSAGE_DELETE_TIME_LIMIT.getValue(), "10m"),
        Map.entry(CarbonioAttribute.WSC_SHOW_USERS_PRESENCE.getValue(), "TRUE"),
        Map.entry(CarbonioAttribute.WSC_SHOW_MESSAGE_READS.getValue(), "TRUE"));
  }

  private static String deriveDomain(MockUserProfile account) {
    if (account.getDomain() != null) {
      return account.getDomain();
    }
    if (account.getEmail() != null && account.getEmail().contains("@")) {
      return account.getEmail().substring(account.getEmail().indexOf('@') + 1);
    }
    return "";
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping User Management mock...");
    super.close();
  }
}
