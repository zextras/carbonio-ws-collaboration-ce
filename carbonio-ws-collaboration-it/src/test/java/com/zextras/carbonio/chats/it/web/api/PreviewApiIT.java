// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

import com.zextras.carbonio.chats.api.PreviewApi;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.PreviewerMockServer;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedFiles;
import com.zextras.carbonio.chats.it.utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.utils.MockedFiles.MockedFileType;
import com.zextras.carbonio.chats.model.ImageQualityEnumDto;
import com.zextras.carbonio.chats.model.ImageShapeEnumDto;
import com.zextras.carbonio.chats.model.ImageTypeEnumDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.model.BinaryBody;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

@ApiIntegrationTest
class PreviewApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final IntegrationTestUtils integrationTestUtils;
  private final PreviewerMockServer previewMockServer;

  public PreviewApiIT(
      PreviewApi previewApi,
      ResteasyRequestDispatcher dispatcher,
      IntegrationTestUtils integrationTestUtils,
      PreviewerMockServer previewMockServer) {
    this.dispatcher = dispatcher;
    this.integrationTestUtils = integrationTestUtils;
    this.previewMockServer = previewMockServer;
    this.dispatcher.getRegistry().addSingletonResource(previewApi);
  }

  private static UUID user1Id;
  private static UUID user2Id;
  private static String user1Token;
  private static String user3Token;
  private static UUID roomId;

  @BeforeAll
  public static void initAll() {
    user1Id = MockedAccount.getAccounts().get(0).getUUID();
    user1Token = MockedAccount.getAccounts().get(0).getToken();
    user2Id = MockedAccount.getAccounts().get(1).getUUID();
    user3Token = MockedAccount.getAccounts().get(2).getToken();
    roomId = UUID.randomUUID();
  }

  @BeforeEach
  public void init() {
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "room", List.of(user1Id, user2Id));
    previewMockServer.reset();
  }

  private byte[] getFileBytes(String name) throws IOException {
    return Objects.requireNonNull(getClass().getResourceAsStream(String.format("/files/%s", name)))
        .readAllBytes();
  }

  @Nested
  @DisplayName("Preview image tests")
  class PreviewImageTests {

    private HttpRequest mockGetPreviewImageRequest(
        String fileId, String area, String quality, String format, Boolean crop) {
      HttpRequest request =
          request()
              .withMethod("GET")
              .withPath("/preview/image/{fileId}/0/{area}/")
              .withPathParameter("fileId", fileId)
              .withPathParameter("area", area)
              .withQueryStringParameter(param("service_type", "chats"));
      // The REST preview SDK only emits crop=true; when crop is false/absent it sends no crop
      // query param at all (see PreviewClient#buildImageQueryString), so only stub it when true.
      if (Boolean.TRUE.equals(crop)) request.withQueryStringParameter(param("crop", "true"));
      if (quality != null)
        request.withQueryStringParameter(param("quality", quality.toLowerCase()));
      if (format != null)
        request.withQueryStringParameter(param("output_format", format.toLowerCase()));
      return request;
    }

    private HttpResponse mockGetPreviewImageResponse(
        String format, Integer status, String filename) {
      try {
        HttpResponse response = response().withStatusCode(status);
        if (filename != null) response.withBody(BinaryBody.binary(getFileBytes(filename)));
        if (format != null) {
          if ("JPEG".equalsIgnoreCase(format)) {
            response.withContentType(MediaType.JPEG);
          } else {
            response.withContentType(MediaType.PNG);
          }
        }
        return response;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private void mockGetThumbnailImage(
        String fileId,
        String area,
        String quality,
        String format,
        String shape,
        Integer status,
        String filename) {
      try {
        HttpRequest request =
            request()
                .withMethod("GET")
                .withPath("/preview/image/{fileId}/0/{area}/thumbnail/")
                .withPathParameter("fileId", fileId)
                .withPathParameter("area", area)
                .withQueryStringParameter(param("service_type", "chats"));
        if (quality != null)
          request.withQueryStringParameter(param("quality", quality.toLowerCase()));
        if (format != null)
          request.withQueryStringParameter(param("output_format", format.toLowerCase()));
        if (shape != null)
          request.withQueryStringParameter(param("shape", shape.toLowerCase()));

        HttpResponse response = response().withStatusCode(status);
        if (filename != null) response.withBody(BinaryBody.binary(getFileBytes(filename)));
        if (format != null) {
          if ("JPEG".equalsIgnoreCase(format)) {
            response.withContentType(MediaType.JPEG);
          } else {
            response.withContentType(MediaType.PNG);
          }
        }
        previewMockServer.when(request).respond(response);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private void mockGetPreviewPDF(
        String fileId, Integer firstPage, Integer lastPage, Integer status, String filename) {
      try {
        HttpRequest request =
            request()
                .withMethod("GET")
                .withPath("/preview/pdf/{fileId}/0/")
                .withPathParameter("fileId", fileId)
                .withQueryStringParameter(param("service_type", "chats"));
        // The REST preview SDK only emits first_page/last_page when the value is > 0
        // (see PreviewClient#buildPdfQueryString), so only stub them under the same condition.
        if (firstPage != null && firstPage > 0)
          request.withQueryStringParameter(param("first_page", firstPage.toString()));
        if (lastPage != null && lastPage > 0)
          request.withQueryStringParameter(param("last_page", lastPage.toString()));

        HttpResponse response = response().withStatusCode(status);
        if (filename != null) response.withBody(BinaryBody.binary(getFileBytes(filename)));
        response.withContentType(MediaType.PDF);

        previewMockServer.when(request).respond(response);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private void mockGetThumbnailPDF(
        String fileId,
        String area,
        String quality,
        String format,
        String shape,
        Integer status,
        String filename) {
      try {
        HttpRequest request =
            request()
                .withMethod("GET")
                .withPath("/preview/pdf/{fileId}/0/{area}/thumbnail/")
                .withPathParameter("fileId", fileId)
                .withPathParameter("area", area)
                .withQueryStringParameter(param("service_type", "chats"));
        if (quality != null)
          request.withQueryStringParameter(param("quality", quality.toLowerCase()));
        if (format != null)
          request.withQueryStringParameter(param("output_format", format.toLowerCase()));
        if (shape != null)
          request.withQueryStringParameter(param("shape", shape.toLowerCase()));

        HttpResponse response = response().withStatusCode(status);
        if (filename != null) response.withBody(BinaryBody.binary(getFileBytes(filename)));
        if (format != null) {
          if ("JPEG".equalsIgnoreCase(format)) {
            response.withContentType(MediaType.JPEG);
          } else {
            response.withContentType(MediaType.PNG);
          }
        }
        previewMockServer.when(request).respond(response);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private String previewImageUrl(
        String fileId,
        String area,
        ImageQualityEnumDto quality,
        ImageTypeEnumDto format,
        Boolean crop) {
      String result = String.format("/preview/image/%s/%s/?", fileId, area);
      if (quality != null) result += "quality=" + quality + "&";
      if (format != null) result += "output_format=" + format + "&";
      if (crop != null) result += "crop=" + crop;
      return result;
    }

    private String thumbnailImageUrl(
        String fileId,
        String area,
        ImageQualityEnumDto quality,
        ImageTypeEnumDto format,
        ImageShapeEnumDto shape) {
      String result = String.format("/preview/image/%s/%s/thumbnail/?", fileId, area);
      if (quality != null) result += "quality=" + quality + "&";
      if (format != null) result += "output_format=" + format + "&";
      if (shape != null) result += "shape=" + shape;
      return result;
    }

    private String previewPDFUrl(String fileId, Integer firstPage, Integer lastPage) {
      String result = String.format("/preview/pdf/%s/?", fileId);
      if (firstPage != null) result += "firstPage=" + firstPage + "&";
      if (lastPage != null) result += "lastPage=" + lastPage;

      return result;
    }

    private String thumbnailPDFUrl(
        String fileId,
        String area,
        ImageQualityEnumDto quality,
        ImageTypeEnumDto format,
        ImageShapeEnumDto shape) {
      String result = String.format("/preview/pdf/%s/%s/thumbnail/?", fileId, area);
      if (quality != null) result += "quality=" + quality + "&";
      if (format != null) result += "output_format=" + format + "&";
      if (shape != null) result += "shape=" + shape;
      return result;
    }

    @Test
    @DisplayName("Correctly returns the image preview for requested id")
    void getImagePreview_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.SNOOPY_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);
      FileMock expectedFile = MockedFiles.getPreview(MockedFileType.SNOOPY_PREVIEW);
      previewMockServer
          .when(
              mockGetPreviewImageRequest(
                  fileMock.getId(), "320x160", "HIGH", "JPEG", false))
          .respond(
              mockGetPreviewImageResponse(
                  "JPEG", Status.OK.getStatusCode(), expectedFile.getName()));

      MockHttpResponse response =
          dispatcher.get(
              previewImageUrl(
                  fileMock.getId(),
                  "320x160",
                  ImageQualityEnumDto.HIGH,
                  ImageTypeEnumDto.JPEG,
                  false),
              user1Token);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertArrayEquals(expectedFile.getFileBytes(), response.getOutput());
      assertEquals("image/jpeg", response.getOutputHeaders().get("Content-Type").get(0).toString());
    }

    @Test
    @DisplayName("Correctly returns the image thumbnail for requested id")
    void getImageThumbnail_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.SNOOPY_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);
      FileMock expectedFile = MockedFiles.getPreview(MockedFileType.SNOOPY_PREVIEW);
      mockGetThumbnailImage(
          fileMock.getId(),
          "320x160",
          "HIGH",
          "JPEG",
          "RECTANGULAR",
          Status.OK.getStatusCode(),
          expectedFile.getName());

      MockHttpResponse response =
          dispatcher.get(
              thumbnailImageUrl(
                  fileMock.getId(),
                  "320x160",
                  ImageQualityEnumDto.HIGH,
                  ImageTypeEnumDto.JPEG,
                  ImageShapeEnumDto.RECTANGULAR),
              user1Token);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertArrayEquals(expectedFile.getFileBytes(), response.getOutput());
      assertEquals("image/jpeg", response.getOutputHeaders().get("Content-Type").get(0).toString());
    }

    @Test
    @DisplayName("Correctly returns the pdf preview for requested id")
    void getPDFPreview_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_PDF);
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);
      FileMock expectedFile = MockedFiles.get(MockedFileType.PEANUTS_PDF);
      mockGetPreviewPDF(fileMock.getId(), 1, 0, Status.OK.getStatusCode(), expectedFile.getName());

      MockHttpResponse response = dispatcher.get(previewPDFUrl(fileMock.getId(), 1, 0), user1Token);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertArrayEquals(expectedFile.getFileBytes(), response.getOutput());
      assertEquals(
          "application/pdf", response.getOutputHeaders().get("Content-Type").get(0).toString());
    }

    @Test
    @DisplayName("Correctly returns the pdf thumbnail for requested id")
    void getPDFThumbnail_testOk() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_PDF);
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);
      FileMock expectedFile = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      mockGetThumbnailPDF(
          fileMock.getId(),
          "320x160",
          "HIGH",
          "JPEG",
          "RECTANGULAR",
          Status.OK.getStatusCode(),
          expectedFile.getName());

      MockHttpResponse response =
          dispatcher.get(
              thumbnailPDFUrl(
                  fileMock.getId(),
                  "320x160",
                  ImageQualityEnumDto.HIGH,
                  ImageTypeEnumDto.JPEG,
                  ImageShapeEnumDto.RECTANGULAR),
              user1Token);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertArrayEquals(expectedFile.getFileBytes(), response.getOutput());
      assertEquals("image/jpeg", response.getOutputHeaders().get("Content-Type").get(0).toString());
    }

    @Test
    @DisplayName("Returns 424 if the Previewer server is down")
    void getAttachmentPreview_testExceptionPreviewerKO() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.SNOOPY_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);
      previewMockServer
          .when(
              mockGetPreviewImageRequest(
                  fileMock.getId(), "320x160", "HIGH", "JPEG", false))
          .respond(mockGetPreviewImageResponse(null, 500, null));

      MockHttpResponse response =
          dispatcher.get(
              previewImageUrl(
                  fileMock.getId(),
                  "320x160",
                  ImageQualityEnumDto.HIGH,
                  ImageTypeEnumDto.JPEG,
                  false),
              user1Token);

      assertEquals(424, response.getStatus());
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if the user is not authenticated return a status code 401")
    void getAttachmentPreview_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response =
          dispatcher.get(
              previewImageUrl(
                  UUID.randomUUID().toString(),
                  "320x160",
                  ImageQualityEnumDto.HIGH,
                  ImageTypeEnumDto.JPEG,
                  false),
              null);

      assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if authenticated user isn't a room member then return a"
            + " status code 404")
    void getAttachmentPreview_testErrorUserIsNotARoomMember() throws Exception {
      FileMock fileMock = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
      integrationTestUtils.generateAndSaveFileMetadata(
          fileMock, FileMetadataType.ATTACHMENT, user1Id, roomId);

      MockHttpResponse response =
          dispatcher.get(
              previewImageUrl(
                  UUID.randomUUID().toString(),
                  "320x160",
                  ImageQualityEnumDto.HIGH,
                  ImageTypeEnumDto.JPEG,
                  false),
              user3Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName(
        "Given an attachment identifier, if the attachment doesn't exist then return a status code"
            + " 404")
    void getAttachmentPreview_testErrorFileNotExists() throws Exception {
      MockHttpResponse response =
          dispatcher.get(
              previewImageUrl(
                  UUID.randomUUID().toString(),
                  "320x160",
                  ImageQualityEnumDto.HIGH,
                  ImageTypeEnumDto.JPEG,
                  false),
              user1Token);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
  }
}
