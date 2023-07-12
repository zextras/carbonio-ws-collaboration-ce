// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockUserProfile;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.it.utils.MockedFiles;
import com.zextras.carbonio.chats.it.utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.utils.MockedFiles.MockedFileType;
import com.zextras.carbonio.chats.model.CapabilitiesDto;
import com.zextras.carbonio.chats.model.UserDto;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.entities.UserInfo;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.verify.VerificationTimes;

@ApiIntegrationTest
public class UsersApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final UserManagementMockServer  userManagementMockServer;
  private final StorageMockServer         storageMockServer;
  private final ObjectMapper              objectMapper;
  private final IntegrationTestUtils      integrationTestUtils;
  private final AppClock                  clock;
  private final AppConfig                 appConfig;

  public UsersApiIT(
    ResteasyRequestDispatcher dispatcher,
    UserManagementMockServer userManagementMockServer,
    StorageMockServer storageMockServer, ObjectMapper objectMapper,
    IntegrationTestUtils integrationTestUtils,
    Clock clock,
    AppConfig appConfig
  ) {
    this.dispatcher = dispatcher;
    this.userManagementMockServer = userManagementMockServer;
    this.storageMockServer = storageMockServer;
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
    public void getUser_testOK() throws Exception {
      UUID userId = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a723");
      OffsetDateTime ofdt = OffsetDateTime.now();
      clock.fixTimeAt(ofdt.toInstant());
      integrationTestUtils.generateAndSaveUser(userId, "hello", ofdt);

      MockHttpResponse mockHttpResponse = dispatcher.get(url(userId), "6g2R31FDn9epUpbyLhZSltqACqd33K9qa0b3lsJL");
      assertEquals(200, mockHttpResponse.getStatus());
      UserDto user = objectMapper.readValue(mockHttpResponse.getContentAsString(), new TypeReference<>() {
      });
      assertEquals("332a9527-3388-4207-be77-6d7e2978a723", user.getId().toString());
      assertEquals("snoopy@peanuts.com", user.getEmail());
      assertEquals("Snoopy", user.getName());
      assertEquals("hello", user.getStatusMessage());
      assertEquals(clock.instant().toEpochMilli(), user.getPictureUpdatedAt().toInstant().toEpochMilli());
    }

    @Test
    @DisplayName("Returns the requested user")
    public void getUser_testUserNotFound() throws Exception {
      UUID userId = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a722");
      clock.fixTimeAt(OffsetDateTime.now().toInstant());

      MockHttpResponse mockHttpResponse = dispatcher.get(url(userId), "6g2R31FDn9epUpbyLhZSltqACqd33K9qa0b3lsJL");
      assertEquals(404, mockHttpResponse.getStatus());
    }
  }

  @Nested
  @DisplayName("Get users tests")
  class GetUsersTests {

    private String url(List<String> userIds) {
      return "/users?" + userIds.stream().map(id -> String.join("=", "userIds", id))
        .collect(Collectors.joining("&"));
    }

    @Test
    @DisplayName("Returns the requested users")
    public void getUsers_testOK() throws Exception {
      List<String> userIds = Arrays.asList(
        "332a9527-3388-4207-be77-6d7e2978a723",
        "82735f6d-4c6c-471e-99d9-4eef91b1ec45",
        "ea7b9b61-bef5-4cf4-80cb-19612c42593a"
      );
      integrationTestUtils.generateAndSaveUser(UUID.fromString(userIds.get(0)), "status message 1",
        OffsetDateTime.parse("0001-01-01T00:00:00Z"));
      UserInfo user1 = new UserInfo(new UserId("332a9527-3388-4207-be77-6d7e2978a723"), "snoopy@peanuts.com", "Snoopy",
        "peanuts.com");
      UserInfo user2 = new UserInfo(new UserId("82735f6d-4c6c-471e-99d9-4eef91b1ec45"), "charlie.brown@peanuts.com",
        "Charlie Brown", "peanuts.com");
      UserInfo user3 = new UserInfo(new UserId("ea7b9b61-bef5-4cf4-80cb-19612c42593a"), "lucy.van.pelt@peanuts.com",
        "Lucy van Pelt", "peanuts.com");
      userManagementMockServer.mockUsersBulk(userIds, List.of(user1, user2, user3), true);

      MockHttpResponse mockHttpResponse = dispatcher.get(url(userIds), "F2TkzabOK2pu91sL951ofbJ7Ur3zcJKV9gBwdB84");
      assertEquals(200, mockHttpResponse.getStatus());
      List<UserDto> users = objectMapper.readValue(mockHttpResponse.getContentAsString(), new TypeReference<>() {
      });
      assertEquals("332a9527-3388-4207-be77-6d7e2978a723", users.get(0).getId().toString());
      assertEquals("snoopy@peanuts.com", users.get(0).getEmail());
      assertEquals("Snoopy", users.get(0).getName());
      assertEquals("status message 1", users.get(0).getStatusMessage());
      assertEquals(OffsetDateTime.parse("0001-01-01T00:00:00Z"), users.get(0).getPictureUpdatedAt());
      assertEquals("82735f6d-4c6c-471e-99d9-4eef91b1ec45", users.get(1).getId().toString());
      assertEquals("charlie.brown@peanuts.com", users.get(1).getEmail());
      assertEquals("Charlie Brown", users.get(1).getName());
      assertEquals("ea7b9b61-bef5-4cf4-80cb-19612c42593a", users.get(2).getId().toString());
      assertEquals("lucy.van.pelt@peanuts.com", users.get(2).getEmail());
      assertEquals("Lucy van Pelt", users.get(2).getName());
    }

    @Test
    @DisplayName("Returns parts of the requested users")
    public void getUser_testPartiallyOK() throws Exception {
      List<String> userIds = Arrays.asList(
        "332a9527-3388-4207-be77-6d7e2978a723",
        "82735f6d-4c6c-471e-99d9-4eef91b1ec45",
        "ea7b9b61-bef5-4cf4-80cb-19612c42593a"
      );
      integrationTestUtils.generateAndSaveUser(UUID.fromString(userIds.get(0)), "status message 1",
        OffsetDateTime.parse("0001-01-01T00:00:00Z"));
      UserInfo user1 = new UserInfo(new UserId("332a9527-3388-4207-be77-6d7e2978a723"), "snoopy@peanuts.com", "Snoopy",
        "peanuts.com");
      UserInfo user2 = new UserInfo(new UserId("82735f6d-4c6c-471e-99d9-4eef91b1ec45"), "charlie.brown@peanuts.com",
        "Charlie Brown", "peanuts.com");
      userManagementMockServer.mockUsersBulk(userIds, List.of(user1, user2), true);

      MockHttpResponse mockHttpResponse = dispatcher.get(url(userIds), "F2TkzabOK2pu91sL951ofbJ7Ur3zcJKV9gBwdB84");
      assertEquals(200, mockHttpResponse.getStatus());
      List<UserDto> users = objectMapper.readValue(mockHttpResponse.getContentAsString(), new TypeReference<>() {
      });
      assertEquals("332a9527-3388-4207-be77-6d7e2978a723", users.get(0).getId().toString());
      assertEquals("snoopy@peanuts.com", users.get(0).getEmail());
      assertEquals("Snoopy", users.get(0).getName());
      assertEquals("status message 1", users.get(0).getStatusMessage());
      assertEquals(OffsetDateTime.parse("0001-01-01T00:00:00Z"), users.get(0).getPictureUpdatedAt());
      assertEquals("82735f6d-4c6c-471e-99d9-4eef91b1ec45", users.get(1).getId().toString());
      assertEquals("charlie.brown@peanuts.com", users.get(1).getEmail());
      assertEquals("Charlie Brown", users.get(1).getName());
    }

    @Test
    @DisplayName("Returns empty list")
    public void getUser_testUserNotFound() throws Exception {
      List<String> userIds = Collections.singletonList("332a9527-3388-4207-be77-6d7e2978a722");

      MockHttpResponse mockHttpResponse = dispatcher.get(url(userIds), "6g2R31FDn9epUpbyLhZSltqACqd33K9qa0b3lsJL");
      userManagementMockServer.mockUsersBulk(userIds, Collections.emptyList(), true);
      List<UserDto> users = objectMapper.readValue(mockHttpResponse.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(200, mockHttpResponse.getStatus());
      assertTrue(users.isEmpty());
    }
  }

  @Nested
  @DisplayName("Gets user picture tests")
  public class GetsUserPictureTests {

    private String url(UUID userId) {
      return String.format("/users/%s/picture", userId);
    }

    @Test
    @DisplayName("Given a user identifier, correctly returns the user picture")
    public void getUserPicture_testOk() throws Exception {
      MockUserProfile account = MockedAccount.getAccount(MockedAccountType.SNOOPY);
      FileMock fileMock = MockedFiles.get(MockedFileType.SNOOPY_IMAGE);

      integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.USER_AVATAR, account.getUUID(), null);
      storageMockServer.mockNSLookupUrl(account.getId(), true);
      storageMockServer.mockDownload(account.getId(), account.getId(), fileMock, true);

      MockHttpResponse response = dispatcher.get(url(account.getUUID()), account.getToken());
      assertEquals(200, response.getStatus());

      assertArrayEquals(fileMock.getFileBytes(), response.getOutput());
      assertEquals(
        String.format("inline; filename=\"%s\"", fileMock.getName()),
        response.getOutputHeaders().get("Content-Disposition").get(0));
      assertEquals(fileMock.getMimeType(), response.getOutputHeaders().get("Content-Type").get(0).toString());
      assertEquals(fileMock.getSize(), response.getOutputHeaders().get("Content-Length").get(0));

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", account.getToken()), 1);
      storageMockServer.verify(storageMockServer.getNSLookupUrlRequest(account.getId()),
        VerificationTimes.exactly(1));
      storageMockServer.verify(storageMockServer.getDownloadRequest(account.getId(), account.getId()),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Given a user identifier, if the user is not authenticated returns status code 401")
    public void getUserPicture_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID()), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a user identifier, if the user hasn't the picture returns status code 404")
    public void getUserPicture_testErrorUserHasNoPicture() throws Exception {
      MockUserProfile account = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT);
      MockHttpResponse response = dispatcher.get(url(account.getUUID()), account.getToken());
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", account.getToken()), 1);
    }

    @Test
    @DisplayName("Given a user identifier, if the storage hasn't the picture file returns status code 424")
    public void getUserPicture_testErrorStorageHasNoPictureFile() throws Exception {
      MockUserProfile account = MockedAccount.getAccount(MockedAccountType.LINUS_VAN_PELT);
      integrationTestUtils.generateAndSaveFileMetadata(account.getUUID(), FileMetadataType.USER_AVATAR,
        account.getUUID(), null);
      MockHttpResponse response = dispatcher.get(url(account.getUUID()), account.getToken());
      assertEquals(424, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", account.getToken()), 1);
    }
  }

  @Nested
  @DisplayName("Update user picture tests")
  public class UpdateUserPictureTests {

    private String url(UUID userId) {
      return String.format("/users/%s/picture", userId);
    }

    @Test
    @DisplayName("Given a user identifier and a picture file, correctly updates the user pictures")
    public void updateUserPicture_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.SNOOPY_IMAGE);
      MockUserProfile account = MockedAccount.getAccount(MockedAccountType.SNOOPY);
      clock.fixTimeAt(Instant.parse("2022-01-01T00:00:00Z"));

      storageMockServer.mockNSLookupUrl(account.getId(), true);
      storageMockServer.mockUploadPut(account.getId(), account.getId(), true);
      byte[] fileBytes = fileMock.getFileBytes();

      MockHttpResponse response = dispatcher.put(url(account.getUUID()), fileBytes,
        Map.of(
          "Content-Type",
          "application/octet-stream",
          "fileName", "\\u0073\\u006e\\u006f\\u006f\\u0070\\u0079\\u002e\\u006a\\u0070\\u0067",
          "mimeType", fileMock.getMimeType()
        ),
        account.getToken()
      );
      clock.fixTimeAt(null);

      assertEquals(204, response.getStatus());
      assertEquals(0, response.getContentAsString().length());
      Optional<User> user = integrationTestUtils.getUserById(account.getUUID());
      assertTrue(user.isPresent());
      assertEquals(OffsetDateTime.ofInstant(Instant.parse("2022-01-01T00:00:00Z"), ZoneOffset.systemDefault()),
        user.get().getPictureUpdatedAt());
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", account.getToken()), 1);
      storageMockServer.verify(storageMockServer.getNSLookupUrlRequest(account.getId()), VerificationTimes.exactly(1));
      storageMockServer.verify(storageMockServer.getUploadPutRequest(account.getId(), account.getId()),
        VerificationTimes.exactly(1));
      //TODO: 01/03/22 verify event dispatcher iterations
    }

    @Test
    @DisplayName("Given a user identifier and a picture file, if user is not authenticated returns status code 401")
    public void updateUserPicture_testErrorUnauthenticatedUser() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      MockHttpResponse response = dispatcher.put(url(UUID.randomUUID()), fileMock.getFileBytes(),
        Map.of(
          "Content-Type",
          "application/octet-stream",
          "fileName", Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
          "mimeType", fileMock.getMimeType()
        ),
        null
      );
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a user identifier and a picture file, if X-Content-Disposition is missing returns status code 400")
    public void updateUserPicture_testErrorMissingXContentDisposition() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      MockUserProfile account = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN);
      MockHttpResponse response = dispatcher.put(url(account.getUUID()), fileMock.getFileBytes(),
        Collections.singletonMap("Content-Type", "application/octet-stream"), account.getToken());
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", account.getToken()), 1);
    }

    @Test
    @DisplayName("Given a user identifier and a picture file, if the identifier is not that of the authenticated user "
      + "returns status code 403")
    public void updateUserPicture_testErrorUserIsNotTheAuthenticatedUser() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      MockUserProfile account1 = MockedAccount.getAccount(MockedAccountType.SNOOPY);
      MockUserProfile account2 = MockedAccount.getAccount(MockedAccountType.PEPERITA_PATTY);

      MockHttpResponse response = dispatcher.put(url(account1.getUUID()), fileMock.getFileBytes(),
        Map.of(
          "Content-Type",
          "application/octet-stream",
          "fileName", "\\u0070\\u0065\\u0061\\u006e\\u0075\\u0074\\u0073\\u002e\\u006a\\u0070\\u0067",
          "mimeType", fileMock.getMimeType()
        ),
        account2.getToken()
      );
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", account2.getToken()), 1);
    }

    @Test
    @DisplayName("Given a ruser identifier and a picture file, if the image is too large returns status code 400")
    public void updateUserPicture_testErrorImageTooLarge() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.CHARLIE_BROWN_LARGE_IMAGE);
      MockUserProfile account = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN);
      MockHttpResponse response = dispatcher.put(url(account.getUUID()), fileMock.getFileBytes(),
        Map.of(
          "Content-Type",
          "application/octet-stream",
          "fileName", Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
          "mimeType", fileMock.getMimeType()
        ),
        account.getToken()
      );
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", account.getToken()), 1);
    }

    @Test
    @DisplayName("Given a user identifier and a picture file, if the file isn't an image returns status code 400")
    public void updateUserPicture_testErrorFileIsNotImage() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.LUCY_VAN_PELT_PDF);
      MockUserProfile account = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT);

      MockHttpResponse response = dispatcher.put(url(account.getUUID()), fileMock.getFileBytes(),
        Map.of(
          "Content-Type",
          "application/octet-stream",
          "fileName", Base64.getEncoder().encodeToString(fileMock.getName().getBytes()),
          "mimeType", fileMock.getMimeType()
        ),
        account.getToken()
      );
      assertEquals(400, response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", account.getToken()), 1);
    }
  }

  @Nested
  @DisplayName("Delete user picture tests")
  public class DeleteUserPictureTests {

    private String url(UUID userId) {
      return String.format("/users/%s/picture", userId);
    }

    @Test
    @DisplayName("Correctly deletes the user picture")
    public void deleteUserPicture_testOk() throws Exception {
      MockUserProfile account = MockedAccount.getAccount(MockedAccountType.SNOOPY);
      FileMock fileMock = MockedFiles.get(MockedFileType.SNOOPY_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.USER_AVATAR, account.getUUID(), null);
      integrationTestUtils.generateAndSaveUser(account.getUUID(), "hello",
        OffsetDateTime.ofInstant(clock.instant(), clock.getZone()));
      storageMockServer.mockNSLookupUrl(account.getId(), true);
      storageMockServer.mockDelete(account.getId(), account.getId(), true);

      MockHttpResponse response = dispatcher.delete(url(account.getUUID()), account.getToken());
      assertEquals(204, response.getStatus());
      Optional<FileMetadata> metadata = integrationTestUtils.getFileMetadataById(fileMock.getUUID());
      assertTrue(metadata.isEmpty());
      Optional<User> user = integrationTestUtils.getUserById(account.getUUID());
      assertTrue(user.isPresent());
      assertNull(user.get().getPictureUpdatedAt());

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", account.getToken()), 1);
      storageMockServer.verify(storageMockServer.getNSLookupUrlRequest(account.getId()), VerificationTimes.exactly(1));
      storageMockServer.verify(storageMockServer.getDeleteRequest(account.getId(), account.getId()),
        VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("If the user is not authenticated, it returns status code 401")
    public void deleteUserPicture_unauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID()), null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If user is not the picture owner, it return a status code 403")
    public void deleteUserPicture_userNotPictureOwner() throws Exception {
      MockUserProfile snoopy = MockedAccount.getAccount(MockedAccountType.SNOOPY);
      MockUserProfile charlie = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN);

      MockHttpResponse response = dispatcher.delete(url(charlie.getUUID()), snoopy.getToken());
      assertEquals(403, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("If the user hasn't its picture, it returns a status code 404")
    public void deleteUserPicture_fileNotFound() throws Exception {
      MockUserProfile account = MockedAccount.getAccount(MockedAccountType.SCHROEDER);
      MockHttpResponse response = dispatcher.delete(url(account.getUUID()), account.getToken());
      assertEquals(404, response.getStatus());
      assertEquals(0, response.getOutput().length);

      userManagementMockServer.verify("GET", String.format("/auth/token/%s", account.getToken()), 1);
    }
  }

  @Nested
  @DisplayName("Gets user capabilities")
  public class GetsUserCapabilities {

    private static final String URL = "/users/capabilities";

    @Test
    @DisplayName("Returns default user capabilities")
    public void getCapabilities_defaultValuesTestOk() throws Exception {
      MockUserProfile account = MockedAccount.getAccount(MockedAccountType.SNOOPY);
      MockHttpResponse response = dispatcher.get(URL, account.getToken());
      assertEquals(200, response.getStatus());

      CapabilitiesDto capabilities = objectMapper.readValue(response.getContentAsString(), CapabilitiesDto.class);
      assertEquals(false, capabilities.isCanVideoCall());
      assertEquals(false, capabilities.isCanVideoCallRecord());
      assertEquals(false, capabilities.isCanUseVirtualBackground());
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
    public void getCapabilities_configuredValuesTestOk() throws Exception {
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

      CapabilitiesDto capabilities = objectMapper.readValue(response.getContentAsString(), CapabilitiesDto.class);
      assertEquals(false, capabilities.isCanVideoCall());
      assertEquals(false, capabilities.isCanVideoCallRecord());
      assertEquals(false, capabilities.isCanUseVirtualBackground());
      assertEquals(false, capabilities.isCanSeeMessageReads());
      assertEquals(false, capabilities.isCanSeeUsersPresence());
      assertEquals(15, capabilities.getEditMessageTimeLimitInMinutes());
      assertEquals(15, capabilities.getDeleteMessageTimeLimitInMinutes());
      assertEquals(20, capabilities.getMaxGroupMembers());
      assertEquals(512, capabilities.getMaxRoomImageSizeInKb());
      assertEquals(512, capabilities.getMaxUserImageSizeInKb());

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
    public void getCapabilities_unauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(URL, null);
      assertEquals(401, response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }
}
