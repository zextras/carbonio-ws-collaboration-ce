// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockUserProfile;
import com.zextras.carbonio.user_management.sdk.grpc.GetUserByIdRequest;
import com.zextras.carbonio.user_management.sdk.grpc.GetUserMyselfRequest;
import com.zextras.carbonio.user_management.sdk.grpc.GetUsersRequest;
import com.zextras.carbonio.user_management.sdk.grpc.GetUsersResponse;
import com.zextras.carbonio.user_management.sdk.grpc.UserInfoProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserInfoResponse;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfResponse;
import com.zextras.carbonio.user_management.sdk.grpc.UserTypeProto;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

public class UserManagementMockServer implements CloseableResource {

  public static final String SERVER_NAME = "user-management-mock-server";

  private final Server server;
  private final MockUserManagementService service;

  public UserManagementMockServer() {
    this.service = new MockUserManagementService();
    registerMockedAccounts();
    try {
      this.server =
          InProcessServerBuilder.forName(SERVER_NAME)
              .directExecutor()
              .addService(service)
              .build()
              .start();
    } catch (IOException e) {
      throw new RuntimeException("Failed to start User Management gRPC in-process server", e);
    }
  }

  private void registerMockedAccounts() {
    for (MockUserProfile account : MockedAccount.getAccounts()) {
      UserTypeProto typeProto =
          switch (account.getType()) {
            case GUEST -> UserTypeProto.GUEST;
            default -> UserTypeProto.INTERNAL;
          };

      UserInfoProto userInfo =
          UserInfoProto.newBuilder()
              .setUserId(account.getId())
              .setEmail(account.getEmail())
              .setFullName(account.getName())
              .setDomain(deriveDomain(account))
              .setStatus("active")
              .setType(typeProto)
              .build();

      UserMyselfProto userMyself =
          UserMyselfProto.newBuilder()
              .setInfo(userInfo)
              .setLocale("en")
              .addFeatures("carbonioFeatureWscEnabled")
              .putAllCapabilities(getDefaultCapabilities())
              .build();

      service.registerToken(account.getToken(), userMyself);
      service.registerUserById(account.getId(), userInfo);
    }
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

  /**
   * Returns the mock service implementation, allowing tests to register additional dynamic
   * responses (e.g., for bulk user lookups).
   */
  public MockUserManagementService getService() {
    return service;
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping User Management gRPC mock...");
    server.shutdownNow();
  }

  public static class MockUserManagementService
      extends UserManagementServiceGrpc.UserManagementServiceImplBase {

    private final Map<String, UserMyselfProto> tokenToUserMyself = new HashMap<>();
    private final Map<String, UserInfoProto> userIdToInfo = new HashMap<>();

    public void registerToken(String token, UserMyselfProto userMyself) {
      tokenToUserMyself.put(token, userMyself);
    }

    public void registerUserById(String userId, UserInfoProto userInfo) {
      userIdToInfo.put(userId, userInfo);
    }

    @Override
    public void getUserMyself(
        GetUserMyselfRequest request, StreamObserver<UserMyselfResponse> responseObserver) {
      String token = request.getToken();
      UserMyselfProto userMyself = tokenToUserMyself.get(token);
      if (userMyself == null) {
        responseObserver.onError(
            Status.UNAUTHENTICATED.withDescription("Invalid token").asRuntimeException());
        return;
      }
      responseObserver.onNext(UserMyselfResponse.newBuilder().setUser(userMyself).build());
      responseObserver.onCompleted();
    }

    @Override
    public void getUserById(
        GetUserByIdRequest request, StreamObserver<UserInfoResponse> responseObserver) {
      String userId = request.getUserId();
      UserInfoProto userInfo = userIdToInfo.get(userId);
      if (userInfo == null) {
        responseObserver.onError(
            Status.NOT_FOUND.withDescription("User not found").asRuntimeException());
        return;
      }
      responseObserver.onNext(UserInfoResponse.newBuilder().setUser(userInfo).build());
      responseObserver.onCompleted();
    }

    @Override
    public void getUsers(
        GetUsersRequest request, StreamObserver<GetUsersResponse> responseObserver) {
      GetUsersResponse.Builder responseBuilder = GetUsersResponse.newBuilder();
      for (String userId : request.getUserIdsList()) {
        UserInfoProto userInfo = userIdToInfo.get(userId);
        if (userInfo != null) {
          responseBuilder.addUsers(userInfo);
        }
      }
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    }
  }
}
