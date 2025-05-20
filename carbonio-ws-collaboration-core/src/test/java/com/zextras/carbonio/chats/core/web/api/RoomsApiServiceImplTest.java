// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.ForwardMessageDto;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@UnitTest
class RoomsApiServiceImplTest {

  private final RoomsApiService roomsApiService;
  private final RoomService roomService;
  private final MembersService membersService;
  private final AttachmentService attachmentService;
  private final MeetingService meetingService;
  private final SecurityContext securityContext;

  public RoomsApiServiceImplTest() {
    this.securityContext = mock(SecurityContext.class);
    this.roomService = mock(RoomService.class);
    this.membersService = mock(MembersService.class);
    this.attachmentService = mock(AttachmentService.class);
    this.meetingService = mock(MeetingService.class);
    this.roomsApiService =
        new RoomsApiServiceImpl(roomService, membersService, attachmentService, meetingService);
  }

  private UUID user2Id;

  private UUID roomId;

  private UserPrincipal user1;
  private UserPrincipal guest;

  private String genericFileName;
  private String snoopyFileName;

  @BeforeEach
  void init() {
    UUID guestId = UUID.randomUUID();
    UUID user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();

    user1 = UserPrincipal.create(user1Id).userType(UserType.INTERNAL);
    guest = UserPrincipal.create(guestId).userType(UserType.GUEST);

    roomId = UUID.randomUUID();

    genericFileName = StringFormatUtils.encodeToUtf8("fileName");
    snoopyFileName = StringFormatUtils.encodeToUtf8("snoopy-image");
  }

  @AfterEach
  void afterEach() {
    verifyNoMoreInteractions(roomService, membersService, attachmentService, meetingService);
    reset(roomService, membersService, attachmentService, meetingService, securityContext);
  }

  @Nested
  @DisplayName("List rooms tests")
  class ListRoomsTest {

    @Test
    @DisplayName("List rooms with with an authenticated user without extra fields")
    void listRooms_testAuthenticatedUser_without_extra_fields() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.listRooms(null, securityContext);

      verify(roomService, times(1)).getRooms(null, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("List rooms with with a guest user without extra fields")
    void listRooms_testGuestUser_without_extra_fields() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      Response response = roomsApiService.listRooms(null, securityContext);

      verify(roomService, times(1)).getRooms(null, guest);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("List rooms with with a non-valid user without extra fields")
    void listRooms_testUnauthorizedUser_without_extra_fields() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class, () -> roomsApiService.listRooms(null, securityContext));
    }
  }

  @Nested
  @DisplayName("Get room tests")
  class GetRoomTest {

    @Test
    @DisplayName("Get room with with an authenticated user")
    void getRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.getRoom(roomId, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Get room with with a guest user")
    void getRoom_testGuestUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      Response response = roomsApiService.getRoom(roomId, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, guest);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Get room with with a non-valid user")
    void getRoom_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class, () -> roomsApiService.getRoom(roomId, securityContext));
    }
  }

  @Nested
  @DisplayName("Insert room tests")
  class InsertRoomTest {

    @Test
    @DisplayName("Insert room with with an authenticated user")
    void insertRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create().type(RoomTypeDto.GROUP).name("test");
      Response response = roomsApiService.insertRoom(roomCreationFieldsDto, securityContext);

      verify(roomService, times(1)).createRoom(roomCreationFieldsDto, user1);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room with with an authenticated user with name for one to one")
    void insertRoom_testAuthenticatedUser_oneToOneWithName() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create().type(RoomTypeDto.ONE_TO_ONE).name("test");
      Response response = roomsApiService.insertRoom(roomCreationFieldsDto, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room with with an authenticated user with description for one to one")
    void insertRoom_testAuthenticatedUser_oneToOneWithDescription() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create().type(RoomTypeDto.ONE_TO_ONE).description("test");
      Response response = roomsApiService.insertRoom(roomCreationFieldsDto, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room with with an authenticated user without name for group")
    void insertRoom_testAuthenticatedUser_groupWithoutName() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create().type(RoomTypeDto.GROUP);
      Response response = roomsApiService.insertRoom(roomCreationFieldsDto, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room with with an authenticated user without name for temporary room")
    void insertRoom_testAuthenticatedUser_temporaryWithoutName() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create().type(RoomTypeDto.TEMPORARY);
      Response response = roomsApiService.insertRoom(roomCreationFieldsDto, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room with with a guest user")
    void insertRoom_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create().type(RoomTypeDto.GROUP).name("test");

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.insertRoom(roomCreationFieldsDto, securityContext));
    }

    @Test
    @DisplayName("Insert room with with a non-valid user")
    void insertRoom_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create().type(RoomTypeDto.GROUP).name("test");

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.insertRoom(roomCreationFieldsDto, securityContext));
    }
  }

  @Nested
  @DisplayName("Delete room tests")
  class DeleteRoomTest {

    @Test
    @DisplayName("Delete room with with an authenticated user")
    void deleteRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.GROUP));

      Response response = roomsApiService.deleteRoom(roomId, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);
      verify(roomService, times(1)).deleteRoom(roomId, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Delete room with with an authenticated user for one to one")
    void deleteRoom_testAuthenticatedUser_oneToOne() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.ONE_TO_ONE));

      Response response = roomsApiService.deleteRoom(roomId, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Delete room with with an authenticated user but it's not found")
    void deleteRoom_testAuthenticatedUser_notFound() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1)).thenReturn(null);

      Response response = roomsApiService.deleteRoom(roomId, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Delete room with with a guest user")
    void deleteRoom_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      assertThrows(
          ForbiddenException.class, () -> roomsApiService.deleteRoom(roomId, securityContext));
    }

    @Test
    @DisplayName("Delete room with with a non-valid user")
    void deleteRoom_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class, () -> roomsApiService.deleteRoom(roomId, securityContext));
    }
  }

  @Nested
  @DisplayName("Update room tests")
  class UpdateRoomTest {

    @Test
    @DisplayName("Update room with with an authenticated user")
    void updateRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.GROUP));

      RoomEditableFieldsDto roomEditableFieldsDto = RoomEditableFieldsDto.create().name("test123");
      Response response =
          roomsApiService.updateRoom(roomId, roomEditableFieldsDto, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);
      verify(roomService, times(1)).updateRoom(roomId, roomEditableFieldsDto, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Update room with with an authenticated user for one to one")
    void updateRoom_testAuthenticatedUser_oneToOne() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.ONE_TO_ONE));

      RoomEditableFieldsDto roomEditableFieldsDto = RoomEditableFieldsDto.create().name("test123");
      Response response =
          roomsApiService.updateRoom(roomId, roomEditableFieldsDto, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Update room with with an authenticated user without name and description")
    void updateRoom_testAuthenticatedUser_withoutNameAndDescription() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.GROUP));

      RoomEditableFieldsDto roomEditableFieldsDto = RoomEditableFieldsDto.create();
      Response response =
          roomsApiService.updateRoom(roomId, roomEditableFieldsDto, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Update room with with an authenticated user but it's not found")
    void updateRoom_testAuthenticatedUser_notFound() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1)).thenReturn(null);

      RoomEditableFieldsDto roomEditableFieldsDto = RoomEditableFieldsDto.create().name("test123");
      Response response =
          roomsApiService.updateRoom(roomId, roomEditableFieldsDto, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Update room with with a guest user")
    void updateRoom_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      RoomEditableFieldsDto roomEditableFieldsDto = RoomEditableFieldsDto.create().name("test123");

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.updateRoom(roomId, roomEditableFieldsDto, securityContext));
    }

    @Test
    @DisplayName("Update room with with a non-valid user")
    void updateRoom_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      RoomEditableFieldsDto roomEditableFieldsDto = RoomEditableFieldsDto.create().name("test123");

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.updateRoom(roomId, roomEditableFieldsDto, securityContext));
    }
  }

  @Nested
  @DisplayName("Update room owners tests")
  class UpdateRoomOwnersTest {

    @Test
    @DisplayName("Update room owners with with an authenticated user")
    void updateRoomOwners_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      List<MemberDto> memberDtos = List.of(MemberDto.create().userId(user2Id).owner(true));
      Response response = roomsApiService.updateRoomOwners(roomId, memberDtos, securityContext);

      verify(membersService, times(1)).updateRoomOwners(roomId, memberDtos, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Update room owners with with a guest user")
    void updateRoomOwners_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      List<MemberDto> memberDtos = List.of(MemberDto.create().userId(user2Id).owner(true));

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.updateRoomOwners(roomId, memberDtos, securityContext));
    }

    @Test
    @DisplayName("Update room owners with with a non-valid user")
    void updateRoomOwners_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      List<MemberDto> memberDtos = List.of(MemberDto.create().userId(user2Id).owner(true));

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.updateRoomOwners(roomId, memberDtos, securityContext));
    }
  }

  @Nested
  @DisplayName("Get room picture tests")
  class GetRoomPictureTest {

    @Test
    @DisplayName("Get room picture with with an authenticated user")
    void getRoomPicture_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      FileContentAndMetadata contentAndMetadata = mock(FileContentAndMetadata.class);
      FileMetadata fileMetadata = mock(FileMetadata.class);
      when(contentAndMetadata.getMetadata()).thenReturn(fileMetadata);
      when(fileMetadata.getMimeType()).thenReturn("image/jpeg");
      when(roomService.getRoomPicture(roomId, user1)).thenReturn(contentAndMetadata);

      Response response = roomsApiService.getRoomPicture(roomId, securityContext);

      verify(roomService, times(1)).getRoomPicture(roomId, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Get room picture with with a guest user")
    void getRoomPicture_testGuestUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(guest);
      FileContentAndMetadata contentAndMetadata = mock(FileContentAndMetadata.class);
      FileMetadata fileMetadata = mock(FileMetadata.class);
      when(contentAndMetadata.getMetadata()).thenReturn(fileMetadata);
      when(fileMetadata.getMimeType()).thenReturn("image/jpeg");
      when(roomService.getRoomPicture(roomId, guest)).thenReturn(contentAndMetadata);

      Response response = roomsApiService.getRoomPicture(roomId, securityContext);

      verify(roomService, times(1)).getRoomPicture(roomId, guest);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Get room picture with with a non-valid user")
    void getRoomPicture_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.getRoomPicture(roomId, securityContext));
    }
  }

  @Nested
  @DisplayName("Update room picture tests")
  class UpdateRoomPictureTest {

    @Test
    @DisplayName("Update room picture with an authenticated user")
    void updateRoomPicture_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      InputStream imageInputStream = mock(InputStream.class);
      Response response =
          roomsApiService.updateRoomPicture(
              roomId,
              snoopyFileName,
              "image/jpeg",
              256L,
              imageInputStream,
              securityContext);

      verify(roomService, times(1))
          .setRoomPicture(roomId, imageInputStream, "image/jpeg", 256L, "snoopy-image", user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Update room picture with an authenticated user without file name")
    void updateRoomPicture_testAuthenticatedUser_fileNameNotFound() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.updateRoomPicture(
                  roomId, null, "image/jpeg", 256L, mock(InputStream.class), securityContext));
    }

    @Test
    @DisplayName("Update room picture with an authenticated user with file name not encoded")
    void updateRoomPicture_testAuthenticatedUser_fileNameNotEncoded() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.updateRoomPicture(
                  roomId,
                  "snoopy-image",
                  "image/jpeg",
                  256L,
                  mock(InputStream.class),
                  securityContext));
    }

    @Test
    @DisplayName("Update room picture with an authenticated user without mime type")
    void updateRoomPicture_testAuthenticatedUser_mimeTypeNotFound() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.updateRoomPicture(
                  roomId,
                  snoopyFileName,
                  null,
                  256L,
                  mock(InputStream.class),
                  securityContext));
    }

    @Test
    @DisplayName("Update room picture with a guest user")
    void updateRoomPicture_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      assertThrows(
          ForbiddenException.class,
          () ->
              roomsApiService.updateRoomPicture(
                  roomId,
                  snoopyFileName,
                  "image/jpeg",
                  256L,
                  mock(InputStream.class),
                  securityContext));
    }

    @Test
    @DisplayName("Update room picture with a non-valid user")
    void updateRoomPicture_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () ->
              roomsApiService.updateRoomPicture(
                  roomId,
                  snoopyFileName,
                  "image/jpeg",
                  256L,
                  mock(InputStream.class),
                  securityContext));
    }
  }

  @Nested
  @DisplayName("Delete room picture tests")
  class DeleteRoomPictureTest {

    @Test
    @DisplayName("Delete room picture with an authenticated user")
    void deleteRoomPicture_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.deleteRoomPicture(roomId, securityContext);

      verify(roomService, times(1)).deleteRoomPicture(roomId, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Delete room picture with a guest user")
    void deleteRoomPicture_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.deleteRoomPicture(roomId, securityContext));
    }

    @Test
    @DisplayName("Delete room picture with a non-valid user")
    void deleteRoomPicture_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.deleteRoomPicture(roomId, securityContext));
    }
  }

  @Nested
  @DisplayName("Forward messages tests")
  class ForwardMessagesTest {

    @Test
    @DisplayName("Forward messages with an authenticated user")
    void forwardMessages_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      List<ForwardMessageDto> forwardMessageDtos =
          List.of(ForwardMessageDto.create().originalMessage("original-message"));
      Response response =
          roomsApiService.forwardMessages(roomId, forwardMessageDtos, securityContext);

      verify(roomService, times(1)).forwardMessages(roomId, forwardMessageDtos, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Forward messages with a guest user")
    void forwardMessages_testGuestUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      List<ForwardMessageDto> forwardMessageDtos =
          List.of(ForwardMessageDto.create().originalMessage("original-message"));
      Response response =
          roomsApiService.forwardMessages(roomId, forwardMessageDtos, securityContext);

      verify(roomService, times(1)).forwardMessages(roomId, forwardMessageDtos, guest);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Forward messages with a non-valid user")
    void forwardMessages_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      List<ForwardMessageDto> forwardMessageDtos =
          List.of(ForwardMessageDto.create().originalMessage("original-message"));

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.forwardMessages(roomId, forwardMessageDtos, securityContext));
    }
  }

  @Nested
  @DisplayName("Mute room tests")
  class MuteRoomTest {

    @Test
    @DisplayName("Mute room with an authenticated user")
    void muteRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.muteRoom(roomId, securityContext);

      verify(roomService, times(1)).muteRoom(roomId, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Mute room with a guest user")
    void muteRoom_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      assertThrows(
          ForbiddenException.class, () -> roomsApiService.muteRoom(roomId, securityContext));
    }

    @Test
    @DisplayName("Mute room with a non-valid user")
    void muteRoom_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class, () -> roomsApiService.muteRoom(roomId, securityContext));
    }
  }

  @Nested
  @DisplayName("Unmute room tests")
  class UnmuteRoomTest {

    @Test
    @DisplayName("Unmute room with an authenticated user")
    void unmuteRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.unmuteRoom(roomId, securityContext);

      verify(roomService, times(1)).unmuteRoom(roomId, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Unmute room with a guest user")
    void unmuteRoom_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      assertThrows(
          ForbiddenException.class, () -> roomsApiService.unmuteRoom(roomId, securityContext));
    }

    @Test
    @DisplayName("Unmute room with a non-valid user")
    void unmuteRoom_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class, () -> roomsApiService.unmuteRoom(roomId, securityContext));
    }
  }

  @Nested
  @DisplayName("Clear room history tests")
  class ClearRoomHistoryTest {

    @Test
    @DisplayName("Clear room history with an authenticated user")
    void clearRoomHistory_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.clearRoomHistory(roomId, securityContext);

      verify(roomService, times(1)).clearRoomHistory(roomId, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Clear room history with a guest user")
    void clearRoomHistory_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.clearRoomHistory(roomId, securityContext));
    }

    @Test
    @DisplayName("Clear room history with a non-valid user")
    void clearRoomHistory_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.clearRoomHistory(roomId, securityContext));
    }
  }

  @Nested
  @DisplayName("List room members tests")
  class ListRoomMembersTest {

    @Test
    @DisplayName("List room members with an authenticated user")
    void listRoomMembers_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.listRoomMembers(roomId, securityContext);

      verify(membersService, times(1)).getRoomMembers(roomId, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("List room members with a guest user")
    void listRoomMembers_testGuestUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      Response response = roomsApiService.listRoomMembers(roomId, securityContext);

      verify(membersService, times(1)).getRoomMembers(roomId, guest);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("List room members with a non-valid user")
    void listRoomMembers_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.listRoomMembers(roomId, securityContext));
    }
  }

  @Nested
  @DisplayName("Insert room members tests")
  class InsertRoomMembersTest {

    @Test
    @DisplayName("Insert room members with an authenticated user")
    void insertRoomMembers_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      List<MemberToInsertDto> memberToInsertDtos =
          List.of(MemberToInsertDto.create().userId(user2Id));
      Response response =
          roomsApiService.insertRoomMembers(roomId, memberToInsertDtos, securityContext);

      verify(membersService, times(1)).insertRoomMembers(roomId, memberToInsertDtos, user1);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room members with a guest user")
    void insertRoomMembers_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      List<MemberToInsertDto> memberToInsertDtos =
          List.of(MemberToInsertDto.create().userId(user2Id));

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.insertRoomMembers(roomId, memberToInsertDtos, securityContext));
    }

    @Test
    @DisplayName("Insert room members with a non-valid user")
    void insertRoomMembers_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      List<MemberToInsertDto> memberToInsertDtos =
          List.of(MemberToInsertDto.create().userId(user2Id));

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.insertRoomMembers(roomId, memberToInsertDtos, securityContext));
    }
  }

  @Nested
  @DisplayName("Delete room member tests")
  class DeleteRoomMemberTest {

    @Test
    @DisplayName("Delete room member with an authenticated user")
    void deleteRoomMember_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.deleteRoomMember(roomId, user2Id, securityContext);

      verify(membersService, times(1)).deleteRoomMember(roomId, user2Id, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Delete room member with a guest user")
    void deleteRoomMember_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.deleteRoomMember(roomId, user2Id, securityContext));
    }

    @Test
    @DisplayName("Delete room member with a non-valid user")
    void deleteRoomMember_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.deleteRoomMember(roomId, user2Id, securityContext));
    }
  }

  @Nested
  @DisplayName("Insert owner tests")
  class InsertOwnerTest {

    @Test
    @DisplayName("Insert owner with an authenticated user")
    void insertOwner_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.insertOwner(roomId, user2Id, securityContext);

      verify(membersService, times(1)).setOwner(roomId, user2Id, true, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert owner with a guest user")
    void insertOwner_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.insertOwner(roomId, user2Id, securityContext));
    }

    @Test
    @DisplayName("Insert owner with a non-valid user")
    void insertOwner_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.insertOwner(roomId, user2Id, securityContext));
    }
  }

  @Nested
  @DisplayName("Delete owner tests")
  class DeleteOwnerTest {

    @Test
    @DisplayName("Delete owner with an authenticated user")
    void deleteOwner_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.deleteOwner(roomId, user2Id, securityContext);

      verify(membersService, times(1)).setOwner(roomId, user2Id, false, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Delete owner with a guest user")
    void deleteOwner_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.deleteOwner(roomId, user2Id, securityContext));
    }

    @Test
    @DisplayName("Delete owner with a non-valid user")
    void deleteOwner_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.deleteOwner(roomId, user2Id, securityContext));
    }
  }

  @Nested
  @DisplayName("List room attachments info tests")
  class ListRoomAttachmentsInfoTest {

    @Test
    @DisplayName("List room attachments info with an authenticated user")
    void listRoomAttachmentsInfo_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response =
          roomsApiService.listRoomAttachmentsInfo(roomId, 10, "test", securityContext);

      verify(attachmentService, times(1)).getAttachmentInfoByRoomId(roomId, 10, "test", user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("List room attachments info with a guest user")
    void listRoomAttachmentsInfo_testGuestUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      Response response =
          roomsApiService.listRoomAttachmentsInfo(roomId, 10, "test", securityContext);

      verify(attachmentService, times(1)).getAttachmentInfoByRoomId(roomId, 10, "test", guest);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("List room attachments info with a non-valid user")
    void listRoomAttachmentsInfo_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.listRoomAttachmentsInfo(roomId, 10, "test", securityContext));
    }
  }

  @Nested
  @DisplayName("Insert attachment tests")
  class InsertAttachmentTest {

    InputStream attachment = mock(InputStream.class);

    @Test
    @DisplayName("Insert attachment with an authenticated user with area, correct format")
    void insertAttachment_testAuthenticatedUser_areaCorrectFormat() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response =
          roomsApiService.insertAttachment(
              roomId,
              genericFileName,
              "image/jpeg",
              1024L,
              attachment,
              null,
              "message-id",
              "reply-id",
              "10x5",
              securityContext);

      verify(attachmentService, times(1))
          .addAttachment(
              roomId,
              attachment,
              "image/jpeg",
              1024L,
              "fileName",
              "",
              "message-id",
              "reply-id",
              "10x5",
              user1);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert attachment with an authenticated user without area")
    void insertAttachment_testAuthenticatedUser_withoutArea() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response =
          roomsApiService.insertAttachment(
              roomId,
              genericFileName,
              "image/jpeg",
              1024L,
              attachment,
              null,
              "message-id",
              "reply-id",
              null,
              securityContext);

      verify(attachmentService, times(1))
          .addAttachment(
              roomId,
              attachment,
              "image/jpeg",
              1024L,
              "fileName",
              "",
              "message-id",
              "reply-id",
              null,
              user1);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert attachment with an authenticated user with area in wrong format")
    void insertAttachment_testAuthenticatedUser_areaWrongFormat() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response =
          roomsApiService.insertAttachment(
              roomId,
              genericFileName,
              "image/jpeg",
              1024L,
              attachment,
              null,
              "message-id",
              "reply-id",
              "wrong_format",
              securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert attachment with an authenticated user without file name")
    void insertAttachment_testAuthenticatedUser_fileNameNotFound() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.insertAttachment(
                  roomId,
                  null,
                  "image/jpeg",
                  1024L,
                  attachment,
                  null,
                  "message-id",
                  "reply-id",
                  "10x5",
                  securityContext));
    }

    @Test
    @DisplayName("Insert attachment with an authenticated user with file name not encoded")
    void insertAttachment_testAuthenticatedUser_fileNameNotEncoded() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.insertAttachment(
                  roomId,
                  "fileName",
                  "image/jpeg",
                  1024L,
                  attachment,
                  null,
                  "message-id",
                  "reply-id",
                  "10x5",
                  securityContext));
    }

    @Test
    @DisplayName("Insert attachment with an authenticated user with description not encoded")
    void insertAttachment_testAuthenticatedUser_descriptionNotEncoded() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.insertAttachment(
                  roomId,
                  genericFileName,
                  "image/jpeg",
                  1024L,
                  attachment,
                  "description",
                  "message-id",
                  "reply-id",
                  "10x5",
                  securityContext));
    }

    @Test
    @DisplayName("Insert attachment with an authenticated user without mime type")
    void insertAttachment_testAuthenticatedUser_mimeTypeNotFound() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.insertAttachment(
                  roomId,
                  genericFileName,
                  null,
                  1024L,
                  attachment,
                  null,
                  "message-id",
                  "reply-id",
                  "10x5",
                  securityContext));
    }

    @Test
    @DisplayName("Insert attachment with a guest user")
    void insertAttachment_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      assertThrows(
          ForbiddenException.class,
          () ->
              roomsApiService.insertAttachment(
                  roomId,
                  genericFileName,
                  "image/jpeg",
                  1024L,
                  attachment,
                  null,
                  "message-id",
                  "reply-id",
                  "10x5",
                  securityContext));
    }

    @Test
    @DisplayName("Insert attachment with a non-valid user")
    void insertAttachment_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () ->
              roomsApiService.insertAttachment(
                  roomId,
                  genericFileName,
                  "image/jpeg",
                  1024L,
                  attachment,
                  null,
                  "message-id",
                  "reply-id",
                  "10x5",
                  securityContext));
    }
  }

  @Nested
  @DisplayName("Get meeting room by id tests")
  class GetMeetingRoomByIdTest {

    @Test
    @DisplayName("Get meeting room by id with an authenticated user")
    void getMeetingRoomById_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.getMeetingByRoomId(roomId, securityContext);

      verify(meetingService, times(1)).getMeetingByRoomId(roomId, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Get meeting room by id with a guest user")
    void getMeetingRoomById_testGuestUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      Response response = roomsApiService.getMeetingByRoomId(roomId, securityContext);

      verify(meetingService, times(1)).getMeetingByRoomId(roomId, guest);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Get meeting room by id with a non-valid user")
    void getMeetingRoomById_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.getMeetingByRoomId(roomId, securityContext));
    }
  }
}
