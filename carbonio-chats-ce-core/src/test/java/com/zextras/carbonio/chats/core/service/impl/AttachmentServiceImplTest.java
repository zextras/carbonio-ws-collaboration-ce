package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zextras.carbonio.chats.api.RFC3339DateFormat;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadataBuilder;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.AttachmentAddedEvent;
import com.zextras.carbonio.chats.core.data.event.AttachmentRemovedEvent;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.model.PaginationFilter;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.AttachmentsPaginationDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@UnitTest
public class AttachmentServiceImplTest {

  private final AttachmentService      attachmentService;
  private final FileMetadataRepository fileMetadataRepository;
  private final StoragesService        storagesService;
  private final RoomService            roomService;
  private final EventDispatcher        eventDispatcher;
  private final ObjectMapper           objectMapper;

  @TempDir
  private Path tempDir;

  public AttachmentServiceImplTest(AttachmentMapper attachmentMapper) {
    this.fileMetadataRepository = mock(FileMetadataRepository.class);
    this.storagesService = mock(StoragesService.class);
    this.roomService = mock(RoomService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .setDateFormat(new RFC3339DateFormat());
    ;
    this.attachmentService = new AttachmentServiceImpl(
      this.fileMetadataRepository,
      attachmentMapper,
      this.storagesService,
      this.roomService,
      this.eventDispatcher,
      this.objectMapper);
  }

  private static UUID user1Id;
  private static UUID user2Id;
  private static UUID user3Id;
  private static UUID roomId;
  private static Room room;


  @BeforeAll
  public static void initAll() {
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    user3Id = UUID.randomUUID();
    roomId = UUID.randomUUID();
    room = Room.create();
    room
      .id(roomId.toString())
      .type(RoomTypeDto.GROUP)
      .name("room1")
      .description("Room one")
      .subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false),
        Subscription.create(room, user3Id.toString()).owner(false)));
  }

  @Nested
  @DisplayName("Gets attachment info by room id tests")
  public class GetsAllRoomAttachmentInfoTests {

    @Test
    @DisplayName("Returns a single page of attachments info of the required room")
    public void getAttachmentInfoByRoomId_testOkSinglePage() {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime attachmentTimestamp = OffsetDateTime.now();
      when(
        fileMetadataRepository.getByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT, 11, null))
        .thenReturn(List.of(
          FileMetadataBuilder.create().id(file1Id.toString()).name("image1.jpg")
            .originalSize(0L).mimeType("image/jpg").type(FileMetadataType.ATTACHMENT)
            .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(attachmentTimestamp)
            .updatedAt(attachmentTimestamp),
          FileMetadataBuilder.create().id(file2Id.toString()).name("pdf1.pdf")
            .originalSize(0L).mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
            .userId(user2Id.toString()).roomId(roomId.toString()).createdAt(attachmentTimestamp.plusHours(1))
            .updatedAt(attachmentTimestamp.plusHours(1))));
      AttachmentsPaginationDto attachmentsPagination = attachmentService.getAttachmentInfoByRoomId(roomId, 10, null,
        currentUser);

      assertEquals(2, attachmentsPagination.getAttachments().size());
      assertEquals(file1Id, attachmentsPagination.getAttachments().get(0).getId());
      assertEquals(file2Id, attachmentsPagination.getAttachments().get(1).getId());
      assertNull(attachmentsPagination.getFilter());
      verify(roomService, times(1)).getRoomAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

    @Test
    @DisplayName("Returns first page of attachments info of the required room")
    public void getAttachmentInfoByRoomId_testOkFirstPage() throws Exception {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      UUID file3Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime attachmentTimestamp = OffsetDateTime.now();
      when(
        fileMetadataRepository.getByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT, 3, null))
        .thenReturn(List.of(
          FileMetadataBuilder.create().id(file1Id.toString()).name("image1.jpg")
            .originalSize(0L).mimeType("image/jpg").type(FileMetadataType.ATTACHMENT)
            .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(attachmentTimestamp.plusHours(2))
            .updatedAt(attachmentTimestamp.plusHours(2)),
          FileMetadataBuilder.create().id(file2Id.toString()).name("image2.jpg")
            .originalSize(0L).mimeType("image/jpg").type(FileMetadataType.ATTACHMENT)
            .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(attachmentTimestamp.plusHours(1))
            .updatedAt(attachmentTimestamp.plusHours(1)),
          FileMetadataBuilder.create().id(file3Id.toString()).name("pdf1.pdf")
            .originalSize(0L).mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
            .userId(user2Id.toString()).roomId(roomId.toString()).createdAt(attachmentTimestamp)
            .updatedAt(attachmentTimestamp)));
      AttachmentsPaginationDto attachmentsPagination = attachmentService.getAttachmentInfoByRoomId(roomId, 2, null,
        currentUser);

      assertEquals(2, attachmentsPagination.getAttachments().size());
      assertEquals(file1Id, attachmentsPagination.getAttachments().get(0).getId());
      assertEquals(file2Id, attachmentsPagination.getAttachments().get(1).getId());
      assertNotNull(attachmentsPagination.getFilter());
      String expectedFilter = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(
        PaginationFilter.create(file2Id.toString(), attachmentTimestamp.plusHours(1))));
      assertEquals(expectedFilter, attachmentsPagination.getFilter());
      verify(roomService, times(1)).getRoomAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

    @Test
    @DisplayName("Returns last page of attachments info of the required room")
    public void getAttachmentInfoByRoomId_testOkLastPage() throws Exception {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime attachmentTimestamp = OffsetDateTime.now();
      PaginationFilter paginationFilter = PaginationFilter.create(file1Id.toString(), attachmentTimestamp.plusHours(1));
      String filter = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(paginationFilter));
      when(
        fileMetadataRepository.getByRoomIdAndType(eq(roomId.toString()), eq(FileMetadataType.ATTACHMENT), eq(3),
          any(PaginationFilter.class)))
        .thenReturn(List.of(
          FileMetadataBuilder.create().id(file2Id.toString()).name("pdf1.pdf")
            .originalSize(0L).mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
            .userId(user2Id.toString()).roomId(roomId.toString()).createdAt(attachmentTimestamp)
            .updatedAt(attachmentTimestamp)));

      AttachmentsPaginationDto attachmentsPagination = attachmentService.getAttachmentInfoByRoomId(roomId, 2,
        filter, currentUser);

      assertEquals(1, attachmentsPagination.getAttachments().size());
      assertEquals(file2Id, attachmentsPagination.getAttachments().get(0).getId());
      assertNull(attachmentsPagination.getFilter());
      verify(roomService, times(1)).getRoomAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

    @Test
    @DisplayName("Given a room identifier, if authenticated user isn't a room member then throws a 'forbidden' exception")
    public void getAttachmentInfoByRoomId_testAuthenticatedUserIsNotARoomMember() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false))
        .thenThrow(new ForbiddenException(
          String.format("User '%s' is not a member of room '%s'", currentUser.getId(), roomId)));

      assertThrows(ForbiddenException.class, () ->
        attachmentService.getAttachmentInfoByRoomId(roomId, 10, null, currentUser));
    }

    @Test
    @DisplayName("Re throws the exception if the room was not found")
    public void getAttachmentInfoByRoomId_testRoomNotFound() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false))
        .thenThrow(new NotFoundException());

      NotFoundException notFoundException = assertThrows(NotFoundException.class, () ->
        attachmentService.getAttachmentInfoByRoomId(roomId, 10, null, currentUser));
      assertEquals(String.format("Not Found - Not Found"), notFoundException.getMessage());
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns an empty list when there isn't any attachment of the required room")
    public void getAttachmentInfoByRoomId_testNoAttachment() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(
        fileMetadataRepository.getByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT, 11, null))
        .thenReturn(List.of());

      AttachmentsPaginationDto attachmentsPagination = attachmentService.getAttachmentInfoByRoomId(roomId, 10, null,
        currentUser);

      assertNotNull(attachmentsPagination);
      assertEquals(0, attachmentsPagination.getAttachments().size());
      assertNull(attachmentsPagination.getFilter());
      verify(roomService, times(1)).getRoomAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }
  }

  @Nested
  @DisplayName("Get attachment by id tests")
  class GetAttachmentByIdTests {

    @Test
    @DisplayName("Retrieves the specified attachments")
    public void getAttachmentById_testOk() throws Exception {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user2Id);

      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(
        FileMetadataBuilder.create().id(attachmentUuid.toString()).name("image1.jpg")
          .originalSize(0L).mimeType("image/jpg").type(FileMetadataType.ATTACHMENT)
          .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
          .updatedAt(OffsetDateTime.now()))
      );
      when(storagesService.getFileById(attachmentUuid.toString(), user1Id.toString()))
        .thenReturn(Files.createFile(tempDir.resolve("temp.txt")).toFile());
      FileContentAndMetadata attachmentById = attachmentService.getAttachmentById(attachmentUuid, currentUser);

      assertNotNull(attachmentById);
      assertNotNull(attachmentById.getFile());
      assertNotNull(attachmentById.getMetadata());
      assertEquals(attachmentUuid.toString(), attachmentById.getMetadata().getId());
      assertEquals("temp.txt", attachmentById.getFile().getName());
      verify(roomService, times(1)).getRoomAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

    @Test
    @DisplayName("Throws an exception if the attachment is not found in our local db")
    public void getAttachmentById_testNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.empty());

      NotFoundException notFoundException = assertThrows(NotFoundException.class,
        () -> attachmentService.getAttachmentById(attachmentUuid, currentUser));
      assertEquals(String.format("Not Found - File with id '%s' not found", attachmentUuid),
        notFoundException.getMessage());
    }

    @Test
    @DisplayName("Re throws an exception if the user is not part of the room")
    public void getAttachmentById_testForbidden() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(
        FileMetadataBuilder.create().id(attachmentUuid.toString()).name("image1.jpg")
          .originalSize(0L).mimeType("image/jpg").type(FileMetadataType.ATTACHMENT)
          .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
          .updatedAt(OffsetDateTime.now()))
      );
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenThrow(new ForbiddenException());

      assertThrows(ForbiddenException.class, () -> attachmentService.getAttachmentById(attachmentUuid, currentUser));
    }

    @Test
    @DisplayName("Re throws an exception if the room is not found")
    public void getAttachmentById_testRoomNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(
        FileMetadataBuilder.create().id(attachmentUuid.toString()).name("image1.jpg")
          .originalSize(0L).mimeType("image/jpg").type(FileMetadataType.ATTACHMENT)
          .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
          .updatedAt(OffsetDateTime.now()))
      );
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenThrow(new NotFoundException());

      NotFoundException notFoundException = assertThrows(NotFoundException.class,
        () -> attachmentService.getAttachmentById(attachmentUuid, currentUser));
      assertEquals(String.format("Not Found - Not Found"), notFoundException.getMessage());
    }

    @Test
    @DisplayName("Re throws an exception if the file is not found")
    public void getAttachmentById_testFileNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(
        FileMetadataBuilder.create().id(attachmentUuid.toString()).name("image1.jpg")
          .originalSize(0L).mimeType("image/jpg").type(FileMetadataType.ATTACHMENT)
          .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
          .updatedAt(OffsetDateTime.now()))
      );
      when(storagesService.getFileById(attachmentUuid.toString(), user1Id.toString()))
        .thenThrow(new InternalErrorException());
      assertThrows(InternalErrorException.class,
        () -> attachmentService.getAttachmentById(attachmentUuid, currentUser));
      verify(roomService, times(1)).getRoomAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

  }

  @Nested
  @DisplayName("Get attachment preview by id tests")
  class GetAttachmentPreviewByIdTests {

    @Test
    @Disabled
    @DisplayName("Returns the attachment preview")
    public void getAttachmentPreviewById_testOk() throws Exception {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadataBuilder metadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("test.pdf")
        .originalSize(0L).mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
        .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now());
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(metadata));
      when(storagesService.getPreview(metadata, user1Id.toString()))
        .thenReturn(Files.createFile(tempDir.resolve("temp.txt")).toFile());
      File attachment = attachmentService.getAttachmentPreviewById(attachmentUuid,
        currentUser);

      assertNotNull(attachment);
      assertEquals("temp.txt", attachment.getName());

      verify(roomService, times(1)).getRoomAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

    @Test
    @DisplayName("Exception if the file was not found in our repository")
    public void getAttachmentPreviewById_testNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.empty());
      NotFoundException notFoundException = assertThrows(NotFoundException.class,
        () -> attachmentService.getAttachmentPreviewById(attachmentUuid, currentUser));
      assertEquals(String.format("Not Found - File with id '%s' not found", attachmentUuid),
        notFoundException.getMessage());
    }

    @Test
    @DisplayName("Re throws the exception if the user is not part of the room")
    public void getAttachmentPreviewById_testForbidden() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadataBuilder metadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("test.pdf")
        .originalSize(0L).mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
        .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now());
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(metadata));
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenThrow(new ForbiddenException());

      assertThrows(ForbiddenException.class,
        () -> attachmentService.getAttachmentPreviewById(attachmentUuid, currentUser));
    }

    @Test
    @DisplayName("Re throws the exception if the user is not part of the room")
    public void getAttachmentPreviewById_testRoomNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadataBuilder metadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("test.pdf")
        .originalSize(0L).mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
        .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now());
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(metadata));
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenThrow(new NotFoundException());

      NotFoundException notFoundException = assertThrows(NotFoundException.class,
        () -> attachmentService.getAttachmentPreviewById(attachmentUuid, currentUser));
      assertEquals(String.format("Not Found - Not Found"), notFoundException.getMessage());
    }

    @Test
    @DisplayName("Re throws the exception if storages throws an exception")
    public void getAttachmentPreviewById_testStorageError() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadataBuilder metadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("test.pdf")
        .originalSize(0L).mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
        .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now());
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(metadata));
      when(storagesService.getPreview(metadata, user1Id.toString())).thenThrow(new InternalErrorException());

      assertThrows(InternalErrorException.class,
        () -> attachmentService.getAttachmentPreviewById(attachmentUuid, currentUser));
      verify(roomService, times(1)).getRoomAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

  }

  @Nested
  @DisplayName("Get attachment info by id")
  class GetAttachmentInfoByIdTests {

    @Test
    @DisplayName("Returns the attachment info")
    public void getAttachmentInfoById_testOk() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadataBuilder metadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("test.pdf")
        .originalSize(0L).mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
        .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now());
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(metadata));

      AttachmentDto attachmentInfo = attachmentService.getAttachmentInfoById(attachmentUuid, currentUser);

      assertNotNull(attachmentInfo);
      assertEquals(attachmentUuid, attachmentInfo.getId());
      verify(roomService, times(1)).getRoomAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

    @Test
    @DisplayName("Throws an exception if the file is not found")
    public void getAttachmentInfoById_testNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.empty());

      NotFoundException notFoundException = assertThrows(NotFoundException.class,
        () -> attachmentService.getAttachmentInfoById(attachmentUuid, currentUser));
      assertEquals(String.format("Not Found - File with id '%s' not found", attachmentUuid),
        notFoundException.getMessage());
    }

    @Test
    @DisplayName("Re throws an exception if the user is not a room member")
    public void getAttachmentInfoById_testForbidden() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadataBuilder metadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("test.pdf")
        .originalSize(0L).mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
        .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now());
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(metadata));
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenThrow(new ForbiddenException());

      assertThrows(ForbiddenException.class,
        () -> attachmentService.getAttachmentInfoById(attachmentUuid, currentUser));
    }

    @Test
    @DisplayName("Re throws an exception if the room is not found")
    public void getAttachmentInfoById_testRoomNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadataBuilder metadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("test.pdf")
        .originalSize(0L).mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
        .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now());
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(metadata));
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenThrow(new NotFoundException());

      NotFoundException notFoundException = assertThrows(NotFoundException.class,
        () -> attachmentService.getAttachmentInfoById(attachmentUuid, currentUser));
      assertEquals(String.format("Not Found - Not Found"), notFoundException.getMessage());
    }

  }

  @Nested
  @DisplayName("Add attachment tests")
  class AddAttachmentTests {

    @Test
    @DisplayName("Creates the attachment and returns it")
    public void addAttachment_testOk() throws Exception {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      File attachmentFile = tempDir.resolve("temp.pdf").toFile();
      Files.writeString(attachmentFile.toPath(), "test!");
      FileMetadataBuilder expectedMetadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("temp.pdf").originalSize(attachmentFile.length()).mimeType("application/pdf")
        .type(FileMetadataType.ATTACHMENT).userId(user1Id.toString()).roomId(roomId.toString());
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attachmentUuid);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        attachmentService.addAttachment(roomId, attachmentFile, "application/pdf", "temp.pdf", currentUser);
      }

      verify(storagesService, times(1)).saveFile(attachmentFile, expectedMetadata, currentUser.toString());
      verifyNoMoreInteractions(storagesService);
      verify(fileMetadataRepository, times(1)).save(expectedMetadata);
      verifyNoMoreInteractions(fileMetadataRepository);
      verify(roomService, times(1)).getRoomAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
      verify(eventDispatcher, times(1))
        .sendToTopic(user1Id, roomId.toString(), AttachmentAddedEvent.create(roomId).from(user1Id));
      verifyNoMoreInteractions(eventDispatcher);
    }

    @Test
    @DisplayName("Re throws an exception if the user is not a member of the room")
    public void addAttachment_testRoomNotFound() throws Exception {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      File attachmentFile = tempDir.resolve("temp.pdf").toFile();
      Files.writeString(attachmentFile.toPath(), "test!");
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenThrow(new NotFoundException());
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attachmentUuid);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        NotFoundException notFoundException = assertThrows(NotFoundException.class,
          () -> attachmentService.addAttachment(roomId, attachmentFile, "application/pdf", "temp.pdf", currentUser));
        assertEquals(String.format("Not Found - Not Found"), notFoundException.getMessage());
      }
    }

    @Test
    @DisplayName("Re throws an exception if the user is not a member of the room")
    public void addAttachment_testForbidden() throws Exception {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      File attachmentFile = tempDir.resolve("temp.pdf").toFile();
      Files.writeString(attachmentFile.toPath(), "test!");
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenThrow(new ForbiddenException());
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attachmentUuid);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        assertThrows(ForbiddenException.class,
          () -> attachmentService.addAttachment(roomId, attachmentFile, "application/pdf", "temp.pdf", currentUser));
      }
    }

    @Test
    @DisplayName("Re throws an exception if storages throws an exception")
    public void addAttachment_testInternalException() throws Exception {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      File attachmentFile = tempDir.resolve("temp.pdf").toFile();
      Files.writeString(attachmentFile.toPath(), "test!");
      FileMetadataBuilder expectedMetadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("temp.pdf").originalSize(attachmentFile.length()).mimeType("application/pdf")
        .type(FileMetadataType.ATTACHMENT).userId(user1Id.toString()).roomId(roomId.toString());
      doThrow(new InternalErrorException()).when(storagesService)
        .saveFile(attachmentFile, expectedMetadata, user1Id.toString());
      try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
        uuid.when(UUID::randomUUID).thenReturn(attachmentUuid);
        uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
        assertThrows(InternalErrorException.class,
          () -> attachmentService.addAttachment(roomId, attachmentFile, "application/pdf", "temp.pdf", currentUser));
      }
    }

  }

  @Nested
  @DisplayName("Delete attachment tests")
  class DeleteAttachmentTests {

    @Test
    @DisplayName("Correctly deletes the attachment by its owner")
    public void deleteAttachment_testOkByAttachmentOwner() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user2Id);
      FileMetadataBuilder expectedMetadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("temp.pdf").mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
        .userId(user2Id.toString()).roomId(roomId.toString());
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(expectedMetadata));
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenReturn(room);

      attachmentService.deleteAttachment(attachmentUuid, currentUser);

      verify(fileMetadataRepository, times(1)).delete(expectedMetadata);
      verify(fileMetadataRepository, times(1)).getById(attachmentUuid.toString());
      verifyNoMoreInteractions(fileMetadataRepository);
      verify(storagesService, times(1)).deleteFile(attachmentUuid.toString(), user2Id.toString());
      verifyNoMoreInteractions(storagesService);
      verify(eventDispatcher, times(1)).sendToTopic(user2Id, roomId.toString(),
        AttachmentRemovedEvent.create(roomId).from(user2Id));
    }

    @Test
    @DisplayName("Correctly deletes the attachment by room owner")
    public void deleteAttachment_testOkByRoomOwner() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadataBuilder expectedMetadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("temp.pdf").mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
        .userId(user2Id.toString()).roomId(roomId.toString());
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(expectedMetadata));
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenReturn(room);

      attachmentService.deleteAttachment(attachmentUuid, currentUser);

      verify(fileMetadataRepository, times(1)).delete(expectedMetadata);
      verify(fileMetadataRepository, times(1)).getById(attachmentUuid.toString());
      verifyNoMoreInteractions(fileMetadataRepository);
      verify(storagesService, times(1)).deleteFile(attachmentUuid.toString(), user2Id.toString());
      verifyNoMoreInteractions(storagesService);
      verify(eventDispatcher, times(1)).sendToTopic(user1Id, roomId.toString(),
        AttachmentRemovedEvent.create(roomId).from(user1Id));
    }

    @Test
    @DisplayName("Throws a not found exception if the file was not found")
    public void deleteAttachment_testFileNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.empty());

      NotFoundException notFoundException = assertThrows(NotFoundException.class,
        () -> attachmentService.deleteAttachment(attachmentUuid, currentUser));
      assertEquals(String.format("Not Found - File with id '%s' not found", attachmentUuid),
        notFoundException.getMessage());
    }

    @Test
    @DisplayName("Re throws the exception if the room was not found")
    public void deleteAttachment_testRoomNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadataBuilder expectedMetadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("temp.pdf").mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
        .userId(user2Id.toString()).roomId(roomId.toString());
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(expectedMetadata));
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenThrow(new NotFoundException());

      NotFoundException notFoundException = assertThrows(NotFoundException.class,
        () -> attachmentService.deleteAttachment(attachmentUuid, currentUser));
      assertEquals(String.format("Not Found - Not Found"), notFoundException.getMessage());
    }

    @Test
    @DisplayName("Re throws an exception when storages throws an exception")
    public void deleteAttachment_testStoragesThrowsError() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      FileMetadataBuilder expectedMetadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("temp.pdf").mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
        .userId(user2Id.toString()).roomId(roomId.toString());
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(expectedMetadata));
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenReturn(room);
      doThrow(new InternalErrorException()).when(storagesService)
        .deleteFile(attachmentUuid.toString(), user2Id.toString());

      assertThrows(InternalErrorException.class, () -> attachmentService.deleteAttachment(attachmentUuid, currentUser));
    }

    @Test
    @DisplayName("Throws a forbidden exception if authenticated user isn't attachment owner or room owner")
    public void deleteAttachment_testAuthenticatedUserIsNotAttachmentOwnerOrRoomOwner() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user3Id);
      FileMetadataBuilder expectedMetadata = FileMetadataBuilder.create().id(attachmentUuid.toString())
        .name("temp.pdf").mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
        .userId(user2Id.toString()).roomId(roomId.toString());
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.of(expectedMetadata));
      when(roomService.getRoomAndCheckUser(roomId, currentUser, false)).thenReturn(room);

      assertThrows(ForbiddenException.class, () -> attachmentService.deleteAttachment(attachmentUuid, currentUser));
    }
  }
}
