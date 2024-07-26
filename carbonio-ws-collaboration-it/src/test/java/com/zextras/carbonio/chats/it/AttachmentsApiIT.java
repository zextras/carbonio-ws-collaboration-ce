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
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedFiles;
import com.zextras.carbonio.chats.it.utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.utils.MockedFiles.MockedFileType;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import java.util.UUID;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.verify.VerificationTimes;

@ApiIntegrationTest
public class AttachmentsApiIT {

  private final FileMetadataRepository fileMetadataRepository;
  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final StorageMockServer storageMockServer;

  public AttachmentsApiIT(
      AttachmentsApi attachmentsApi,
      FileMetadataRepository fileMetadataRepository,
      ObjectMapper objectMapper,
      ResteasyRequestDispatcher dispatcher,
      IntegrationTestUtils integrationTestUtils,
      StorageMockServer storageMockServer) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.objectMapper = objectMapper;
    this.dispatcher = dispatcher;
    this.integrationTestUtils = integrationTestUtils;
    this.storageMockServer = storageMockServer;
    this.dispatcher.getRegistry().addSingletonResource(attachmentsApi);
  }

  private static UUID user1Id;
  private static UUID user2Id;
  private static String user1Token;
  private static String user2Token;
  private static String user3Token;
  private static UUID roomId;

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
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
  }

  @AfterEach
  public void afterEach() {
    storageMockServer.setIsAliveResponse(true);
  }

  @Nested
  @DisplayName("Gets attachment test")
  class GetsAttachmentTests {

    private String url(String attachmentId) {
      return String.format("/attachments/%s/download", attachmentId);
    }

    @Test
    @DisplayName("Correctly returns the image mime type attachment file for requested id")
    void getImageMimeTypeAttachment_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.SNOOPY_IMAGE);
      fileMetadataRepository.save(
          integrationTestUtils.generateAndSaveFileMetadata(
              fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId));
      storageMockServer.mockNSLookupUrl(user1Id.toString(), true);
      storageMockServer.mockDownload(fileMock.getId(), user1Id.toString(), fileMock, true);

      MockHttpResponse response = dispatcher.get(url(fileMock.getId()), user1Token);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertArrayEquals(fileMock.getFileBytes(), response.getOutput());
      assertEquals(
          String.format("inline; filename=\"%s\"", fileMock.getName()),
          response.getOutputHeaders().get("Content-Disposition").get(0));
      assertEquals(
          fileMock.getMimeType(),
          response.getOutputHeaders().get("Content-Type").get(0).toString());
      assertEquals(fileMock.getSize(), response.getOutputHeaders().get("Content-Length").get(0));
      storageMockServer.verify(
          storageMockServer.getNSLookupUrlRequest(user1Id.toString()),
          VerificationTimes.exactly(1));
      storageMockServer.verify(
          storageMockServer.getDownloadRequest(fileMock.getId(), user1Id.toString()),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Correctly returns the docx mime type attachment file for requested id")
    void getDocxMimeTypeAttachment_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.DOCUMENT_DOCX);
      fileMetadataRepository.save(
          integrationTestUtils.generateAndSaveFileMetadata(
              fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId));
      storageMockServer.mockNSLookupUrl(user1Id.toString(), true);
      storageMockServer.mockDownload(fileMock.getId(), user1Id.toString(), fileMock, true);

      MockHttpResponse response = dispatcher.get(url(fileMock.getId()), user1Token);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertArrayEquals(fileMock.getFileBytes(), response.getOutput());
      assertEquals(
          String.format("inline; filename=\"%s\"", fileMock.getName()),
          response.getOutputHeaders().get("Content-Disposition").get(0));
      assertEquals(
          fileMock.getMimeType(),
          response.getOutputHeaders().get("Content-Type").get(0).toString());
      assertEquals(fileMock.getSize(), response.getOutputHeaders().get("Content-Length").get(0));
      storageMockServer.verify(
          storageMockServer.getNSLookupUrlRequest(user1Id.toString()),
          VerificationTimes.exactly(1));
      storageMockServer.verify(
          storageMockServer.getDownloadRequest(fileMock.getId(), user1Id.toString()),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Correctly returns the pptx mime type attachment file for requested id")
    void getPptxMimeTypeAttachment_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PRESENTATION_PPTX);
      fileMetadataRepository.save(
          integrationTestUtils.generateAndSaveFileMetadata(
              fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId));
      storageMockServer.mockNSLookupUrl(user1Id.toString(), true);
      storageMockServer.mockDownload(fileMock.getId(), user1Id.toString(), fileMock, true);

      MockHttpResponse response = dispatcher.get(url(fileMock.getId()), user1Token);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertArrayEquals(fileMock.getFileBytes(), response.getOutput());
      assertEquals(
          String.format("inline; filename=\"%s\"", fileMock.getName()),
          response.getOutputHeaders().get("Content-Disposition").get(0));
      assertEquals(
          fileMock.getMimeType(),
          response.getOutputHeaders().get("Content-Type").get(0).toString());
      assertEquals(fileMock.getSize(), response.getOutputHeaders().get("Content-Length").get(0));
      storageMockServer.verify(
          storageMockServer.getNSLookupUrlRequest(user1Id.toString()),
          VerificationTimes.exactly(1));
      storageMockServer.verify(
          storageMockServer.getDownloadRequest(fileMock.getId(), user1Id.toString()),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Correctly returns the xlsx mime type attachment file for requested id")
    void getXlsxMimeTypeAttachment_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.CALC_XLSX);
      fileMetadataRepository.save(
          integrationTestUtils.generateAndSaveFileMetadata(
              fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId));
      storageMockServer.mockNSLookupUrl(user1Id.toString(), true);
      storageMockServer.mockDownload(fileMock.getId(), user1Id.toString(), fileMock, true);

      MockHttpResponse response = dispatcher.get(url(fileMock.getId()), user1Token);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertArrayEquals(fileMock.getFileBytes(), response.getOutput());
      assertEquals(
          String.format("inline; filename=\"%s\"", fileMock.getName()),
          response.getOutputHeaders().get("Content-Disposition").get(0));
      assertEquals(
          fileMock.getMimeType(),
          response.getOutputHeaders().get("Content-Type").get(0).toString());
      assertEquals(fileMock.getSize(), response.getOutputHeaders().get("Content-Length").get(0));
      storageMockServer.verify(
          storageMockServer.getNSLookupUrlRequest(user1Id.toString()),
          VerificationTimes.exactly(1));
      storageMockServer.verify(
          storageMockServer.getDownloadRequest(fileMock.getId(), user1Id.toString()),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName(
        "Correctly returns the application octet-stream mime type attachment file for requested id")
    void getApplicationOctetStreamMimeTypeAttachment_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.TEST_ZX);
      fileMetadataRepository.save(
          integrationTestUtils.generateAndSaveFileMetadata(
              fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId));
      storageMockServer.mockNSLookupUrl(user1Id.toString(), true);
      storageMockServer.mockDownload(fileMock.getId(), user1Id.toString(), fileMock, true);

      MockHttpResponse response = dispatcher.get(url(fileMock.getId()), user1Token);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertArrayEquals(fileMock.getFileBytes(), response.getOutput());
      assertEquals(
          String.format("inline; filename=\"%s\"", fileMock.getName()),
          response.getOutputHeaders().get("Content-Disposition").get(0));
      assertEquals(
          fileMock.getMimeType(),
          response.getOutputHeaders().get("Content-Type").get(0).toString());
      assertEquals(fileMock.getSize(), response.getOutputHeaders().get("Content-Length").get(0));
      storageMockServer.verify(
          storageMockServer.getNSLookupUrlRequest(user1Id.toString()),
          VerificationTimes.exactly(1));
      storageMockServer.verify(
          storageMockServer.getDownloadRequest(fileMock.getId(), user1Id.toString()),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if the user is not authenticated return a status code 401")
    void getAttachment_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID().toString()), null);

      assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if authenticated user isn't a room member then return a"
            + " status code 403")
    void getAttachment_testErrorUserIsNotARoomMember() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

      MockHttpResponse response = dispatcher.get(url(fileMock.getId()), user3Token);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if the identifier is not an UUID return a status code 404")
    void getAttachment_testErrorIdentifierIsNotUUID() throws Exception {
      MockHttpResponse response = dispatcher.get(url("not_a_uuid"), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if the attachment doesn't exist then return a status code"
            + " 404")
    void getAttachment_testErrorFileNotExists() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID().toString()), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Gets attachment information tests")
  class GetsAttachmentInformationTests {

    private String url(String attachmentId) {
      return String.format("/attachments/%s", attachmentId);
    }

    @Test
    @DisplayName("Correctly returns the attachment information for requested id")
    void getAttachmentInfo_testOk() throws Exception {
      FileMetadata savedMetadata =
          integrationTestUtils.generateAndSaveFileMetadata(
              UUID.randomUUID(),
              "Test image",
              "image/png",
              FileMetadataType.ATTACHMENT,
              user1Id,
              roomId);

      MockHttpResponse response = dispatcher.get(url(savedMetadata.getId()), user1Token);
      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      AttachmentDto attachment =
          objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
      assertEquals(savedMetadata.getId(), attachment.getId().toString());
      assertEquals(roomId, attachment.getRoomId());
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if the user is not authenticated return a status code 401")
    void getAttachmentInfo_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID().toString()), null);

      assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if authenticated user isn't a room member then return a"
            + " status code 403")
    void getAttachmentInfo_testErrorUserIsNotARoomMember() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

      MockHttpResponse response = dispatcher.get(url(fileMock.getId()), user3Token);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if the identifier is not an UUID return a status code 404")
    void getAttachmentInfo_testErrorIdentifierIsNotUUID() throws Exception {
      MockHttpResponse response = dispatcher.get(url("not_a_uuid"), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if the attachment doesn't exist then return a status code"
            + " 404")
    void getAttachmentInfo_testErrorFileNotExists() throws Exception {
      MockHttpResponse response = dispatcher.get(url(UUID.randomUUID().toString()), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }

  @Nested
  @DisplayName("Deletes attachment tests")
  class DeletesAttachmentTests {

    private String url(String attachmentId) {
      return String.format("/attachments/%s", attachmentId);
    }

    @Test
    @DisplayName("Correctly deletes the attachment with requested id by its owner")
    void deleteAttachment_testOkByAttachmentOwner() throws Exception {
      FileMetadata fileMetadata =
          integrationTestUtils.generateAndSaveFileMetadata(
              MockedFiles.get(MockedFileType.PEANUTS_IMAGE),
              FileMetadataType.ATTACHMENT,
              user2Id,
              roomId);

      storageMockServer.mockNSLookupUrl(user2Id.toString(), true);
      storageMockServer.mockDelete(fileMetadata.getId(), user2Id.toString(), true);

      MockHttpResponse response = dispatcher.delete(url(fileMetadata.getId()), user2Token);
      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      assertTrue(fileMetadataRepository.getById(fileMetadata.getId()).isEmpty());
      storageMockServer.verify(
          storageMockServer.getNSLookupUrlRequest(user2Id.toString()),
          VerificationTimes.exactly(1));
      storageMockServer.verify(
          storageMockServer.getDeleteRequest(fileMetadata.getId(), user2Id.toString()),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName("Correctly deletes the attachment with requested id by room owner")
    void deleteAttachment_testOkByRoomOwner() throws Exception {
      FileMetadata fileMetadata =
          integrationTestUtils.generateAndSaveFileMetadata(
              MockedFiles.get(MockedFileType.PEANUTS_IMAGE),
              FileMetadataType.ATTACHMENT,
              user2Id,
              roomId);

      storageMockServer.mockNSLookupUrl(user2Id.toString(), true);
      storageMockServer.mockDelete(fileMetadata.getId(), user2Id.toString(), true);

      MockHttpResponse response = dispatcher.delete(url(fileMetadata.getId()), user1Token);
      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      assertTrue(fileMetadataRepository.getById(fileMetadata.getId()).isEmpty());

      storageMockServer.verify(
          storageMockServer.getNSLookupUrlRequest(user2Id.toString()),
          VerificationTimes.exactly(1));
      storageMockServer.verify(
          storageMockServer.getDeleteRequest(fileMetadata.getId(), user2Id.toString()),
          VerificationTimes.exactly(1));
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if the user is not authenticated return a status code 401")
    void deleteAttachment_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID().toString()), null);

      assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if authenticated user isn't a room member then return a"
            + " status code 403")
    void deleteAttachment_testErrorUserIsNotARoomMember() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

      MockHttpResponse response = dispatcher.delete(url(fileMock.getId()), user3Token);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if authenticated user isn't the attachment owner then"
            + " return a status code 403")
    void deleteAttachment_testErrorUserIsNotAnAttachmentOwner() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

      MockHttpResponse response = dispatcher.delete(url(fileMock.getId()), user2Token);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if the identifier is not an UUID return a status code 404")
    void deleteAttachment_testErrorIdentifierIsNotUUID() throws Exception {
      MockHttpResponse response = dispatcher.delete(url("not_a_uuid"), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if the attachment doesn't exist then return a status code"
            + " 404")
    void deleteAttachment_testErrorFileNotExists() throws Exception {
      MockHttpResponse response = dispatcher.delete(url(UUID.randomUUID().toString()), user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }
  }
}
