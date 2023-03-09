// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it;

import com.zextras.carbonio.chats.api.PreviewApi;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedFiles;
import com.zextras.carbonio.chats.it.utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.utils.MockedFiles.MockedFileType;
import com.zextras.carbonio.chats.model.ImageQualityEnumDto;
import com.zextras.carbonio.chats.model.ImageTypeEnumDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.BinaryBody;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;



//@TestInstance(PER_CLASS)
public class PreviewApiIT {
  private final FileMetadataRepository fileMetadataRepository;
  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils      integrationTestUtils;
  private final UserManagementMockServer  userManagementMockServer;

  private  ClientAndServer mockServerClient;
  public PreviewApiIT(
    PreviewApi previewApi,
    ResteasyRequestDispatcher dispatcher,
    IntegrationTestUtils integrationTestUtils,
    UserManagementMockServer userManagementMockServer,
    FileMetadataRepository fileMetadataRepository,
    ObjectMapper objectMapper
  ) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.objectMapper = objectMapper;
    this.dispatcher = dispatcher;
    this.integrationTestUtils = integrationTestUtils;
    this.userManagementMockServer = userManagementMockServer;
    this.dispatcher.getRegistry().addSingletonResource(previewApi);
    this.mockServerClient = startClientAndServer();;
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
    //integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
    userManagementMockServer.reset();
    mockServerClient.reset();
  }

  @AfterEach
  public void afterEach() {
    //mockServerClient.reset();
  }
  public byte[] getFileBytes(String name) throws IOException {
    return Objects.requireNonNull(getClass().getResourceAsStream(String.format("/files/%s", name))).readAllBytes();
  }

  @Nested
  @DisplayName("Preview image tests")
  public class PreviewImageTests {


    private void mockGetPreviewImage(String fileId,
                                     String area,
                                     ImageQualityEnumDto quality,
                                     ImageTypeEnumDto format,
                                     Boolean crop,
                                     Status status,
                                     String filename) {
      try {
        HttpRequest request = request()
          .withMethod("GET")
          .withPath("/preview/image/{fileId}/{version}/{area}")
          .withPathParameter("fileId", fileId)
          .withPathParameter("version", "1")
          .withPathParameter("area", area)
          .withQueryStringParameter(param("service_type", "chats"));
        if (quality != null) request.withQueryStringParameter(param("quality", quality.toString()));
        if (format != null) request.withQueryStringParameter(param("output_format", format.toString()));
        if (crop != null) request.withQueryStringParameter(param("crop", crop.toString()));

        HttpResponse response = response()
          .withStatusCode(status.hashCode());
        if (filename != null) response.withBody(BinaryBody.binary(getFileBytes(filename)));
        if (format != null) {
          if (format == ImageTypeEnumDto.JPEG) {
            response.withContentType(MediaType.JPEG);
          } else {
            request.withContentType(MediaType.PNG);
          }
        }
        mockServerClient.when(
          request
        ).respond(
          response
        );
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private String previewImageUrl(String fileId,
                                   String area,
                                   ImageQualityEnumDto quality,
                                   ImageTypeEnumDto format,
                                   Boolean crop) {
      String result = String.format("/image/%s/1/%s/?", fileId, area);
      if (quality != null) result += "quality=" + quality + "&";
      if (format != null) result += "output_format=" + format + "&";
      if (crop != null) result += "crop=" + crop;
      return result;
    }

    @Test
    @DisplayName("Correctly returns the attachment preview for requested id")
    public void getAttachmentPreview_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.SNOOPY_IMAGE);
      //integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);
      mockGetPreviewImage(fileMock.getId(),
        "320x160",
        ImageQualityEnumDto.HIGH,
        ImageTypeEnumDto.JPEG,
        false,
        Status.OK,
        "snoopy.jpg");

      /*MockHttpResponse response = dispatcher.get(previewImageUrl(
          fileMock.getId(),
          "320x160",
          ImageQualityEnumDto.HIGH,
          ImageTypeEnumDto.JPEG,
          false),
        user1Token);*/

      FileMock expectedFile = MockedFiles.getPreview(MockedFileType.SNOOPY_PREVIEW);

      //assertEquals(Status.OK.getStatusCode(), response.getStatus());
      //assertArrayEquals(expectedFile.getFileBytes(), response.getOutput());
      //assertEquals("image/jpeg", response.getOutputHeaders().get("Content-Type").get(0).toString());
      //assertEquals(expectedFile.getSize(), response.getOutputHeaders().get("Content-Length").get(0));
      //userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
      /*previewerMockServer.verify("GET",
        String.format("/preview/image/%s/1/320x160/", fileMock.getId()),
        Map.of("service_type", "chats"), 1);*/
    }

   /* @Test
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
  }*/
  }
}