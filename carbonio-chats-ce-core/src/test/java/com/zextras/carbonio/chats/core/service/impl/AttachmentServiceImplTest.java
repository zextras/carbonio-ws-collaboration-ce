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
import com.zextras.carbonio.chats.core.data.entity.FileMetadataBuilder;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.OrderDirection;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.swing.SortOrder;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class AttachmentServiceImplTest {

  private final AttachmentService      attachmentService;
  private final FileMetadataRepository fileMetadataRepository;
  private final StoragesService        storagesService;
  private final RoomService            roomService;
  private final EventDispatcher        eventDispatcher;

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
  @DisplayName("Gets all room attachment info tests")
  public class GetsAllRoomAttachmentInfoTests {

    @Test
    @DisplayName("Given a room identifier, correctly returns all attachments info of the required room")
    public void getAttachmentInfoByRoomId_testOk() throws Exception {
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      when(
        fileMetadataRepository.getByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT, OrderDirection.DESC))
        .thenReturn(List.of(
          FileMetadataBuilder.create().id(file1Id.toString()).name("image1.jpg")
            .originalSize(0L).mimeType("image/jpg").type(FileMetadataType.ATTACHMENT)
            .userId(user1Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now()),
          FileMetadataBuilder.create().id(file2Id.toString()).name("pdf1.pdf")
            .originalSize(0L).mimeType("application/pdf").type(FileMetadataType.ATTACHMENT)
            .userId(user2Id.toString()).roomId(roomId.toString()).createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())));
      List<AttachmentDto> attachmentInfo = attachmentService.getAttachmentInfoByRoomId(roomId,
        UserPrincipal.create(user1Id));

      assertEquals(2, attachmentInfo.size());
      assertTrue(attachmentInfo.stream().anyMatch(attach -> file1Id.equals(attach.getId())));
      assertTrue(attachmentInfo.stream().anyMatch(attach -> file2Id.equals(attach.getId())));
      verify(fileMetadataRepository, times(1))
        .getByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT, OrderDirection.DESC);
      verifyNoMoreInteractions(fileMetadataRepository);
    }

    @Test
    @DisplayName("Given a room identifier, if authenticated user isn't a room member then throws a 'forbidden' exception")
    public void getAttachmentInfoByRoomId_testAuthenticatedUserIsNotARoomMember() {
      UserPrincipal currentUser = UserPrincipal.create(UUID.randomUUID());
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
      when(
        fileMetadataRepository.getByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT, OrderDirection.DESC))
        .thenReturn(List.of());

      List<AttachmentDto> attachmentInfo = attachmentService.getAttachmentInfoByRoomId(roomId,
        UserPrincipal.create(user1Id));

      assertNotNull(attachmentInfo);
      assertEquals(0, attachmentInfo.size());
      verify(fileMetadataRepository, times(1))
        .getByRoomIdAndType(roomId.toString(), FileMetadataType.ATTACHMENT, OrderDirection.DESC);
      verifyNoMoreInteractions(fileMetadataRepository);
    }

  }


}
