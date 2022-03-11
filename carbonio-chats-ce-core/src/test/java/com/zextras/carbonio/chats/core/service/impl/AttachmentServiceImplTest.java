package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.FileMetadataBuilder;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.OrderDirection;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
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
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@UnitTest
public class AttachmentServiceImplTest {

  private final AttachmentService      attachmentService;
  private final FileMetadataRepository fileMetadataRepository;
  private final StoragesService        storagesService;
  private final RoomService            roomService;
  private final EventDispatcher        eventDispatcher;

  @TempDir
  private Path tempDir;

  public AttachmentServiceImplTest(AttachmentMapper attachmentMapper) {
    this.fileMetadataRepository = mock(FileMetadataRepository.class);
    this.storagesService = mock(StoragesService.class);
    this.roomService = mock(RoomService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.attachmentService = new AttachmentServiceImpl(
      this.fileMetadataRepository,
      attachmentMapper,
      this.storagesService,
      this.roomService,
      this.eventDispatcher);
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
    @DisplayName("Returns all attachments info of the required room")
    public void getAttachmentInfoByRoomId_testOk() throws Exception {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      OffsetDateTime attachmentTimestamp = OffsetDateTime.now();
      when(
        fileMetadataRepository.getByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT, OrderDirection.DESC))
        .thenReturn(List.of(
          FileMetadataBuilder.create().id(file1Id.toString()).name("image1.jpg")
            .originalSize(0L).mimeType("image/jpg").type(FileMetadataType.ATTACHMENT)
            .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(attachmentTimestamp)
            .updatedAt(attachmentTimestamp), //TODO try to solve this
          FileMetadataBuilder.create().id(file2Id.toString()).name("pdf1.pdf")
            .originalSize(0L).mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
            .userId(user2Id.toString()).roomId(roomId.toString()).createdAt(attachmentTimestamp.plusHours(1))
            .updatedAt(attachmentTimestamp.plusHours(1))));
      List<AttachmentDto> attachmentInfo = attachmentService.getAttachmentInfoByRoomId(roomId, currentUser);

      assertEquals(2, attachmentInfo.size());
      assertEquals(file1Id, attachmentInfo.get(0).getId());
      assertEquals(file2Id, attachmentInfo.get(1).getId());
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

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        attachmentService.getAttachmentInfoByRoomId(roomId, currentUser));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", currentUser.getId(), roomId),
        exception.getMessage());
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns an empty list when there isn't any attachment of the required room")
    public void getAttachmentInfoByRoomId_testNoAttachment() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(
        fileMetadataRepository.getByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT, OrderDirection.DESC))
        .thenReturn(List.of());

      List<AttachmentDto> attachmentInfo = attachmentService.getAttachmentInfoByRoomId(roomId, currentUser);

      assertNotNull(attachmentInfo);
      assertEquals(0, attachmentInfo.size());
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

      assertThrows(NotFoundException.class, () -> attachmentService.getAttachmentById(attachmentUuid, currentUser));
    }

    @Test
    @DisplayName("Re throws an exception if the user is not part of the room")
    public void getAttachmentById_testForbidden() throws Exception {
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
    @DisplayName("Re throws an exception if the file is not found")
    public void getAttachmentById_testFileNotFound() throws Exception {
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
      FileContentAndMetadata attachmentPreviewById = attachmentService.getAttachmentPreviewById(attachmentUuid,
        currentUser);

      assertNotNull(attachmentPreviewById);
      assertNotNull(attachmentPreviewById.getMetadata());
      assertNotNull(attachmentPreviewById.getFile());
      assertEquals(attachmentUuid.toString(), attachmentPreviewById.getMetadata().getId());
      assertEquals("image/jpeg", attachmentPreviewById.getMetadata().getMimeType());
      assertEquals("temp.txt", attachmentPreviewById.getFile().getName());

      verify(roomService, times(1)).getRoomAndCheckUser(roomId, currentUser, false);
      verifyNoMoreInteractions(roomService);
    }

    @Test
    @DisplayName("Exception if the file was not found in our repository")
    public void getAttachmentPreviewById_testNotFound() {
      UUID attachmentUuid = UUID.randomUUID();
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(fileMetadataRepository.getById(attachmentUuid.toString())).thenReturn(Optional.empty());
      assertThrows(NotFoundException.class,
        () -> attachmentService.getAttachmentPreviewById(attachmentUuid, currentUser));
    }

    @Test
    @DisplayName("Re throws the exception if the user is not part of the room")
    public void getAttachmentPreviewById_testForbidden() throws Exception {
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
    @DisplayName("Re throws the exception if storages throws an exception")
    public void getAttachmentPreviewById_testStorageError() throws Exception {
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


}
