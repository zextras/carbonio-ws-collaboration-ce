// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zextras.carbonio.chats.api.RFC3339DateFormat;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.FileMetadataBuilder;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.model.AttachmentFilter;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.model.PaginationFilter;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.StanzaResponse;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.AttachmentsPaginationDto;
import com.zextras.carbonio.chats.model.BulkDeleteAttachmentsResponseDto;
import com.zextras.carbonio.chats.model.IdDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.preview.sdk.PreviewClient;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@UnitTest
public class AttachmentServiceImplTest {

  private final AttachmentService attachmentService;
  private final FileMetadataRepository fileMetadataRepository;
  private final StoragesService storagesService;
  private final RoomService roomService;
  private final MessageDispatcher messageDispatcher;
  private final ObjectMapper objectMapper;

  public AttachmentServiceImplTest(AttachmentMapper attachmentMapper) {
    this.fileMetadataRepository = mock(FileMetadataRepository.class);
    this.storagesService = mock(StoragesService.class);
    this.roomService = mock(RoomService.class);
    this.messageDispatcher = mock(MessageDispatcher.class);
    this.objectMapper =
        JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build()
            .setDateFormat(new RFC3339DateFormat());
    this.attachmentService =
        new AttachmentServiceImpl(
            this.fileMetadataRepository,
            attachmentMapper,
            this.storagesService,
            this.roomService,
            this.messageDispatcher,
            this.objectMapper,
            mock(PreviewClient.class));
  }

  private static UUID user1Id;
  private static UUID user2Id;
  private static UUID user3Id;
  private static UUID roomId;
  private static Room room1;
  private static Room room2;

  @BeforeAll
  public static void initAll() {
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    user3Id = UUID.randomUUID();
    roomId = UUID.randomUUID();
    room1 = Room.create();
    room1
        .id(roomId.toString())
        .type(RoomTypeDto.GROUP)
        .name("room1")
        .description("Room one")
        .subscriptions(
            List.of(
                Subscription.create(room1, user1Id.toString()).owner(true),
                Subscription.create(room1, user2Id.toString()).owner(false),
                Subscription.create(room1, user3Id.toString()).owner(false)));
    room2 = Room.create();
    room2
        .id(roomId.toString())
        .type(RoomTypeDto.GROUP)
        .name("room2")
        .description("Room two")
        .subscriptions(
            List.of(
                Subscription.create(room1, user1Id.toString()).owner(true),
                Subscription.create(room1, user2Id.toString()).owner(false),
                Subscription.create(room1, user3Id.toString()).owner(false)));
  }

  @Nested
  @DisplayName("Get attachment info by room id tests")
  class GetAttachmentInfoTests {

    @Test
    @DisplayName("Returns a single page of attachments info of the required room")
    void getAttachmentInfoByRoomId_testOkSinglePage() {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime attachmentTimestamp = OffsetDateTime.now();
      when(fileMetadataRepository.getByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT, 11, null, null))
          .thenReturn(
              List.of(
                  FileMetadataBuilder.create()
                      .id(file1Id.toString())
                      .name("image1.jpg")
                      .originalSize(0L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(attachmentTimestamp)
                      .updatedAt(attachmentTimestamp)
                      .build(),
                  FileMetadataBuilder.create()
                      .id(file2Id.toString())
                      .name("pdf1.pdf")
                      .originalSize(0L)
                      .mimeType("application/pdf")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user2Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(attachmentTimestamp.plusHours(1))
                      .updatedAt(attachmentTimestamp.plusHours(1))
                      .build()));
      when(fileMetadataRepository.countByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT, null))
          .thenReturn(2L);
      AttachmentsPaginationDto attachmentsPagination =
          attachmentService.getAttachmentInfoByRoomId(roomId, 10, null, null, currentUser);

      assertEquals(2, attachmentsPagination.getAttachments().size());
      assertEquals(2L, attachmentsPagination.getTotal());
      assertEquals(file1Id, attachmentsPagination.getAttachments().get(0).getId());
      assertEquals(file2Id, attachmentsPagination.getAttachments().get(1).getId());
      assertNull(attachmentsPagination.getCursor());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

    @Test
    @DisplayName("Returns first page of attachments info of the required room")
    void getAttachmentInfoByRoomId_testOkFirstPage() throws Exception {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      UUID file3Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime attachmentTimestamp = OffsetDateTime.now();
      when(fileMetadataRepository.getByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT, 3, null, null))
          .thenReturn(
              List.of(
                  FileMetadataBuilder.create()
                      .id(file1Id.toString())
                      .name("image1.jpg")
                      .originalSize(0L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(attachmentTimestamp.plusHours(2))
                      .updatedAt(attachmentTimestamp.plusHours(2))
                      .build(),
                  FileMetadataBuilder.create()
                      .id(file2Id.toString())
                      .name("image2.jpg")
                      .originalSize(0L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(attachmentTimestamp.plusHours(1))
                      .updatedAt(attachmentTimestamp.plusHours(1))
                      .build(),
                  FileMetadataBuilder.create()
                      .id(file3Id.toString())
                      .name("pdf1.pdf")
                      .originalSize(0L)
                      .mimeType("application/pdf")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user2Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(attachmentTimestamp)
                      .updatedAt(attachmentTimestamp)
                      .build()));
      when(fileMetadataRepository.countByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT, null))
          .thenReturn(3L);
      AttachmentsPaginationDto attachmentsPagination =
          attachmentService.getAttachmentInfoByRoomId(roomId, 2, null, null, currentUser);

      assertEquals(2, attachmentsPagination.getAttachments().size());
      assertEquals(3L, attachmentsPagination.getTotal());
      assertEquals(file1Id, attachmentsPagination.getAttachments().get(0).getId());
      assertEquals(file2Id, attachmentsPagination.getAttachments().get(1).getId());
      assertNotNull(attachmentsPagination.getCursor());
      String expectedCursor =
          Base64.getEncoder()
              .encodeToString(
                  objectMapper.writeValueAsBytes(
                      PaginationFilter.create(
                          file2Id.toString(),
                          attachmentTimestamp.plusHours(1),
                          null,
                          "created_at",
                          "desc")));
      assertEquals(expectedCursor, attachmentsPagination.getCursor());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

    @Test
    @DisplayName("Returns last page of attachments info of the required room")
    void getAttachmentInfoByRoomId_testOkLastPage() throws Exception {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime attachmentTimestamp = OffsetDateTime.now();
      PaginationFilter paginationFilter =
          PaginationFilter.create(file1Id.toString(), attachmentTimestamp.plusHours(1), null);
      String filter =
          Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(paginationFilter));
      when(fileMetadataRepository.getByRoomIdAndType(
              eq(roomId.toString()),
              eq(FileMetadataType.ATTACHMENT),
              eq(3),
              any(PaginationFilter.class),
              isNull()))
          .thenReturn(
              List.of(
                  FileMetadataBuilder.create()
                      .id(file2Id.toString())
                      .name("pdf1.pdf")
                      .originalSize(0L)
                      .mimeType("application/pdf")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user2Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(attachmentTimestamp)
                      .updatedAt(attachmentTimestamp)
                      .build()));
      when(fileMetadataRepository.countByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT, null))
          .thenReturn(2L);

      AttachmentsPaginationDto attachmentsPagination =
          attachmentService.getAttachmentInfoByRoomId(roomId, 2, filter, null, currentUser);

      assertEquals(1, attachmentsPagination.getAttachments().size());
      assertEquals(2L, attachmentsPagination.getTotal());
      assertEquals(file2Id, attachmentsPagination.getAttachments().get(0).getId());
      assertNull(attachmentsPagination.getCursor());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

    @Test
    @DisplayName(
        "Given a room identifier, if authenticated user isn't a room member then throws a"
            + " 'forbidden' exception")
    void getAttachmentInfoByRoomId_testAuthenticatedUserIsNotARoomMember() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false))
          .thenThrow(
              new ForbiddenException(
                  String.format(
                      "User '%s' is not a member of room '%s'", currentUser.getId(), roomId)));

      assertThrows(
          ForbiddenException.class,
          () -> attachmentService.getAttachmentInfoByRoomId(roomId, 10, null, null, currentUser));
    }

    @Test
    @DisplayName("Re throws the exception if the room was not found")
    void getAttachmentInfoByRoomId_testRoomNotFound() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false))
          .thenThrow(new NotFoundException());

      NotFoundException notFoundException =
          assertThrows(
              NotFoundException.class,
              () ->
                  attachmentService.getAttachmentInfoByRoomId(roomId, 10, null, null, currentUser));
      assertEquals("Not Found - Not Found", notFoundException.getMessage());
    }

    @Test
    @DisplayName(
        "Given a room identifier, correctly returns an empty list when there isn't any attachment"
            + " of the required room")
    void getAttachmentInfoByRoomId_testNoAttachment() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT, 11, null, null))
          .thenReturn(List.of());
      when(fileMetadataRepository.countByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT, null))
          .thenReturn(0L);

      AttachmentsPaginationDto attachmentsPagination =
          attachmentService.getAttachmentInfoByRoomId(roomId, 10, null, null, currentUser);

      assertNotNull(attachmentsPagination);
      assertEquals(0, attachmentsPagination.getAttachments().size());
      assertEquals(0L, attachmentsPagination.getTotal());
      assertNull(attachmentsPagination.getCursor());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }
  }

  @Nested
  @DisplayName("Get attachment by id tests")
  class GetAttachmentByIdTests {

    @Test
    @DisplayName("Retrieves the specified attachments")
    void getAttachmentById_testOk() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user2Id);

      FileMetadata fileMetadata =
          FileMetadataBuilder.create()
              .id(attachmentUuid.toString())
              .name("image1.jpg")
              .originalSize(0L)
              .mimeType("image/jpg")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user1Id.toString())
              .roomId(roomId.toString())
              .createdAt(OffsetDateTime.now())
              .updatedAt(OffsetDateTime.now())
              .build();
      when(fileMetadataRepository.getById(attachmentUuid.toString()))
          .thenReturn(Optional.of(fileMetadata));

      when(storagesService.getFileStreamById(attachmentUuid.toString(), user1Id.toString()))
          .thenReturn(mock(InputStream.class));
      FileContentAndMetadata attachmentById =
          attachmentService.getAttachmentById(attachmentUuid, currentUser);

      assertNotNull(attachmentById);
      assertNotNull(attachmentById.getFileStream());
      assertNotNull(attachmentById.getMetadata());
      assertEquals(attachmentUuid.toString(), attachmentById.getMetadata().getId());
      verify(fileMetadataRepository, times(1)).getById(attachmentUuid.toString());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verify(storagesService, times(1))
          .getFileStreamById(attachmentUuid.toString(), user1Id.toString());
      verifyNoMoreInteractions(fileMetadataRepository, roomService, storagesService);
    }

    @Test
    @DisplayName("Throws an exception if the attachment is not found in our local db")
    void getAttachmentById_testNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.empty());

      NotFoundException notFoundException =
          assertThrows(
              NotFoundException.class,
              () -> attachmentService.getAttachmentById(attachmentUuid, currentUser));
      assertEquals(
          String.format("Not Found - Attachment '%s' not found", attachmentUuid),
          notFoundException.getMessage());

      verify(fileMetadataRepository, times(1)).getById(attachmentUuid.toString());
      verifyNoMoreInteractions(fileMetadataRepository);
      verifyNoInteractions(roomService, storagesService);
    }

    @Test
    @DisplayName("Re throws an exception if the user is not part of the room")
    void getAttachmentById_testForbidden() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString()))
          .thenReturn(
              Optional.of(
                  FileMetadataBuilder.create()
                      .id(attachmentUuid.toString())
                      .name("image1.jpg")
                      .originalSize(0L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(OffsetDateTime.now())
                      .updatedAt(OffsetDateTime.now())
                      .build()));
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false))
          .thenThrow(new ForbiddenException());

      assertThrows(
          ForbiddenException.class,
          () -> attachmentService.getAttachmentById(attachmentUuid, currentUser));

      verify(fileMetadataRepository, times(1)).getById(attachmentUuid.toString());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoMoreInteractions(fileMetadataRepository, roomService);
      verifyNoInteractions(storagesService);
    }

    @Test
    @DisplayName("Re throws an exception if the room is not found")
    void getAttachmentById_testRoomNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString()))
          .thenReturn(
              Optional.of(
                  FileMetadataBuilder.create()
                      .id(attachmentUuid.toString())
                      .name("image1.jpg")
                      .originalSize(0L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(OffsetDateTime.now())
                      .updatedAt(OffsetDateTime.now())
                      .build()));
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false))
          .thenThrow(new NotFoundException());

      NotFoundException notFoundException =
          assertThrows(
              NotFoundException.class,
              () -> attachmentService.getAttachmentById(attachmentUuid, currentUser));
      assertEquals("Not Found - Not Found", notFoundException.getMessage());

      verify(fileMetadataRepository, times(1)).getById(attachmentUuid.toString());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoMoreInteractions(fileMetadataRepository, roomService);
      verifyNoInteractions(storagesService);
    }

    @Test
    @DisplayName("Re throws an exception if storages service throws an exception")
    void getAttachmentById_testStorageException() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString()))
          .thenReturn(
              Optional.of(
                  FileMetadataBuilder.create()
                      .id(attachmentUuid.toString())
                      .name("image1.jpg")
                      .originalSize(0L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(OffsetDateTime.now())
                      .updatedAt(OffsetDateTime.now())
                      .build()));
      when(storagesService.getFileStreamById(attachmentUuid.toString(), user1Id.toString()))
          .thenThrow(new StorageException());
      assertThrows(
          StorageException.class,
          () -> attachmentService.getAttachmentById(attachmentUuid, currentUser));

      verify(fileMetadataRepository, times(1)).getById(attachmentUuid.toString());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verify(storagesService, times(1))
          .getFileStreamById(attachmentUuid.toString(), user1Id.toString());
      verifyNoMoreInteractions(fileMetadataRepository, roomService, storagesService);
    }
  }

  @Nested
  @DisplayName("Get attachment info by id")
  class GetAttachmentInfoByIdTests {

    @Test
    @DisplayName("Returns the attachment info")
    void getAttachmentInfoById_testOk() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadata metadata =
          FileMetadataBuilder.create()
              .id(attachmentUuid.toString())
              .name("test.pdf")
              .originalSize(0L)
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user1Id.toString())
              .roomId(roomId.toString())
              .messageId("xmpp-msg-id-123")
              .createdAt(OffsetDateTime.now())
              .updatedAt(OffsetDateTime.now())
              .build();
      when(fileMetadataRepository.getById(attachmentUuid.toString()))
          .thenReturn(Optional.of(metadata));

      AttachmentDto attachmentInfo =
          attachmentService.getAttachmentInfoById(attachmentUuid, currentUser);

      assertNotNull(attachmentInfo);
      assertEquals(attachmentUuid, attachmentInfo.getId());
      assertEquals("xmpp-msg-id-123", attachmentInfo.getMessageId());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

    @Test
    @DisplayName("Throws an exception if the file is not found")
    void getAttachmentInfoById_testNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.empty());

      NotFoundException notFoundException =
          assertThrows(
              NotFoundException.class,
              () -> attachmentService.getAttachmentInfoById(attachmentUuid, currentUser));
      assertEquals(
          String.format("Not Found - Attachment '%s' not found", attachmentUuid),
          notFoundException.getMessage());
    }

    @Test
    @DisplayName("Re throws an exception if the user is not a room member")
    void getAttachmentInfoById_testForbidden() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadata metadata =
          FileMetadataBuilder.create()
              .id(attachmentUuid.toString())
              .name("test.pdf")
              .originalSize(0L)
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user1Id.toString())
              .roomId(roomId.toString())
              .createdAt(OffsetDateTime.now())
              .updatedAt(OffsetDateTime.now())
              .build();
      when(fileMetadataRepository.getById(attachmentUuid.toString()))
          .thenReturn(Optional.of(metadata));
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false))
          .thenThrow(new ForbiddenException());

      assertThrows(
          ForbiddenException.class,
          () -> attachmentService.getAttachmentInfoById(attachmentUuid, currentUser));
    }

    @Test
    @DisplayName("Re throws an exception if the room is not found")
    void getAttachmentInfoById_testRoomNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadata metadata =
          FileMetadataBuilder.create()
              .id(attachmentUuid.toString())
              .name("test.pdf")
              .originalSize(0L)
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user1Id.toString())
              .roomId(roomId.toString())
              .createdAt(OffsetDateTime.now())
              .updatedAt(OffsetDateTime.now())
              .build();
      when(fileMetadataRepository.getById(attachmentUuid.toString()))
          .thenReturn(Optional.of(metadata));
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false))
          .thenThrow(new NotFoundException());

      NotFoundException notFoundException =
          assertThrows(
              NotFoundException.class,
              () -> attachmentService.getAttachmentInfoById(attachmentUuid, currentUser));
      assertEquals("Not Found - Not Found", notFoundException.getMessage());
    }
  }

  @Nested
  @DisplayName("Add attachment tests")
  class AddAttachmentTests {

    @Test
    @DisplayName("Creates the attachment and returns it")
    void addAttachment_testOk() {
      UUID attachmentUuid = UUID.randomUUID();
      OffsetDateTime attachmentDate = OffsetDateTime.parse("2022-01-01T00:00:00Z");
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);
      when(messageDispatcher.sendAttachment(
              eq(roomId.toString()),
              eq(user1Id.toString()),
              eq(attachmentUuid.toString()),
              eq("temp.pdf"),
              eq("application/pdf"),
              eq(1024L),
              eq("description"),
              eq("xmpp-msg-abc"),
              isNull(),
              isNull()))
          .thenReturn(new StanzaResponse("xmpp-msg-abc", "stanza-id-xyz"));
      InputStream fileStream = mock(InputStream.class);
      FileMetadata savedMetadata =
          FileMetadataBuilder.create()
              .id(attachmentUuid.toString())
              .name("temp.pdf")
              .originalSize(1024L)
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user1Id.toString())
              .roomId(roomId.toString())
              .messageId("xmpp-msg-abc")
              .stanzaId("stanza-id-xyz")
              .createdAt(attachmentDate)
              .build();
      when(fileMetadataRepository.save(any())).thenReturn(savedMetadata);
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attachmentUuid);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        uuid.when(() -> UUID.fromString(attachmentUuid.toString())).thenReturn(attachmentUuid);
        attachmentService.addAttachment(
            roomId,
            fileStream,
            "application/pdf",
            1024L,
            "temp.pdf",
            "description",
            "xmpp-msg-abc",
            null,
            null,
            currentUser);
      }

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verify(storagesService, times(1))
          .saveFile(fileStream, attachmentUuid.toString(), currentUser.toString(), 1024L);
      verify(messageDispatcher, times(1))
          .sendAttachment(
              roomId.toString(),
              user1Id.toString(),
              attachmentUuid.toString(),
              "temp.pdf",
              "application/pdf",
              1024L,
              "description",
              "xmpp-msg-abc",
              null,
              null);
      ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
      verify(fileMetadataRepository, times(1)).save(captor.capture());
      assertEquals("xmpp-msg-abc", captor.getValue().getMessageId());
      assertEquals("stanza-id-xyz", captor.getValue().getStanzaId());
      verifyNoMoreInteractions(
          roomService, storagesService, fileMetadataRepository, messageDispatcher);
    }

    @Test
    @DisplayName(
        "Creating a video attachment does NOT call preview (lazy generation on first view)")
    void addAttachment_videoDoesNotTriggerGeneration() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);
      when(messageDispatcher.sendAttachment(
              any(), any(), any(), any(), any(), anyLong(), any(), any(), any(), any()))
          .thenReturn(new StanzaResponse("xmpp-msg-abc", "stanza-id-xyz"));
      when(fileMetadataRepository.save(any())).thenReturn(FileMetadata.create());
      InputStream fileStream = mock(InputStream.class);

      IdDto result;
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attachmentUuid);
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        result =
            attachmentService.addAttachment(
                roomId,
                fileStream,
                "video/mp4",
                2048L,
                "clip.mp4",
                "description",
                "xmpp-msg-abc",
                null,
                null,
                currentUser);
      }

      // Upload succeeds end-to-end; preview is NOT called on upload (lazy generation).
      assertEquals(attachmentUuid, result.getId());
    }

    @Test
    @DisplayName("Creating an image attachment does NOT call preview")
    void addAttachment_imageDoesNotCallPreview() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);
      when(messageDispatcher.sendAttachment(
              any(), any(), any(), any(), any(), anyLong(), any(), any(), any(), any()))
          .thenReturn(new StanzaResponse("xmpp-msg-abc", "stanza-id-xyz"));
      when(fileMetadataRepository.save(any())).thenReturn(FileMetadata.create());
      InputStream fileStream = mock(InputStream.class);
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attachmentUuid);
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        attachmentService.addAttachment(
            roomId,
            fileStream,
            "image/png",
            2048L,
            "pic.png",
            "description",
            "xmpp-msg-abc",
            null,
            null,
            currentUser);
      }
    }

    @Test
    @DisplayName("Re throws an exception if the room is not found")
    void addAttachment_testErrorRoomNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      InputStream fileStream = mock(InputStream.class);
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false))
          .thenThrow(new NotFoundException());
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attachmentUuid);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        assertThrows(
            NotFoundException.class,
            () ->
                attachmentService.addAttachment(
                    roomId,
                    fileStream,
                    "application/pdf",
                    1024L,
                    "temp.pdf",
                    "description",
                    null,
                    null,
                    null,
                    currentUser));
      }

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService, storagesService, fileMetadataRepository);
    }

    @Test
    @DisplayName("Re throws an exception if the user is not a member of the room")
    void addAttachment_testErrorForbiddenException() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      InputStream fileStream = mock(InputStream.class);
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false))
          .thenThrow(new ForbiddenException());
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attachmentUuid);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        assertThrows(
            ForbiddenException.class,
            () ->
                attachmentService.addAttachment(
                    roomId,
                    fileStream,
                    "application/pdf",
                    1024L,
                    "temp.pdf",
                    "description",
                    null,
                    null,
                    null,
                    currentUser));
      }

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService, storagesService, fileMetadataRepository);
    }

    @Test
    @DisplayName("Re throws an exception if storages service throws an exception")
    void addAttachment_testStorageException() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      InputStream fileStream = mock(InputStream.class);
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);
      doThrow(new StorageException())
          .when(storagesService)
          .saveFile(fileStream, attachmentUuid.toString(), user1Id.toString(), 1024L);
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attachmentUuid);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        assertThrows(
            StorageException.class,
            () ->
                attachmentService.addAttachment(
                    roomId,
                    fileStream,
                    "application/pdf",
                    1024L,
                    "temp.pdf",
                    "description",
                    "",
                    null,
                    null,
                    currentUser));
      }

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verify(storagesService, times(1))
          .saveFile(fileStream, attachmentUuid.toString(), currentUser.getId(), 1024L);
      verifyNoMoreInteractions(roomService, storagesService, fileMetadataRepository);
    }
  }

  @Nested
  @DisplayName("Copy attachment tests")
  class CopyAttachmentTests {

    @Test
    @DisplayName("Copies the attachment and returns it")
    void copyAttachment_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      UUID originalAttachmentId = UUID.randomUUID();
      FileMetadataBuilder metadataBuilder =
          FileMetadataBuilder.create()
              .id(originalAttachmentId.toString())
              .name("temp.pdf")
              .originalSize(1024L)
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user1Id.toString())
              .roomId(roomId.toString());
      FileMetadata fileMetadata = metadataBuilder.build();
      when(fileMetadataRepository.getById(originalAttachmentId.toString()))
          .thenReturn(Optional.of(fileMetadata));
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);
      UUID attachmentUuid = UUID.randomUUID();
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attachmentUuid);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);
        attachmentService.copyAttachment(room2, originalAttachmentId, currentUser);
      }

      verify(fileMetadataRepository, times(1)).getById(originalAttachmentId.toString());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verify(storagesService, times(1))
          .copyFile(
              originalAttachmentId.toString(),
              user1Id.toString(),
              attachmentUuid.toString(),
              user1Id.toString());
      ArgumentCaptor<FileMetadata> fileMetadataArgumentCaptor =
          ArgumentCaptor.forClass(FileMetadata.class);
      verify(fileMetadataRepository, times(1)).save(fileMetadataArgumentCaptor.capture());
      FileMetadata copiedFileMetadata = fileMetadataArgumentCaptor.getValue();
      assertEquals(attachmentUuid.toString(), copiedFileMetadata.getId());
      assertEquals("temp.pdf", copiedFileMetadata.getName());
      assertEquals(1024L, copiedFileMetadata.getOriginalSize());
      assertEquals("application/pdf", copiedFileMetadata.getMimeType());
      assertEquals(FileMetadataType.ATTACHMENT, copiedFileMetadata.getType());
      assertEquals(user1Id.toString(), copiedFileMetadata.getUserId());
      assertEquals(roomId.toString(), copiedFileMetadata.getRoomId());
      verifyNoMoreInteractions(roomService, storagesService, fileMetadataRepository);
    }

    @Test
    @DisplayName("Re throws an exception if the room is not found")
    void copyAttachment_testErrorRoomNotFound() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      UUID originalAttachmentId = UUID.randomUUID();
      FileMetadataBuilder metadataBuilder =
          FileMetadataBuilder.create()
              .id(originalAttachmentId.toString())
              .name("temp.pdf")
              .originalSize(1024L)
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user1Id.toString())
              .roomId(roomId.toString());
      FileMetadata fileMetadata = metadataBuilder.build();
      when(fileMetadataRepository.getById(originalAttachmentId.toString()))
          .thenReturn(Optional.of(fileMetadata));
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false))
          .thenThrow(NotFoundException.class);
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);

        assertThrows(
            NotFoundException.class,
            () -> attachmentService.copyAttachment(room2, originalAttachmentId, currentUser));
      }

      verify(fileMetadataRepository, times(1)).getById(originalAttachmentId.toString());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService, storagesService, fileMetadataRepository);
    }

    @Test
    @DisplayName("Re throws an exception if the member is not part of the room")
    void copyAttachment_testErrorForbiddenException() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      UUID originalAttachmentId = UUID.randomUUID();
      FileMetadataBuilder metadataBuilder =
          FileMetadataBuilder.create()
              .id(originalAttachmentId.toString())
              .name("temp.pdf")
              .originalSize(1024L)
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user1Id.toString())
              .roomId(roomId.toString());
      FileMetadata fileMetadata = metadataBuilder.build();
      when(fileMetadataRepository.getById(originalAttachmentId.toString()))
          .thenReturn(Optional.of(fileMetadata));
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false))
          .thenThrow(ForbiddenException.class);
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);

        assertThrows(
            ForbiddenException.class,
            () -> attachmentService.copyAttachment(room2, originalAttachmentId, currentUser));
      }

      verify(fileMetadataRepository, times(1)).getById(originalAttachmentId.toString());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService, storagesService, fileMetadataRepository);
    }

    @Test
    @DisplayName("Re throws an exception if storage service fails")
    void copyAttachment_testErrorStorageException() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      UUID originalAttachmentId = UUID.randomUUID();
      FileMetadataBuilder metadataBuilder =
          FileMetadataBuilder.create()
              .id(originalAttachmentId.toString())
              .name("temp.pdf")
              .originalSize(1024L)
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user1Id.toString())
              .roomId(roomId.toString());
      FileMetadata fileMetadata = metadataBuilder.build();
      when(fileMetadataRepository.getById(originalAttachmentId.toString()))
          .thenReturn(Optional.of(fileMetadata));
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);
      UUID attachmentUuid = UUID.randomUUID();
      doThrow(StorageException.class)
          .when(storagesService)
          .copyFile(
              originalAttachmentId.toString(),
              user1Id.toString(),
              attachmentUuid.toString(),
              user1Id.toString());
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attachmentUuid);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        uuid.when(() -> UUID.fromString(roomId.toString())).thenReturn(roomId);

        assertThrows(
            StorageException.class,
            () -> attachmentService.copyAttachment(room2, originalAttachmentId, currentUser));
      }

      verify(fileMetadataRepository, times(1)).getById(originalAttachmentId.toString());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verify(storagesService, times(1))
          .copyFile(
              originalAttachmentId.toString(),
              user1Id.toString(),
              attachmentUuid.toString(),
              user1Id.toString());
      verifyNoMoreInteractions(roomService, storagesService, fileMetadataRepository);
    }
  }

  @Nested
  @DisplayName("Delete attachment tests")
  class DeleteAttachmentTests {

    @Test
    @DisplayName("Correctly deletes the attachment by its owner")
    void deleteAttachment_testOkByAttachmentOwner() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user2Id);
      FileMetadata expectedMetadata =
          FileMetadataBuilder.create()
              .id(attachmentUuid.toString())
              .name("temp.pdf")
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user2Id.toString())
              .roomId(roomId.toString())
              .build();
      when(fileMetadataRepository.getById(attachmentUuid.toString()))
          .thenReturn(Optional.of(expectedMetadata));
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);

      attachmentService.deleteAttachment(attachmentUuid, currentUser);

      verify(fileMetadataRepository, times(1)).getById(attachmentUuid.toString());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verify(storagesService, times(1)).deleteFile(attachmentUuid.toString(), user2Id.toString());
      verify(fileMetadataRepository, times(1)).delete(expectedMetadata);
      verifyNoMoreInteractions(fileMetadataRepository, roomService, storagesService);
    }

    @Test
    @DisplayName("Throws a not found exception if the file was not found")
    void deleteAttachment_testFileNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.empty());

      assertThrows(
          NotFoundException.class,
          () -> attachmentService.deleteAttachment(attachmentUuid, currentUser));

      verify(fileMetadataRepository, times(1)).getById(attachmentUuid.toString());
      verifyNoMoreInteractions(fileMetadataRepository, roomService, storagesService);
    }

    @Test
    @DisplayName("Re throws the exception if the room was not found")
    void deleteAttachment_testRoomNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadata expectedMetadata =
          FileMetadataBuilder.create()
              .id(attachmentUuid.toString())
              .name("temp.pdf")
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user2Id.toString())
              .roomId(roomId.toString())
              .build();
      when(fileMetadataRepository.getById(attachmentUuid.toString()))
          .thenReturn(Optional.of(expectedMetadata));
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false))
          .thenThrow(new NotFoundException());

      assertThrows(
          NotFoundException.class,
          () -> attachmentService.deleteAttachment(attachmentUuid, currentUser));

      verify(fileMetadataRepository, times(1)).getById(attachmentUuid.toString());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoMoreInteractions(fileMetadataRepository, roomService, storagesService);
    }

    @Test
    @DisplayName("Re throws an exception when storages throws an exception")
    void deleteAttachment_testStoragesThrowsError() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user2Id);
      FileMetadata expectedMetadata =
          FileMetadataBuilder.create()
              .id(attachmentUuid.toString())
              .name("temp.pdf")
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user2Id.toString())
              .roomId(roomId.toString())
              .build();
      when(fileMetadataRepository.getById(attachmentUuid.toString()))
          .thenReturn(Optional.of(expectedMetadata));
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);
      doThrow(new StorageException())
          .when(storagesService)
          .deleteFile(attachmentUuid.toString(), user2Id.toString());

      assertThrows(
          StorageException.class,
          () -> attachmentService.deleteAttachment(attachmentUuid, currentUser));

      verify(fileMetadataRepository, times(1)).getById(attachmentUuid.toString());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verify(storagesService, times(1)).deleteFile(attachmentUuid.toString(), user2Id.toString());
      verifyNoMoreInteractions(fileMetadataRepository, roomService, storagesService);
    }

    @Test
    @DisplayName("Throws a forbidden exception if authenticated user isn't the attachment owner")
    void deleteAttachment_testAuthenticatedUserIsNotAttachmentOwner() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user3Id);
      FileMetadata expectedMetadata =
          FileMetadataBuilder.create()
              .id(attachmentUuid.toString())
              .name("temp.pdf")
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user2Id.toString())
              .roomId(roomId.toString())
              .build();
      when(fileMetadataRepository.getById(attachmentUuid.toString()))
          .thenReturn(Optional.of(expectedMetadata));
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);

      assertThrows(
          ForbiddenException.class,
          () -> attachmentService.deleteAttachment(attachmentUuid, currentUser));
    }
  }

  @Nested
  @DisplayName("Get attachment info by room id with filters tests")
  class GetAttachmentInfoWithFiltersTests {

    @Test
    @DisplayName("Returns only attachments uploaded by the specified user")
    void getAttachmentInfoByRoomId_withUserIdFilter() {
      UUID file1Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime ts = OffsetDateTime.now();
      AttachmentFilter filter = AttachmentFilter.create().userId(user2Id.toString());
      when(fileMetadataRepository.getByRoomIdAndType(
              eq(roomId.toString()),
              eq(FileMetadataType.ATTACHMENT),
              eq(11),
              isNull(),
              any(AttachmentFilter.class)))
          .thenReturn(
              List.of(
                  FileMetadataBuilder.create()
                      .id(file1Id.toString())
                      .name("image1.jpg")
                      .originalSize(1024L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user2Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(ts)
                      .updatedAt(ts)
                      .build()));
      when(fileMetadataRepository.countByRoomIdAndType(
              eq(roomId.toString()), eq(FileMetadataType.ATTACHMENT), any(AttachmentFilter.class)))
          .thenReturn(1L);

      AttachmentsPaginationDto result =
          attachmentService.getAttachmentInfoByRoomId(roomId, 10, null, filter, currentUser);

      assertEquals(1, result.getAttachments().size());
      assertEquals(1L, result.getTotal());
      assertEquals(user2Id, result.getAttachments().get(0).getUserId());
      assertNull(result.getCursor());
    }

    @Test
    @DisplayName("Returns only attachments matching the exact MIME type")
    void getAttachmentInfoByRoomId_withMimeTypeExactFilter() {
      UUID file1Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime ts = OffsetDateTime.now();
      AttachmentFilter attachFilter = AttachmentFilter.create().mimeType("application/pdf");
      when(fileMetadataRepository.getByRoomIdAndType(
              eq(roomId.toString()),
              eq(FileMetadataType.ATTACHMENT),
              eq(11),
              isNull(),
              any(AttachmentFilter.class)))
          .thenReturn(
              List.of(
                  FileMetadataBuilder.create()
                      .id(file1Id.toString())
                      .name("doc.pdf")
                      .originalSize(2048L)
                      .mimeType("application/pdf")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(ts)
                      .updatedAt(ts)
                      .build()));

      AttachmentsPaginationDto result =
          attachmentService.getAttachmentInfoByRoomId(roomId, 10, null, attachFilter, currentUser);

      assertEquals(1, result.getAttachments().size());
      assertEquals("application/pdf", result.getAttachments().get(0).getMimeType());
    }

    @Test
    @DisplayName("Returns attachments within the specified timestamp range")
    void getAttachmentInfoByRoomId_withTimestampRange() {
      UUID file1Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime base = OffsetDateTime.now();
      AttachmentFilter attachFilter =
          AttachmentFilter.create().createdAfter(base.minusDays(1)).createdBefore(base.plusDays(1));
      when(fileMetadataRepository.getByRoomIdAndType(
              eq(roomId.toString()),
              eq(FileMetadataType.ATTACHMENT),
              eq(11),
              isNull(),
              any(AttachmentFilter.class)))
          .thenReturn(
              List.of(
                  FileMetadataBuilder.create()
                      .id(file1Id.toString())
                      .name("image.jpg")
                      .originalSize(1024L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(base)
                      .updatedAt(base)
                      .build()));

      AttachmentsPaginationDto result =
          attachmentService.getAttachmentInfoByRoomId(roomId, 10, null, attachFilter, currentUser);

      assertEquals(1, result.getAttachments().size());
    }

    @Test
    @DisplayName("Returns attachments within the specified size range")
    void getAttachmentInfoByRoomId_withSizeRange() {
      UUID file1Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime ts = OffsetDateTime.now();
      AttachmentFilter attachFilter = AttachmentFilter.create().minSize(1000L).maxSize(5000L);
      when(fileMetadataRepository.getByRoomIdAndType(
              eq(roomId.toString()),
              eq(FileMetadataType.ATTACHMENT),
              eq(11),
              isNull(),
              any(AttachmentFilter.class)))
          .thenReturn(
              List.of(
                  FileMetadataBuilder.create()
                      .id(file1Id.toString())
                      .name("image.jpg")
                      .originalSize(2048L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(ts)
                      .updatedAt(ts)
                      .build()));

      AttachmentsPaginationDto result =
          attachmentService.getAttachmentInfoByRoomId(roomId, 10, null, attachFilter, currentUser);

      assertEquals(1, result.getAttachments().size());
      assertEquals(2048L, result.getAttachments().get(0).getSize());
    }

    @Test
    @DisplayName("Returns attachments sorted by size descending and encodes size-based cursor")
    void getAttachmentInfoByRoomId_sortBySizeDescEncodesCursor() throws Exception {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      UUID file3Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime ts = OffsetDateTime.now();
      AttachmentFilter attachFilter = AttachmentFilter.create().sortBy("size").order("desc");
      when(fileMetadataRepository.getByRoomIdAndType(
              eq(roomId.toString()),
              eq(FileMetadataType.ATTACHMENT),
              eq(3),
              isNull(),
              any(AttachmentFilter.class)))
          .thenReturn(
              List.of(
                  FileMetadataBuilder.create()
                      .id(file1Id.toString())
                      .name("a.jpg")
                      .originalSize(3000L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(ts)
                      .updatedAt(ts)
                      .build(),
                  FileMetadataBuilder.create()
                      .id(file2Id.toString())
                      .name("b.jpg")
                      .originalSize(2000L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(ts)
                      .updatedAt(ts)
                      .build(),
                  FileMetadataBuilder.create()
                      .id(file3Id.toString())
                      .name("c.jpg")
                      .originalSize(1000L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(ts)
                      .updatedAt(ts)
                      .build()));

      AttachmentsPaginationDto result =
          attachmentService.getAttachmentInfoByRoomId(roomId, 2, null, attachFilter, currentUser);

      assertEquals(2, result.getAttachments().size());
      assertNotNull(result.getCursor());
      String expectedCursor =
          Base64.getEncoder()
              .encodeToString(
                  objectMapper.writeValueAsBytes(
                      PaginationFilter.create(file2Id.toString(), null, 2000L, "size", "desc")));
      assertEquals(expectedCursor, result.getCursor());
    }

    @Test
    @DisplayName("Returns empty list with no filter when no attachments match")
    void getAttachmentInfoByRoomId_withFilterNoResults() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      AttachmentFilter attachFilter = AttachmentFilter.create().mimeType("video/mp4");
      when(fileMetadataRepository.getByRoomIdAndType(
              eq(roomId.toString()),
              eq(FileMetadataType.ATTACHMENT),
              eq(11),
              isNull(),
              any(AttachmentFilter.class)))
          .thenReturn(List.of());

      AttachmentsPaginationDto result =
          attachmentService.getAttachmentInfoByRoomId(roomId, 10, null, attachFilter, currentUser);

      assertEquals(0, result.getAttachments().size());
      assertNull(result.getCursor());
    }

    @Test
    @DisplayName(
        "Throws BadRequestException when cursor sort params do not match current sort params")
    void getAttachmentInfoByRoomId_cursorSortMismatchThrowsBadRequest() throws Exception {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      UUID someId = UUID.randomUUID();
      String cursor =
          Base64.getEncoder()
              .encodeToString(
                  objectMapper.writeValueAsBytes(
                      PaginationFilter.create(someId.toString(), null, 2000L, "size", "desc")));
      AttachmentFilter attachFilter = AttachmentFilter.create().sortBy("created_at").order("desc");
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);

      assertThrows(
          BadRequestException.class,
          () ->
              attachmentService.getAttachmentInfoByRoomId(
                  roomId, 10, cursor, attachFilter, currentUser));

      verifyNoInteractions(fileMetadataRepository);
    }

    @Test
    @DisplayName("Throws BadRequestException for a malformed pagination cursor")
    void getAttachmentInfoByRoomId_malformedCursorThrowsBadRequest() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      String cursor = Base64.getEncoder().encodeToString("not-valid-json".getBytes());
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);

      assertThrows(
          BadRequestException.class,
          () -> attachmentService.getAttachmentInfoByRoomId(roomId, 10, cursor, null, currentUser));

      verifyNoInteractions(fileMetadataRepository);
    }

    @Test
    @DisplayName("Accepts a legacy cursor without sort fields skipping sort mismatch validation")
    void getAttachmentInfoByRoomId_legacyCursorWithoutSortFieldsSkipsValidation() throws Exception {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      UUID someId = UUID.randomUUID();
      OffsetDateTime ts = OffsetDateTime.now();
      String cursor =
          Base64.getEncoder()
              .encodeToString(
                  objectMapper.writeValueAsBytes(
                      PaginationFilter.create(someId.toString(), ts, null)));
      AttachmentFilter attachFilter = AttachmentFilter.create().sortBy("created_at").order("desc");
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);
      when(fileMetadataRepository.getByRoomIdAndType(
              eq(roomId.toString()),
              eq(FileMetadataType.ATTACHMENT),
              eq(11),
              any(PaginationFilter.class),
              any(AttachmentFilter.class)))
          .thenReturn(List.of());

      AttachmentsPaginationDto result =
          attachmentService.getAttachmentInfoByRoomId(
              roomId, 10, cursor, attachFilter, currentUser);

      assertEquals(0, result.getAttachments().size());
      assertNull(result.getCursor());
    }

    @Test
    @DisplayName("Encodes ascending size-based cursor with asc sort context")
    void getAttachmentInfoByRoomId_sortBySizeAscEncodesCursor() throws Exception {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      UUID file3Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime ts = OffsetDateTime.now();
      AttachmentFilter attachFilter = AttachmentFilter.create().sortBy("size").order("asc");
      when(fileMetadataRepository.getByRoomIdAndType(
              eq(roomId.toString()),
              eq(FileMetadataType.ATTACHMENT),
              eq(3),
              isNull(),
              any(AttachmentFilter.class)))
          .thenReturn(
              List.of(
                  FileMetadataBuilder.create()
                      .id(file1Id.toString())
                      .name("small.jpg")
                      .originalSize(500L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(ts)
                      .updatedAt(ts)
                      .build(),
                  FileMetadataBuilder.create()
                      .id(file2Id.toString())
                      .name("medium.jpg")
                      .originalSize(1000L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(ts)
                      .updatedAt(ts)
                      .build(),
                  FileMetadataBuilder.create()
                      .id(file3Id.toString())
                      .name("large.jpg")
                      .originalSize(2000L)
                      .mimeType("image/jpg")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(ts)
                      .updatedAt(ts)
                      .build()));

      AttachmentsPaginationDto result =
          attachmentService.getAttachmentInfoByRoomId(roomId, 2, null, attachFilter, currentUser);

      assertEquals(2, result.getAttachments().size());
      assertNotNull(result.getCursor());
      String expectedCursor =
          Base64.getEncoder()
              .encodeToString(
                  objectMapper.writeValueAsBytes(
                      PaginationFilter.create(file2Id.toString(), null, 1000L, "size", "asc")));
      assertEquals(expectedCursor, result.getCursor());
    }

    @Test
    @DisplayName("Returns attachments matching a MIME type prefix filter")
    void getAttachmentInfoByRoomId_withMimeTypePrefixFilter() {
      UUID file1Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime ts = OffsetDateTime.now();
      AttachmentFilter attachFilter = AttachmentFilter.create().mimeType("image/");
      when(fileMetadataRepository.getByRoomIdAndType(
              eq(roomId.toString()),
              eq(FileMetadataType.ATTACHMENT),
              eq(11),
              isNull(),
              any(AttachmentFilter.class)))
          .thenReturn(
              List.of(
                  FileMetadataBuilder.create()
                      .id(file1Id.toString())
                      .name("photo.png")
                      .originalSize(1024L)
                      .mimeType("image/png")
                      .type(FileMetadataType.ATTACHMENT)
                      .userId(user1Id.toString())
                      .roomId(roomId.toString())
                      .createdAt(ts)
                      .updatedAt(ts)
                      .build()));

      AttachmentsPaginationDto result =
          attachmentService.getAttachmentInfoByRoomId(roomId, 10, null, attachFilter, currentUser);

      assertEquals(1, result.getAttachments().size());
      assertEquals("image/png", result.getAttachments().get(0).getMimeType());
      assertNull(result.getCursor());
    }
  }

  @Nested
  @DisplayName("Bulk delete room attachments tests")
  class BulkDeleteRoomAttachmentsTests {

    @Test
    @DisplayName("Room member successfully bulk-deletes their own attachments in the room")
    void bulkDeleteRoomAttachments_testOk() {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user2Id);
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);
      when(fileMetadataRepository.getIdsByIdsAndRoomIdAndUserId(
              List.of(file1Id.toString(), file2Id.toString()),
              roomId.toString(),
              user2Id.toString()))
          .thenReturn(List.of(file1Id.toString(), file2Id.toString()));
      when(storagesService.deleteFileList(
              List.of(file1Id.toString(), file2Id.toString()), user2Id.toString()))
          .thenReturn(List.of());

      BulkDeleteAttachmentsResponseDto result =
          attachmentService.bulkDeleteRoomAttachments(
              roomId, List.of(file1Id, file2Id), currentUser);

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verify(fileMetadataRepository, times(1))
          .getIdsByIdsAndRoomIdAndUserId(
              List.of(file1Id.toString(), file2Id.toString()),
              roomId.toString(),
              user2Id.toString());
      verify(storagesService, times(1))
          .deleteFileList(List.of(file1Id.toString(), file2Id.toString()), user2Id.toString());
      verify(fileMetadataRepository, times(1))
          .deleteByIds(List.of(file1Id.toString(), file2Id.toString()));
      verifyNoMoreInteractions(roomService, fileMetadataRepository, storagesService);
      assertEquals(List.of(file1Id, file2Id), result.getSuccessIds());
      assertEquals(List.of(), result.getFailedIds());
    }

    @Test
    @DisplayName("Returns partial success when some attachments fail to delete from storage")
    void bulkDeleteRoomAttachments_partialFailure() {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user2Id);
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);
      when(fileMetadataRepository.getIdsByIdsAndRoomIdAndUserId(
              List.of(file1Id.toString(), file2Id.toString()),
              roomId.toString(),
              user2Id.toString()))
          .thenReturn(List.of(file1Id.toString(), file2Id.toString()));
      when(storagesService.deleteFileList(
              List.of(file1Id.toString(), file2Id.toString()), user2Id.toString()))
          .thenReturn(List.of(file2Id.toString()));

      BulkDeleteAttachmentsResponseDto result =
          attachmentService.bulkDeleteRoomAttachments(
              roomId, List.of(file1Id, file2Id), currentUser);

      verify(fileMetadataRepository, times(1)).deleteByIds(List.of(file1Id.toString()));
      assertEquals(List.of(file1Id), result.getSuccessIds());
      assertEquals(List.of(file2Id), result.getFailedIds());
    }

    @Test
    @DisplayName("Throws ForbiddenException when user is not a room member")
    void bulkDeleteRoomAttachments_testNotMember() {
      UUID file1Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user3Id);
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false))
          .thenThrow(new ForbiddenException());

      assertThrows(
          ForbiddenException.class,
          () -> attachmentService.bulkDeleteRoomAttachments(roomId, List.of(file1Id), currentUser));

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, currentUser, false);
      verifyNoInteractions(fileMetadataRepository, storagesService);
    }

    @Test
    @DisplayName(
        "Throws ForbiddenException when attachments are not found, not in room, or not owned")
    void bulkDeleteRoomAttachments_testAccessDenied() {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user2Id);
      when(roomService.getRoomAndValidateUser(roomId, currentUser, false)).thenReturn(room1);
      when(fileMetadataRepository.getIdsByIdsAndRoomIdAndUserId(
              List.of(file1Id.toString(), file2Id.toString()),
              roomId.toString(),
              user2Id.toString()))
          .thenReturn(List.of(file1Id.toString()));

      assertThrows(
          ForbiddenException.class,
          () ->
              attachmentService.bulkDeleteRoomAttachments(
                  roomId, List.of(file1Id, file2Id), currentUser));

      verify(fileMetadataRepository, times(1))
          .getIdsByIdsAndRoomIdAndUserId(
              List.of(file1Id.toString(), file2Id.toString()),
              roomId.toString(),
              user2Id.toString());
      verifyNoInteractions(storagesService);
      verifyNoMoreInteractions(fileMetadataRepository);
    }
  }

  @Nested
  @DisplayName("Delete attachments by room id tests")
  class DeleteAttachmentsByRoomIdTests {

    @Test
    @DisplayName("Correctly deletes all room attachments when storage succeeds for all")
    void deleteAttachmentsByRoomId_testOk() {
      String file1Id = UUID.randomUUID().toString();
      String file2Id = UUID.randomUUID().toString();

      when(fileMetadataRepository.getIdsByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT))
          .thenReturn(List.of(file1Id, file2Id));
      when(storagesService.deleteFileList(List.of(file1Id, file2Id), user1Id.toString()))
          .thenReturn(List.of());

      attachmentService.deleteAttachmentsByRoomId(roomId, UserPrincipal.create(user1Id));

      verify(fileMetadataRepository, times(1))
          .getIdsByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT);
      verify(storagesService, times(1))
          .deleteFileList(List.of(file1Id, file2Id), user1Id.toString());
      verify(fileMetadataRepository, times(1)).deleteByIds(List.of(file1Id, file2Id));
      verifyNoMoreInteractions(storagesService, fileMetadataRepository);
    }

    @Test
    @DisplayName("Only deletes DB records for attachments successfully deleted from storage")
    void deleteAttachmentsByRoomId_partialStorageFailure() {
      String file1Id = UUID.randomUUID().toString();
      String file2Id = UUID.randomUUID().toString();

      when(fileMetadataRepository.getIdsByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT))
          .thenReturn(List.of(file1Id, file2Id));
      when(storagesService.deleteFileList(List.of(file1Id, file2Id), user1Id.toString()))
          .thenReturn(List.of(file2Id));

      attachmentService.deleteAttachmentsByRoomId(roomId, UserPrincipal.create(user1Id));

      verify(fileMetadataRepository, times(1))
          .getIdsByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT);
      verify(storagesService, times(1))
          .deleteFileList(List.of(file1Id, file2Id), user1Id.toString());
      verify(fileMetadataRepository, times(1)).deleteByIds(List.of(file1Id));
      verifyNoMoreInteractions(storagesService, fileMetadataRepository);
    }

    @Test
    @DisplayName("Correctly deletes no attachments if storage service fails")
    void deleteAttachmentsByRoomId_storageServiceFailed() {
      String file1Id = UUID.randomUUID().toString();
      String file2Id = UUID.randomUUID().toString();

      when(fileMetadataRepository.getIdsByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT))
          .thenReturn(List.of(file1Id, file2Id));
      when(storagesService.deleteFileList(List.of(file1Id, file2Id), user1Id.toString()))
          .thenThrow(StorageException.class);

      attachmentService.deleteAttachmentsByRoomId(roomId, UserPrincipal.create(user1Id));

      verify(fileMetadataRepository, times(1))
          .getIdsByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT);
      verify(storagesService, times(1))
          .deleteFileList(List.of(file1Id, file2Id), user1Id.toString());
      verifyNoMoreInteractions(storagesService, fileMetadataRepository);
    }
  }
}
