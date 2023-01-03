// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.AttachmentsApi;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedFiles;
import com.zextras.carbonio.chats.it.utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.utils.MockedFiles.MockedFileType;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.PreviewerMockServer;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
public class AttachmentsApiIT {

  private final FileMetadataRepository    fileMetadataRepository;
  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper              objectMapper;
  private final IntegrationTestUtils      integrationTestUtils;
  private final StorageMockServer         storageMockServer;
  private final PreviewerMockServer       previewerMockServer;
  private final UserManagementMockServer  userManagementMockServer;

  public AttachmentsApiIT(
    AttachmentsApi attachmentsApi, FileMetadataRepository fileMetadataRepository,
    ObjectMapper objectMapper, ResteasyRequestDispatcher dispatcher,
    IntegrationTestUtils integrationTestUtils,
    StorageMockServer storageMockServer,
    PreviewerMockServer previewerMockServer,
    UserManagementMockServer userManagementMockServer
  ) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.objectMapper = objectMapper;
    this.dispatcher = dispatcher;
    this.integrationTestUtils = integrationTestUtils;
    this.storageMockServer = storageMockServer;
    this.previewerMockServer = previewerMockServer;
    this.userManagementMockServer = userManagementMockServer;
    this.dispatcher.getRegistry().addSingletonResource(attachmentsApi);
  }

  private static UUID   user1Id;
  private static UUID   user2Id;
  private static String user1Token;
  private static String user2Token;
  private static String user3Token;
  private static UUID   roomId;

  @BeforeAll
  public static void initAll() {
    user1Id = MockedAccount.getAccounts().get(0).getUUID();
    user1Token = MockedAccount.getAccounts().get(0).getToken();
    user2Id = MockedAccount.getAccounts().get(1).getUUID();
    user2Token = MockedAccount.getAccounts().get(1).getToken();
    user3Token = MockedAccount.getAccounts().get(2).getToken();
    roomId = UUID.randomUUID();
  }

  @BeforeEach
  public void init() {
    integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
  }

  @AfterEach
  public void afterEach() {
    previewerMockServer.setIsAliveResponse(true);
    storageMockServer.setIsAliveResponse(true);
  }

  @Nested
  @DisplayName("Gets attachment test")
  public class GetsAttachmentTests {

    private String url(String attachmentId) {
      return String.format("/attachments/%s/download", attachmentId);
    }

    @Test
    @DisplayName("Correctly returns the attachment file for requested id")
    public void getAttachment_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.SNOOPY_IMAGE);
      FileMetadata fileMetadata = fileMetadataRepository.save(
        integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId));

      MockHttpResponse response = dispatcher.get(url(fileMock.getId()), user1Token);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertArrayEquals(fileMock.getFileBytes(), response.getOutput());
      assertEquals(
        String.format("inline; filename=\"%s\"", fileMock.getName()),
        response.getOutputHeaders().get("Content-Disposition").get(0));
      assertEquals(fileMock.getMimeType(), response.getOutputHeaders().get("Content-Type").get(0).toString());
      assertEquals(fileMock.getSize(), response.getOutputHeaders().get("Content-Length").get(0));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      storageMockServer.verify("GET", "/download", fileMetadata.getId(), 1);
    }

    @Test
    @DisplayName("Returns 424 if the Storage server is down")
    public void getAttachment_testExceptionStorageServerKO() throws Exception {
      storageMockServer.setIsAliveResponse(false);

      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID().toString()), null);

      assertEquals(424, response.getStatus());
    }

    @Test
    @DisplayName("Given an attachment identifier, if the user is not authenticated return a status code 401")
    public void getAttachment_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID().toString()), null);

      assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given an attachment identifier, if authenticated user isn't a room member then return a status code 403")
    public void getAttachment_testErrorUserIsNotARoomMember() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

      MockHttpResponse response = dispatcher.get(url(fileMock.getId()),
        user3Token);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Given an attachment identifier, if the identifier is not an UUID return a status code 404")
    public void getAttachment_testErrorIdentifierIsNotUUID() throws Exception {
      MockHttpResponse response = dispatcher.get(url("not_a_uuid"), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given an attachment identifier, if the attachment doesn't exist then return a status code 404")
    public void getAttachment_testErrorFileNotExists() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID().toString()), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }

  @Nested
  @DisplayName("Gets attachment preview tests")
  public class GetsAttachmentPreviewTests {

    private String url(String attachmentId) {
      return String.format("/attachments/%s/preview", attachmentId);
    }

    @Test
    @DisplayName("Correctly returns the attachment preview for requested id")
    public void getAttachmentPreview_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.SNOOPY_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

      MockHttpResponse response = dispatcher.get(url(fileMock.getId()), user1Token);

      FileMock expectedFile = MockedFiles.getPreview(MockedFileType.SNOOPY_PREVIEW);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertArrayEquals(expectedFile.getFileBytes(), response.getOutput());
      assertEquals("image/jpeg", response.getOutputHeaders().get("Content-Type").get(0).toString());
      assertEquals(expectedFile.getSize(), response.getOutputHeaders().get("Content-Length").get(0));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      previewerMockServer.verify("GET",
        String.format("/preview/image/%s/1/320x160/", fileMock.getId()),
        Map.of("service_type", "chats"), 1);
    }

    @Test
    @DisplayName("Returns 424 if the Previewer server is down")
    public void getAttachmentPreview_testExceptionPreviewerKO() throws Exception {
      previewerMockServer.setIsAliveResponse(false);

      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID().toString()), null);

      assertEquals(424, response.getStatus());
    }

    @Test
    @DisplayName("Given an attachment identifier, if the user is not authenticated return a status code 401")
    public void getAttachmentPreview_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID().toString()), null);

      assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given an attachment identifier, if authenticated user isn't a room member then return a status code 403")
    public void getAttachmentPreview_testErrorUserIsNotARoomMember() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

      MockHttpResponse response = dispatcher.get(url(fileMock.getId()), user3Token);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Given an attachment identifier, if the identifier is not an UUID return a status code 404")
    public void getAttachmentPreview_testErrorIdentifierIsNotUUID() throws Exception {
      MockHttpResponse response = dispatcher.get(url("not_a_uuid"), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given an attachment identifier, if the attachment doesn't exist then return a status code 404")
    public void getAttachmentPreview_testErrorFileNotExists() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID().toString()), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }

  @Nested
  @DisplayName("Gets attachment information tests")
  public class GetsAttachmentInformationTests {

    private String url(String attachmentId) {
      return String.format("/attachments/%s", attachmentId);
    }

    @Test
    @DisplayName("Correctly returns the attachment information for requested id")
    public void getAttachmentInfo_testOk() throws Exception {
      FileMetadata savedMetadata = integrationTestUtils.generateAndSaveFileMetadata(UUID.randomUUID(),
        FileMetadataType.ATTACHMENT, user1Id, roomId);

      MockHttpResponse response = dispatcher.get(url(savedMetadata.getId()),
        user1Token);
      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      AttachmentDto attachment = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(savedMetadata.getId(), attachment.getId().toString());
      assertEquals(roomId, attachment.getRoomId());
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given an attachment identifier, if the user is not authenticated return a status code 401")
    public void getAttachmentInfo_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID().toString()), null);

      assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given an attachment identifier, if authenticated user isn't a room member then return a status code 403")
    public void getAttachmentInfo_testErrorUserIsNotARoomMember() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

      MockHttpResponse response = dispatcher.get(url(fileMock.getId()), user3Token);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Given an attachment identifier, if the identifier is not an UUID return a status code 404")
    public void getAttachmentInfo_testErrorIdentifierIsNotUUID() throws Exception {
      MockHttpResponse response = dispatcher.get(url("not_a_uuid"), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given an attachment identifier, if the attachment doesn't exist then return a status code 404")
    public void getAttachmentInfo_testErrorFileNotExists() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID().toString()), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }

  @Nested
  @DisplayName("Deletes attachment tests")
  public class DeletesAttachmentTests {

    private String url(String attachmentId) {
      return String.format("/attachments/%s", attachmentId);
    }

    @Test
    @DisplayName("Correctly deletes the attachment with requested id by its owner")
    public void deleteAttachment_testOkByAttachmentOwner() throws Exception {
      FileMetadata fileMetadata = integrationTestUtils.generateAndSaveFileMetadata(
        MockedFiles.get(MockedFileType.PEANUTS_IMAGE),
        FileMetadataType.ATTACHMENT, user2Id, roomId);

      MockHttpResponse response = dispatcher.delete(url(fileMetadata.getId()),
        user2Token);
      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      assertTrue(fileMetadataRepository.getById(fileMetadata.getId()).isEmpty());
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user2Token), 1);
      storageMockServer.verify("DELETE", "/delete", fileMetadata.getId(), 1);
    }

    @Test
    @DisplayName("Correctly deletes the attachment with requested id by room owner")
    public void deleteAttachment_testOkByRoomOwner() throws Exception {
      FileMetadata fileMetadata = integrationTestUtils.generateAndSaveFileMetadata(
        MockedFiles.get(MockedFileType.PEANUTS_IMAGE),
        FileMetadataType.ATTACHMENT, user2Id, roomId);

      MockHttpResponse response = dispatcher.delete(url(fileMetadata.getId()),
        user1Token);
      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      assertTrue(fileMetadataRepository.getById(fileMetadata.getId()).isEmpty());
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      storageMockServer.verify("DELETE", "/delete", fileMetadata.getId(), 1);
    }

    @Test
    @DisplayName("Given an attachment identifier, if the user is not authenticated return a status code 401")
    public void deleteAttachment_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID().toString()),
        null);

      assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given an attachment identifier, if authenticated user isn't a room member then return a status code 403")
    public void deleteAttachment_testErrorUserIsNotARoomMember() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

      MockHttpResponse response = dispatcher.delete(url(fileMock.getId()), user3Token);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

    @Test
    @DisplayName("Given an attachment identifier, if authenticated user isn't the attachment owner then return a status code 403")
    public void deleteAttachment_testErrorUserIsNotAnAttachmentOwner() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

      MockHttpResponse response = dispatcher.delete(url(fileMock.getId()), user2Token);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user2Token), 1);
    }

    @Test
    @DisplayName("Given an attachment identifier, if the identifier is not an UUID return a status code 404")
    public void deleteAttachment_testErrorIdentifierIsNotUUID() throws Exception {
      MockHttpResponse response = dispatcher.delete(url("not_a_uuid"), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given an attachment identifier, if the attachment doesn't exist then return a status code 404")
    public void deleteAttachment_testErrorFileNotExists() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID().toString()), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }
  }
}