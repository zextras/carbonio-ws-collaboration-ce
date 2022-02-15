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
import com.zextras.carbonio.chats.it.Utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.Utils.MockedAccount;
import com.zextras.carbonio.chats.it.Utils.MockedFiles;
import com.zextras.carbonio.chats.it.Utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.annotations.IntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@IntegrationTest
public class AttachmentsApiIT {

  private final FileMetadataRepository    fileMetadataRepository;
  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper              objectMapper;
  private final IntegrationTestUtils      integrationTestUtils;
  private final StorageMockServer         storageMockServer;
  private final UserManagementMockServer  userManagementMockServer;

  public AttachmentsApiIT(
    AttachmentsApi attachmentsApi, FileMetadataRepository fileMetadataRepository,
    ObjectMapper objectMapper, ResteasyRequestDispatcher dispatcher,
    IntegrationTestUtils integrationTestUtils,
    StorageMockServer storageMockServer,
    UserManagementMockServer userManagementMockServer
  ) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.objectMapper = objectMapper;
    this.dispatcher = dispatcher;
    this.integrationTestUtils = integrationTestUtils;
    this.storageMockServer = storageMockServer;
    this.userManagementMockServer = userManagementMockServer;
    this.dispatcher.getRegistry().addSingletonResource(attachmentsApi);
  }

  private static UUID   user1Id;
  private static UUID   user2Id;
  private static String user1Token;
  private static String user3Token;
  private static UUID   roomId;

  @BeforeAll
  public static void initAll() {
    int i = new Random().nextInt(Integer.MAX_VALUE);
    user1Id = MockedAccount.getAccount(i).getUUID();
    user1Token = MockedAccount.getAccount(i).getToken();
    user2Id = MockedAccount.getAccount(i + 1).getUUID();
    user3Token = MockedAccount.getAccount(i + 2).getToken();
    roomId = UUID.randomUUID();
  }

  @BeforeEach
  public void init() {
    integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
  }

  @Test
  @DisplayName("Given an attachment identifier, if the identifier is not an UUID return a status code 404 for all attachment APIs")
  public void generic_testErrorIdentifierIsNotUUID() throws Exception {
    String url = "/attachments/not_a_uuid";
    MockHttpResponse response = dispatcher.get(String.join("", url, "/download"), user1Token);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);

    response = dispatcher.get(String.join("", url, "/preview"), user1Token);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);

    response = dispatcher.get(url, user1Token);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);

    response = dispatcher.delete(url, user1Token);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
  }

  @Test
  @DisplayName("Given an attachment identifier, if the attachment doesn't exist then return a status code 404 for all attachment APIs")
  public void generic_testErrorFileNotExists() throws Exception {
    String url = String.format("/attachments/%s", UUID.randomUUID());

    MockHttpResponse response = dispatcher.get(String.join("", url, "/download"), user1Token);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);

    response = dispatcher.get(String.join("", url, "/preview"), user1Token);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);

    response = dispatcher.get(url, user1Token);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);

    response = dispatcher.delete(url, user1Token);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
  }

  @Test
  @DisplayName("Correctly returns the attachment file for requested id")
  public void getAttachment_testOk() throws Exception {
    FileMock fileMock = MockedFiles.getImages();
    FileMetadata fileMetadata = fileMetadataRepository.save(
      integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId));

    MockHttpResponse response = dispatcher.get(String.format("/attachments/%s/download", fileMock.getId()), user1Token);

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
  @DisplayName("Given an attachment identifier, if the user is not authenticated return a status code 401")
  public void getAttachment_testErrorUnauthenticatedUser() throws Exception {
    MockHttpResponse response = dispatcher.get(String.format("/attachments/%s/download", UUID.randomUUID()), null);

    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
  }

  @Test
  @DisplayName("Given an attachment identifier, if authenticated user isn't a room member then return a status code 403")
  public void getAttachment_testErrorUserIsNotARoomMember() throws Exception {
    FileMock fileMock = MockedFiles.getImages();
    integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

    MockHttpResponse response = dispatcher.get(String.format("/attachments/%s/download", fileMock.getId()), user3Token);

    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
  }

  @Test
  @DisplayName("Correctly returns the attachment preview for requested id")
  public void getAttachmentPreview_testOk() throws Exception {
    FileMock fileMock = MockedFiles.getImages();
    FileMetadata fileMetadata = integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

    MockHttpResponse response = dispatcher.get(String.format("/attachments/%s/preview", fileMock.getId()), user1Token);

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
  @DisplayName("Given an attachment identifier, if the user is not authenticated return a status code 401")
  public void getAttachmentPreview_testErrorUnauthenticatedUser() throws Exception {
    MockHttpResponse response = dispatcher.get(String.format("/attachments/%s/preview", UUID.randomUUID()), null);

    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
  }

  @Test
  @DisplayName("Given an attachment identifier, if authenticated user isn't a room member then return a status code 403")
  public void getAttachmentPreview_testErrorUserIsNotARoomMember() throws Exception {
    FileMock fileMock = MockedFiles.getImages();
    integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

    MockHttpResponse response = dispatcher.get(String.format("/attachments/%s/preview", fileMock.getId()), user3Token);

    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
  }

  @Test
  @DisplayName("Correctly returns the attachment information for requested id")
  public void getAttachmentInfo_testOk() throws Exception {
    FileMetadata savedMetadata = integrationTestUtils.generateAndSaveFileMetadata(UUID.randomUUID(), FileMetadataType.ATTACHMENT, user1Id, roomId);

    MockHttpResponse response = dispatcher.get(String.format("/attachments/%s", savedMetadata.getId()), user1Token);
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
    MockHttpResponse response = dispatcher.get(String.format("/attachments/%s", UUID.randomUUID()), null);

    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
  }

  @Test
  @DisplayName("Given an attachment identifier, if authenticated user isn't a room member then return a status code 403")
  public void getAttachmentInfo_testErrorUserIsNotARoomMember() throws Exception {
    FileMock fileMock = MockedFiles.getImages();
    integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

    MockHttpResponse response = dispatcher.get(String.format("/attachments/%s", fileMock.getId()), user3Token);

    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
  }

  @Test
  @DisplayName("Correctly deletes the attachment with requested id")
  public void deleteAttachment_testOk() throws Exception {
    FileMetadata fileMetadata = integrationTestUtils.generateAndSaveFileMetadata(MockedFiles.getImages(), FileMetadataType.ATTACHMENT, user1Id, roomId);

    MockHttpResponse response = dispatcher.delete(String.format("/attachments/%s", fileMetadata.getId()), user1Token);
    assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    assertTrue(fileMetadataRepository.getById(fileMetadata.getId()).isEmpty());
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    storageMockServer.verify("DELETE", "/delete", fileMetadata.getId(), 1);
  }

  @Test
  @DisplayName("Given an attachment identifier, if the user is not authenticated return a status code 401")
  public void deleteAttachment_testErrorUnauthenticatedUser() throws Exception {
    MockHttpResponse response = dispatcher.delete(String.join("", "/attachments/", UUID.randomUUID().toString()), null);

    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
  }

  @Test
  @DisplayName("Given an attachment identifier, if authenticated user isn't a room member then return a status code 403")
  public void deleteAttachment_testErrorUserIsNotARoomMember() throws Exception {
    FileMock fileMock = MockedFiles.getImages();
    integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

    MockHttpResponse response = dispatcher.delete(String.join("", "/attachments/", fileMock.getId()), user3Token);

    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
  }
}