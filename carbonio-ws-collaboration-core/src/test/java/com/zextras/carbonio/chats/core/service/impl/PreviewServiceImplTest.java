// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.FileMetadataBuilder;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.model.FileResponse;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.PreviewException;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.service.PreviewService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.ImageQualityEnumDto;
import com.zextras.carbonio.chats.model.ImageShapeEnumDto;
import com.zextras.carbonio.chats.model.ImageTypeEnumDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.preview.PreviewClient;
import com.zextras.carbonio.preview.queries.BlobResponse;
import com.zextras.carbonio.preview.queries.Query;
import com.zextras.carbonio.preview.queries.enums.Format;
import com.zextras.carbonio.preview.queries.enums.Quality;
import com.zextras.carbonio.preview.queries.enums.ServiceType;
import com.zextras.carbonio.preview.queries.enums.Shape;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PreviewServiceImplTest {

  private final RoomService roomService;
  private final FileMetadataRepository fileMetadataRepository;
  private final PreviewClient previewClient;
  private final PreviewService previewService;
  private static Room room1;
  private static UUID user1Id;
  private static UUID user2Id;

  public PreviewServiceImplTest() {
    this.roomService = mock(RoomService.class);
    this.fileMetadataRepository = mock(FileMetadataRepository.class);
    this.previewClient = mock(PreviewClient.class);
    this.previewService =
        new PreviewServiceImpl(roomService, fileMetadataRepository, previewClient);
  }

  @BeforeAll
  public static void setup() {
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    room1 = Room.create();
    room1
        .id(UUID.randomUUID().toString())
        .type(RoomTypeDto.GROUP)
        .name("room1")
        .description("Room one")
        .subscriptions(
            List.of(
                Subscription.create(room1, user1Id.toString()).owner(true),
                Subscription.create(room1, user2Id.toString()).owner(false)));
  }

  @Test
  @DisplayName("Returns the preview of an image")
  public void getImagePreview() throws IOException {
    UUID fileId = UUID.randomUUID();
    UserPrincipal currentUser = UserPrincipal.create(user1Id);
    FileMetadata expectedMetadata =
        FileMetadataBuilder.create()
            .id(fileId.toString())
            .name("snoopy.jpg")
            .mimeType("image/jpeg")
            .type(FileMetadataType.ATTACHMENT)
            .userId(user1Id.toString())
            .roomId(room1.getId())
            .build();
    when(fileMetadataRepository.getById(fileId.toString()))
        .thenReturn(Optional.of(expectedMetadata));
    when(roomService.getRoomAndValidateUser(UUID.fromString(room1.getId()), currentUser, false))
        .thenReturn(room1);
    Query parameters =
        new Query.QueryBuilder()
            .setFileOwnerId(user1Id.toString())
            .setServiceType(ServiceType.CHATS)
            .setFileId(fileId.toString())
            .setVersion(0)
            .setPreviewArea("100x100")
            .setQuality(Quality.HIGH)
            .setOutputFormat(Format.JPEG)
            .setCrop(false)
            .build();
    BlobResponse mockBlobResponse = mock(BlobResponse.class);
    when(mockBlobResponse.getContent()).thenReturn(new ByteArrayInputStream("image".getBytes()));
    when(mockBlobResponse.getLength()).thenReturn(5L);
    when(mockBlobResponse.getMimeType()).thenReturn("image/jpeg");
    ArgumentCaptor<Query> parametersCapture = ArgumentCaptor.forClass(Query.class);
    when(previewClient.getPreviewOfImage(any(Query.class)))
        .thenReturn(Try.success(mockBlobResponse));
    FileResponse previewImageResponse =
        previewService.getImage(
            currentUser,
            fileId,
            "100x100",
            Option.of(ImageQualityEnumDto.HIGH),
            Option.of(ImageTypeEnumDto.JPEG),
            Option.of(false));

    verify(previewClient, times(1)).getPreviewOfImage(parametersCapture.capture());
    assertEquals(parametersCapture.getValue().toString(), parameters.toString());
    assertEquals("image", new String(previewImageResponse.getContent().readAllBytes()));
    assertEquals(previewImageResponse.getLength(), 5);
    assertEquals(previewImageResponse.getMimeType(), "image/jpeg");
  }

  @Test
  @DisplayName("Returns the thumbnail of an image")
  public void getImageThumbnail() throws IOException {
    UUID fileId = UUID.randomUUID();
    UserPrincipal currentUser = UserPrincipal.create(user1Id);
    FileMetadata expectedMetadata =
        FileMetadataBuilder.create()
            .id(fileId.toString())
            .name("snoopy.jpg")
            .mimeType("image/jpeg")
            .type(FileMetadataType.ATTACHMENT)
            .userId(user1Id.toString())
            .roomId(room1.getId())
            .build();
    when(fileMetadataRepository.getById(fileId.toString()))
        .thenReturn(Optional.of(expectedMetadata));
    when(roomService.getRoomAndValidateUser(UUID.fromString(room1.getId()), currentUser, false))
        .thenReturn(room1);
    Query parameters =
        new Query.QueryBuilder()
            .setFileOwnerId(user1Id.toString())
            .setServiceType(ServiceType.CHATS)
            .setFileId(fileId.toString())
            .setVersion(0)
            .setPreviewArea("100x100")
            .setQuality(Quality.HIGH)
            .setOutputFormat(Format.JPEG)
            .setShape(Shape.RECTANGULAR)
            .build();
    BlobResponse mockBlobResponse = mock(BlobResponse.class);
    when(mockBlobResponse.getContent()).thenReturn(new ByteArrayInputStream("image".getBytes()));
    when(mockBlobResponse.getLength()).thenReturn(5L);
    when(mockBlobResponse.getMimeType()).thenReturn("image/jpeg");
    ArgumentCaptor<Query> parametersCapture = ArgumentCaptor.forClass(Query.class);
    when(previewClient.getThumbnailOfImage(any(Query.class)))
        .thenReturn(Try.success(mockBlobResponse));
    FileResponse previewImageResponse =
        previewService.getImageThumbnail(
            currentUser,
            fileId,
            "100x100",
            Option.of(ImageQualityEnumDto.HIGH),
            Option.of(ImageTypeEnumDto.JPEG),
            Option.of(ImageShapeEnumDto.RECTANGULAR));

    verify(previewClient, times(1)).getThumbnailOfImage(parametersCapture.capture());
    assertEquals(parametersCapture.getValue().toString(), parameters.toString());
    assertEquals("image", new String(previewImageResponse.getContent().readAllBytes()));
    assertEquals(previewImageResponse.getLength(), 5);
    assertEquals(previewImageResponse.getMimeType(), "image/jpeg");
  }

  @Test
  @DisplayName("Returns the preview of a pdf")
  public void getPDFPreview() throws IOException {
    UUID fileId = UUID.randomUUID();
    UserPrincipal currentUser = UserPrincipal.create(user1Id);
    FileMetadata expectedMetadata =
        FileMetadataBuilder.create()
            .id(fileId.toString())
            .name("test.pdf")
            .mimeType("application/pdf")
            .type(FileMetadataType.ATTACHMENT)
            .userId(user1Id.toString())
            .roomId(room1.getId())
            .build();
    when(fileMetadataRepository.getById(fileId.toString()))
        .thenReturn(Optional.of(expectedMetadata));
    when(roomService.getRoomAndValidateUser(UUID.fromString(room1.getId()), currentUser, false))
        .thenReturn(room1);
    Query parameters =
        new Query.QueryBuilder()
            .setFileOwnerId(user1Id.toString())
            .setServiceType(ServiceType.CHATS)
            .setFileId(fileId.toString())
            .setVersion(0)
            .setFirstPage(1)
            .setLastPage(0)
            .build();
    BlobResponse mockBlobResponse = mock(BlobResponse.class);
    when(mockBlobResponse.getContent()).thenReturn(new ByteArrayInputStream("pdf".getBytes()));
    when(mockBlobResponse.getLength()).thenReturn(3L);
    when(mockBlobResponse.getMimeType()).thenReturn("application/pdf");
    ArgumentCaptor<Query> parametersCapture = ArgumentCaptor.forClass(Query.class);
    when(previewClient.getPreviewOfPdf(any(Query.class))).thenReturn(Try.success(mockBlobResponse));
    FileResponse previewImageResponse = previewService.getPDF(currentUser, fileId, 1, 0);

    verify(previewClient, times(1)).getPreviewOfPdf(parametersCapture.capture());
    assertEquals(parametersCapture.getValue().toString(), parameters.toString());
    assertEquals("pdf", new String(previewImageResponse.getContent().readAllBytes()));
    assertEquals(previewImageResponse.getLength(), 3);
    assertEquals(previewImageResponse.getMimeType(), "application/pdf");
  }

  @Test
  @DisplayName("Returns the thumbnail of a pdf")
  public void getPDFThumbnail() throws IOException {
    UUID fileId = UUID.randomUUID();
    UserPrincipal currentUser = UserPrincipal.create(user1Id);
    FileMetadata expectedMetadata =
        FileMetadataBuilder.create()
            .id(fileId.toString())
            .name("test.pdf")
            .mimeType("application/pdf")
            .type(FileMetadataType.ATTACHMENT)
            .userId(user1Id.toString())
            .roomId(room1.getId())
            .build();
    when(fileMetadataRepository.getById(fileId.toString()))
        .thenReturn(Optional.of(expectedMetadata));
    when(roomService.getRoomAndValidateUser(UUID.fromString(room1.getId()), currentUser, false))
        .thenReturn(room1);
    Query parameters =
        new Query.QueryBuilder()
            .setFileOwnerId(user1Id.toString())
            .setServiceType(ServiceType.CHATS)
            .setFileId(fileId.toString())
            .setVersion(0)
            .setPreviewArea("100x100")
            .setQuality(Quality.HIGH)
            .setOutputFormat(Format.JPEG)
            .setShape(Shape.RECTANGULAR)
            .build();
    BlobResponse mockBlobResponse = mock(BlobResponse.class);
    when(mockBlobResponse.getContent()).thenReturn(new ByteArrayInputStream("pdf".getBytes()));
    when(mockBlobResponse.getLength()).thenReturn(3L);
    when(mockBlobResponse.getMimeType()).thenReturn("application/pdf");
    ArgumentCaptor<Query> parametersCapture = ArgumentCaptor.forClass(Query.class);
    when(previewClient.getThumbnailOfPdf(any(Query.class)))
        .thenReturn(Try.success(mockBlobResponse));
    FileResponse previewImageResponse =
        previewService.getPDFThumbnail(
            currentUser,
            fileId,
            "100x100",
            Option.of(ImageQualityEnumDto.HIGH),
            Option.of(ImageTypeEnumDto.JPEG),
            Option.of(ImageShapeEnumDto.RECTANGULAR));

    verify(previewClient, times(1)).getThumbnailOfPdf(parametersCapture.capture());
    assertEquals(parametersCapture.getValue().toString(), parameters.toString());
    assertEquals("pdf", new String(previewImageResponse.getContent().readAllBytes()));
    assertEquals(previewImageResponse.getLength(), 3);
    assertEquals(previewImageResponse.getMimeType(), "application/pdf");
  }

  @Test
  @DisplayName("Returns error if user is not in the room")
  public void getImagePreviewNotAuthorized() {
    UUID fileId = UUID.randomUUID();
    UserPrincipal currentUser = UserPrincipal.create(user1Id);
    FileMetadata expectedMetadata =
        FileMetadataBuilder.create()
            .id(fileId.toString())
            .name("snoopy.jpg")
            .mimeType("image/jpeg")
            .type(FileMetadataType.ATTACHMENT)
            .userId(user1Id.toString())
            .roomId(room1.getId())
            .build();
    when(fileMetadataRepository.getById(fileId.toString()))
        .thenReturn(Optional.of(expectedMetadata));
    when(roomService.getRoomAndValidateUser(UUID.fromString(room1.getId()), currentUser, false))
        .thenThrow(new ForbiddenException());

    assertThrows(
        ForbiddenException.class,
        () ->
            previewService.getImage(
                currentUser,
                fileId,
                "100x100",
                Option.of(ImageQualityEnumDto.HIGH),
                Option.of(ImageTypeEnumDto.JPEG),
                Option.of(false)));
  }

  @Test
  @DisplayName("Returns error if preview returns an error")
  public void getImagePreviewPreviewException() {
    UUID fileId = UUID.randomUUID();
    UserPrincipal currentUser = UserPrincipal.create(user1Id);
    FileMetadata expectedMetadata =
        FileMetadataBuilder.create()
            .id(fileId.toString())
            .name("snoopy.jpg")
            .mimeType("image/jpeg")
            .type(FileMetadataType.ATTACHMENT)
            .userId(user1Id.toString())
            .roomId(room1.getId())
            .build();
    when(fileMetadataRepository.getById(fileId.toString()))
        .thenReturn(Optional.of(expectedMetadata));
    when(roomService.getRoomAndValidateUser(UUID.fromString(room1.getId()), currentUser, false))
        .thenReturn(room1);
    when(previewClient.getPreviewOfImage(any(Query.class)))
        .thenReturn(Try.failure(new RuntimeException()));
    assertThrows(
        PreviewException.class,
        () ->
            previewService.getImage(
                currentUser,
                fileId,
                "100x100",
                Option.of(ImageQualityEnumDto.HIGH),
                Option.of(ImageTypeEnumDto.JPEG),
                Option.of(false)));
  }
}
