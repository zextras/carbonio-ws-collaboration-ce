// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockUserProfile;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.CapabilitiesDto;
import com.zextras.carbonio.chats.model.UserDto;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.entities.UserInfo;
import com.zextras.carbonio.usermanagement.enumerations.UserStatus;
import com.zextras.carbonio.usermanagement.enumerations.UserType;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class UsersApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final UserManagementMockServer userManagementMockServer;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final AppClock clock;
  private final AppConfig appConfig;

  public UsersApiIT(
      ResteasyRequestDispatcher dispatcher,
      UserManagementMockServer userManagementMockServer,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      Clock clock,
      AppConfig appConfig) {
    this.dispatcher = dispatcher;
    this.userManagementMockServer = userManagementMockServer;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.clock = (AppClock) clock;
    this.appConfig = appConfig;
  }

  @Nested
  @DisplayName("Get user tests")
  class GetUserTests {

    private String url(UUID userId) {
      return String.format("/users/%s", userId);
    }

    @Test
    @DisplayName("Returns the requested user")
    void getUser_testOK() throws Exception {
      UUID userId = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a723");
      integrationTestUtils.generateAndSaveUser(userId, "hello");

      MockHttpResponse mockHttpResponse =
          dispatcher.get(url(userId), "6g2R31FDn9epUpbyLhZSltqACqd33K9qa0b3lsJL");
      assertEquals(200, mockHttpResponse.getStatus());
      UserDto user =
          objectMapper.readValue(mockHttpResponse.getContentAsString(), new TypeReference<>() {});
      assertEquals("332a9527-3388-4207-be77-6d7e2978a723", user.getId().toString());
      assertEquals("snoopy@peanuts.com", user.getEmail());
      assertEquals("Snoopy", user.getName());
      assertEquals("hello", user.getStatusMessage());
    }

    @Test
    @DisplayName("Returns the requested user")
    void getUser_testUserNotFound() throws Exception {
      UUID userId = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a722");
      clock.fixTimeAt(OffsetDateTime.now().toInstant());

      MockHttpResponse mockHttpResponse =
          dispatcher.get(url(userId), "6g2R31FDn9epUpbyLhZSltqACqd33K9qa0b3lsJL");
      assertEquals(404, mockHttpResponse.getStatus());
    }
  }

  @Nested
  @DisplayName("Get users tests")
  class GetUsersTests {

    private String url(List<String> userIds) {
      return "/users?"
          + userIds.stream()
              .map(id -> String.join("=", "userIds", id))
              .collect(Collectors.joining("&"));
    }

    @Test
    @DisplayName("Returns the requested users")
    void getUsers_testOK() throws Exception {
      List<String> userIds =
          Arrays.asList(
              "332a9527-3388-4207-be77-6d7e2978a723",
              "82735f6d-4c6c-471e-99d9-4eef91b1ec45",
              "7156b7fa-78a8-47e3-8b50-102d1db31edc");
      integrationTestUtils.generateAndSaveUser(UUID.fromString(userIds.get(0)), "status message 1");
      UserInfo user1 =
          new UserInfo(
              new UserId("332a9527-3388-4207-be77-6d7e2978a723"),
              "snoopy@peanuts.com",
              "Snoopy",
              "peanuts.com",
              UserStatus.ACTIVE,
              UserType.INTERNAL);
      UserInfo user2 =
          new UserInfo(
              new UserId("82735f6d-4c6c-471e-99d9-4eef91b1ec45"),
              "charlie.brown@peanuts.com",
              "Charlie Brown",
              "peanuts.com",
              UserStatus.ACTIVE,
              UserType.INTERNAL);
      UserInfo user3 =
          new UserInfo(
              new UserId("ea7b9b61-bef5-4cf4-80cb-19612c42593a"),
              "lucy.van.pelt@peanuts.com",
              "Lucy van Pelt",
              "peanuts.com",
              UserStatus.ACTIVE,
              UserType.INTERNAL);
      userManagementMockServer.mockUsersBulk(userIds, List.of(user1, user2, user3), true);

      MockHttpResponse mockHttpResponse =
          dispatcher.get(url(userIds), "F2TkzabOK2pu91sL951ofbJ7Ur3zcJKV9gBwdB84");
      assertEquals(200, mockHttpResponse.getStatus());
      List<UserDto> users =
          objectMapper.readValue(mockHttpResponse.getContentAsString(), new TypeReference<>() {});
      assertEquals("332a9527-3388-4207-be77-6d7e2978a723", users.get(0).getId().toString());
      assertEquals("snoopy@peanuts.com", users.get(0).getEmail());
      assertEquals("Snoopy", users.get(0).getName());
      assertEquals("status message 1", users.get(0).getStatusMessage());
      assertEquals("82735f6d-4c6c-471e-99d9-4eef91b1ec45", users.get(1).getId().toString());
      assertEquals("charlie.brown@peanuts.com", users.get(1).getEmail());
      assertEquals("Charlie Brown", users.get(1).getName());
      assertEquals("ea7b9b61-bef5-4cf4-80cb-19612c42593a", users.get(2).getId().toString());
      assertEquals("lucy.van.pelt@peanuts.com", users.get(2).getEmail());
      assertEquals("Lucy van Pelt", users.get(2).getName());
    }

    @Test
    @DisplayName("Returns parts of the requested users")
    void getUser_testPartiallyOK() throws Exception {
      List<String> userIds =
          Arrays.asList(
              "332a9527-3388-4207-be77-6d7e2978a723",
              "82735f6d-4c6c-471e-99d9-4eef91b1ec45",
              "ea7b9b61-bef5-4cf4-80cb-19612c42593a");
      integrationTestUtils.generateAndSaveUser(UUID.fromString(userIds.get(0)), "status message 1");
      UserInfo user1 =
          new UserInfo(
              new UserId("332a9527-3388-4207-be77-6d7e2978a723"),
              "snoopy@peanuts.com",
              "Snoopy",
              "peanuts.com",
              UserStatus.ACTIVE,
              UserType.INTERNAL);
      UserInfo user2 =
          new UserInfo(
              new UserId("82735f6d-4c6c-471e-99d9-4eef91b1ec45"),
              "charlie.brown@peanuts.com",
              "Charlie Brown",
              "peanuts.com",
              UserStatus.ACTIVE,
              UserType.INTERNAL);
      userManagementMockServer.mockUsersBulk(userIds, List.of(user1, user2), true);

      MockHttpResponse mockHttpResponse =
          dispatcher.get(url(userIds), "F2TkzabOK2pu91sL951ofbJ7Ur3zcJKV9gBwdB84");
      assertEquals(200, mockHttpResponse.getStatus());
      List<UserDto> users =
          objectMapper.readValue(mockHttpResponse.getContentAsString(), new TypeReference<>() {});
      assertEquals("332a9527-3388-4207-be77-6d7e2978a723", users.get(0).getId().toString());
      assertEquals("snoopy@peanuts.com", users.get(0).getEmail());
      assertEquals("Snoopy", users.get(0).getName());
      assertEquals("status message 1", users.get(0).getStatusMessage());
      assertEquals("82735f6d-4c6c-471e-99d9-4eef91b1ec45", users.get(1).getId().toString());
      assertEquals("charlie.brown@peanuts.com", users.get(1).getEmail());
      assertEquals("Charlie Brown", users.get(1).getName());
    }

    @Test
    @DisplayName("Returns empty list")
    void getUser_testUserNotFound() throws Exception {
      List<String> userIds = Collections.singletonList("332a9527-3388-4207-be77-6d7e2978a722");

      MockHttpResponse mockHttpResponse =
          dispatcher.get(url(userIds), "6g2R31FDn9epUpbyLhZSltqACqd33K9qa0b3lsJL");
      userManagementMockServer.mockUsersBulk(userIds, Collections.emptyList(), true);
      List<UserDto> users =
          objectMapper.readValue(mockHttpResponse.getContentAsString(), new TypeReference<>() {});
      assertEquals(200, mockHttpResponse.getStatus());
      assertTrue(users.isEmpty());
    }
  }

  @Nested
  @DisplayName("Gets user capabilities")
  class GetsUserCapabilities {

    private static final String URL = "/users/capabilities";

    @Test
    @DisplayName("Returns default user capabilities")
    void getCapabilities_defaultValuesTestOk() throws Exception {
      MockUserProfile account = MockedAccount.getAccount(MockedAccountType.SNOOPY);
      MockHttpResponse response = dispatcher.get(URL, account.getToken());
      assertEquals(200, response.getStatus());

      CapabilitiesDto capabilities =
          objectMapper.readValue(response.getContentAsString(), CapabilitiesDto.class);
      assertEquals(true, capabilities.isCanVideoCall());
      assertEquals(true, capabilities.isCanUseVirtualBackground());
      assertEquals(true, capabilities.isCanSeeMessageReads());
      assertEquals(true, capabilities.isCanSeeUsersPresence());
      assertEquals(10, capabilities.getEditMessageTimeLimitInMinutes());
      assertEquals(10, capabilities.getDeleteMessageTimeLimitInMinutes());
      assertEquals(128, capabilities.getMaxGroupMembers());
      assertEquals(512, capabilities.getMaxRoomImageSizeInKb());
      assertEquals(512, capabilities.getMaxUserImageSizeInKb());
    }

    @Test
    @DisplayName("Returns configured user capabilities")
    void getCapabilities_configuredValuesTestOk() throws Exception {
      appConfig.set(ConfigName.CAN_VIDEO_CALL, "true");
      appConfig.set(ConfigName.CAN_USE_VIRTUAL_BACKGROUND, "false");
      appConfig.set(ConfigName.CAN_SEE_MESSAGE_READS, "false");
      appConfig.set(ConfigName.CAN_SEE_USERS_PRESENCE, "false");
      appConfig.set(ConfigName.MAX_USER_IMAGE_SIZE_IN_KB, "512");
      appConfig.set(ConfigName.MAX_ROOM_IMAGE_SIZE_IN_KB, "512");
      appConfig.set(ConfigName.EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES, "15");
      appConfig.set(ConfigName.DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES, "15");
      appConfig.set(ConfigName.MAX_GROUP_MEMBERS, "20");

      MockUserProfile account = MockedAccount.getAccount(MockedAccountType.SNOOPY);
      MockHttpResponse response = dispatcher.get(URL, account.getToken());
      assertEquals(200, response.getStatus());

      CapabilitiesDto capabilities =
          objectMapper.readValue(response.getContentAsString(), CapabilitiesDto.class);
      assertEquals(true, capabilities.isCanVideoCall());
      assertEquals(false, capabilities.isCanUseVirtualBackground());
      assertEquals(false, capabilities.isCanSeeMessageReads());
      assertEquals(false, capabilities.isCanSeeUsersPresence());
      assertEquals(15, capabilities.getEditMessageTimeLimitInMinutes());
      assertEquals(15, capabilities.getDeleteMessageTimeLimitInMinutes());
      assertEquals(20, capabilities.getMaxGroupMembers());
      assertEquals(512, capabilities.getMaxRoomImageSizeInKb());
      assertEquals(512, capabilities.getMaxUserImageSizeInKb());

      appConfig.set(ConfigName.CAN_VIDEO_CALL, null);
      appConfig.set(ConfigName.CAN_USE_VIRTUAL_BACKGROUND, null);
      appConfig.set(ConfigName.CAN_SEE_MESSAGE_READS, null);
      appConfig.set(ConfigName.CAN_SEE_USERS_PRESENCE, null);
      appConfig.set(ConfigName.MAX_USER_IMAGE_SIZE_IN_KB, null);
      appConfig.set(ConfigName.MAX_ROOM_IMAGE_SIZE_IN_KB, null);
      appConfig.set(ConfigName.EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES, null);
      appConfig.set(ConfigName.DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES, null);
      appConfig.set(ConfigName.MAX_GROUP_MEMBERS, null);
    }

    @Test
    @DisplayName("If the user is not authenticated, it returns status code 401")
    void getCapabilities_unauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(URL, null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }
}
