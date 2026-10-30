// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zextras.carbonio.async.model.MessagePinned;
import com.zextras.carbonio.async.model.WsEvent;
import com.zextras.carbonio.chats.api.RFC3339DateFormat;
import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.AttachmentFilter;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.model.PinResult;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.MimeTypeCategory;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageReceivedBroadcaster;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.mapper.TimelineItemMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.repository.impl.TimelineRepositoryImpl;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.ChatExportService;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.MessageSentListener;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentOrderDto;
import com.zextras.carbonio.chats.model.AttachmentSortByDto;
import com.zextras.carbonio.chats.model.BulkDeleteAttachmentsRequestDto;
import com.zextras.carbonio.chats.model.BulkDeleteAttachmentsResponseDto;
import com.zextras.carbonio.chats.model.ChatMessageTimelineItemDto;
import com.zextras.carbonio.chats.model.ForwardMessagesRequestDto;
import com.zextras.carbonio.chats.model.ForwardMessagesRequestMessagesInnerDto;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.MimeTypeCategoryDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.chats.model.SendMessageRequestDto;
import com.zextras.carbonio.chats.model.TimelineDto;
import com.zextras.carbonio.chats.model.TimelineItemTypeDto;
import com.zextras.carbonio.chats.model.TimelineMessageDto;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RoomsApiServiceImplTest {

  private final RoomsApiService roomsApiService;
  private final RoomService roomService;
  private final MembersService membersService;
  private final AttachmentService attachmentService;
  private final MeetingService meetingService;
  private final SecurityContext securityContext;
  private final MessageWriteRepository messageWriteRepository;
  private final MessageSentListener messageSentListener;
  private final ChatMessageService chatMessageService;
  private final SystemEventRepository systemEventRepository;
  private final MessageRepository messageRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final StoragesService storagesService;
  private final EventDispatcher eventDispatcher;
  private final MessageReceivedBroadcaster messageReceivedBroadcaster;
  private final ObjectMapper objectMapper;

  public RoomsApiServiceImplTest() {
    this.securityContext = mock(SecurityContext.class);
    this.roomService = mock(RoomService.class);
    this.membersService = mock(MembersService.class);
    this.attachmentService = mock(AttachmentService.class);
    this.meetingService = mock(MeetingService.class);
    this.chatMessageService = mock(ChatMessageService.class);
    when(chatMessageService.getVisibilityFloor(anyString(), anyString()))
        .thenReturn(new VisibilityFloor(null));
    this.eventDispatcher = mock(EventDispatcher.class);
    this.messageReceivedBroadcaster = mock(MessageReceivedBroadcaster.class);
    this.objectMapper =
        JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build()
            .setDateFormat(new RFC3339DateFormat());
    this.fileMetadataRepository = mock(FileMetadataRepository.class);
    this.messageRepository = mock(MessageRepository.class);
    this.messageWriteRepository = mock(MessageWriteRepository.class);
    this.storagesService = mock(StoragesService.class);
    this.systemEventRepository = mock(SystemEventRepository.class);
    this.subscriptionRepository = mock(SubscriptionRepository.class);
    this.messageSentListener = mock(MessageSentListener.class);
    ChatExportService chatExportService = mock(ChatExportService.class);
    this.roomsApiService =
        new RoomsApiServiceImpl(
            roomService,
            membersService,
            attachmentService,
            meetingService,
            chatMessageService,
            eventDispatcher,
            objectMapper,
            fileMetadataRepository,
            messageRepository,
            messageWriteRepository,
            storagesService,
            new TimelineRepositoryImpl(messageRepository, systemEventRepository),
            subscriptionRepository,
            messageReceivedBroadcaster,
            Set.of(messageSentListener),
            chatExportService,
            new TimelineItemMapper(objectMapper));
  }

  private UUID user1Id;
  private UUID user2Id;

  private UUID roomId;

  private UserPrincipal user1;
  private UserPrincipal user2;
  private UserPrincipal guest;

  private String genericFileName;
  private String snoopyFileName;

  @BeforeEach
  void init() {
    UUID guestId = UUID.randomUUID();
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();

    user1 =
        UserPrincipal.create(user1Id)
            .userType(UserType.INTERNAL)
            .carbonioAttributes(
                Map.of(
                    CarbonioAttribute.WSC_PRIVATE_CHAT_CREATION.getValue(),
                    "TRUE",
                    CarbonioAttribute.WSC_GROUP_CHAT_CREATION.getValue(),
                    "TRUE",
                    CarbonioAttribute.WSC_MAX_GROUP_MEMBERS.getValue(),
                    "128",
                    CarbonioAttribute.WSC_ATTACHMENT_UPLOAD.getValue(),
                    "TRUE",
                    CarbonioAttribute.WSC_MAX_ATTACHMENT_SIZE.getValue(),
                    "128",
                    CarbonioAttribute.WSC_MAX_ROOM_PICTURE_SIZE.getValue(),
                    "2"));
    user2 = UserPrincipal.create(user2Id).userType(UserType.INTERNAL);
    guest = UserPrincipal.create(guestId).userType(UserType.GUEST);

    roomId = UUID.randomUUID();

    genericFileName = StringFormatUtils.encodeToUtf8("fileName");
    snoopyFileName = StringFormatUtils.encodeToUtf8("snoopy-image");
  }

  @AfterEach
  void afterEach() {
    verifyNoMoreInteractions(roomService, membersService, attachmentService, meetingService);
    reset(
        roomService,
        membersService,
        attachmentService,
        meetingService,
        securityContext,
        chatMessageService,
        systemEventRepository);
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
    @DisplayName("Insert room of one to one type with with an authenticated user")
    void insertRoom_testAuthenticatedUser_privateChatCreation() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create()
              .type(RoomTypeDto.ONE_TO_ONE)
              .members(List.of(MemberDto.create().userId(user2Id)));
      Response response = roomsApiService.insertRoom(roomCreationFieldsDto, securityContext);

      verify(roomService, times(1)).createRoom(roomCreationFieldsDto, user1);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room of group type with with an authenticated user")
    void insertRoom_testAuthenticatedUser_groupCreation() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      UUID user3Id = UUID.randomUUID();
      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create()
              .type(RoomTypeDto.GROUP)
              .name("test")
              .members(
                  List.of(MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id)));
      Response response = roomsApiService.insertRoom(roomCreationFieldsDto, securityContext);

      verify(roomService, times(1)).createRoom(roomCreationFieldsDto, user1);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room without a type answers bad request")
    void insertRoom_missingType() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      RoomCreationFieldsDto roomCreationFieldsDto = RoomCreationFieldsDto.create().name("room");

      assertThrows(
          BadRequestException.class,
          () -> roomsApiService.insertRoom(roomCreationFieldsDto, securityContext));
      verifyNoInteractions(roomService);
    }

    @Test
    @DisplayName("Insert room with an authenticated user with feature disabled for one to one")
    void insertRoom_testAuthenticatedUser_oneToOneWithFeatureDisabled() {
      when(securityContext.getUserPrincipal()).thenReturn(user2);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create()
              .type(RoomTypeDto.ONE_TO_ONE)
              .members(List.of(MemberDto.create().userId(user1Id)));

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.insertRoom(roomCreationFieldsDto, securityContext));
    }

    @Test
    @DisplayName("Insert room with an authenticated user with feature disabled for group")
    void insertRoom_testAuthenticatedUser_groupWithFeatureDisabled() {
      when(securityContext.getUserPrincipal()).thenReturn(user2);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create().type(RoomTypeDto.GROUP);

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.insertRoom(roomCreationFieldsDto, securityContext));
    }

    @Test
    @DisplayName("Insert group room with an authenticated user with null members")
    void insertGroupRoom_testAuthenticatedUser_nullMembers() throws Exception {
      user2.carbonioAttributes(
          Map.of(CarbonioAttribute.WSC_GROUP_CHAT_CREATION.getValue(), "TRUE"));
      when(securityContext.getUserPrincipal()).thenReturn(user2);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create().type(RoomTypeDto.GROUP);

      Response response = roomsApiService.insertRoom(roomCreationFieldsDto, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert group room with an authenticated user with less than 2 members")
    void insertGroupRoom_testAuthenticatedUser_lessThanTwoMembers() throws Exception {
      user2.carbonioAttributes(
          Map.of(CarbonioAttribute.WSC_GROUP_CHAT_CREATION.getValue(), "TRUE"));
      when(securityContext.getUserPrincipal()).thenReturn(user2);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create()
              .type(RoomTypeDto.GROUP)
              .members(List.of(MemberDto.create().userId(user1Id)));

      Response response = roomsApiService.insertRoom(roomCreationFieldsDto, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room exceeding the max members limit with an authenticated user")
    void insertRoom_testAuthenticatedUser_maxMembersLimitExceeded() {
      user2.carbonioAttributes(
          Map.of(
              CarbonioAttribute.WSC_GROUP_CHAT_CREATION.getValue(),
              "TRUE",
              CarbonioAttribute.WSC_MAX_GROUP_MEMBERS.getValue(),
              "2"));
      when(securityContext.getUserPrincipal()).thenReturn(user2);

      UUID user3Id = UUID.randomUUID();
      UUID user4Id = UUID.randomUUID();
      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create()
              .type(RoomTypeDto.GROUP)
              .members(
                  List.of(
                      MemberDto.create().userId(user1Id),
                      MemberDto.create().userId(user3Id),
                      MemberDto.create().userId(user4Id)));

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.insertRoom(roomCreationFieldsDto, securityContext));
    }

    @Test
    @DisplayName("Insert room with with an authenticated user without members for one to one")
    void insertRoom_testAuthenticatedUser_oneToOneWithEmptyMembers() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create().type(RoomTypeDto.ONE_TO_ONE);
      Response response = roomsApiService.insertRoom(roomCreationFieldsDto, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName(
        "Insert room with with an authenticated user with more than 1 member for one to one")
    void insertRoom_testAuthenticatedUser_oneToOneWithMoreMembers() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      RoomCreationFieldsDto roomCreationFieldsDto =
          RoomCreationFieldsDto.create()
              .type(RoomTypeDto.ONE_TO_ONE)
              .members(List.of(MemberDto.create(), MemberDto.create()));
      Response response = roomsApiService.insertRoom(roomCreationFieldsDto, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
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
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.GROUP));

      List<MemberDto> memberDtos = List.of(MemberDto.create().userId(user2Id).owner(true));
      Response response = roomsApiService.updateRoomOwners(roomId, memberDtos, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);
      verify(membersService, times(1)).updateRoomOwners(roomId, memberDtos, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Update room owners on one to one room with with an authenticated user")
    void updateRoomOwnersOnOneToOneRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.ONE_TO_ONE));

      List<MemberDto> memberDtos = List.of(MemberDto.create().userId(user2Id).owner(true));
      Response response = roomsApiService.updateRoomOwners(roomId, memberDtos, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
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
              roomId, snoopyFileName, "image/jpeg", 256L, imageInputStream, securityContext);

      verify(roomService, times(1))
          .setRoomPicture(roomId, imageInputStream, "image/jpeg", 256L, "snoopy-image", user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Update room picture with an authenticated user with null content length")
    void updateRoomPicture_testAuthenticatedUser_nullContentLength() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.updateRoomPicture(
                  roomId,
                  snoopyFileName,
                  "image/jpeg",
                  null,
                  mock(InputStream.class),
                  securityContext));
    }

    @Test
    @DisplayName("Update room picture with an authenticated user with negative content length")
    void updateRoomPicture_testAuthenticatedUser_negativeContentLength() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.updateRoomPicture(
                  roomId,
                  snoopyFileName,
                  "image/jpeg",
                  -1L,
                  mock(InputStream.class),
                  securityContext));
    }

    @Test
    @DisplayName(
        "Update room picture with image that exceeds the max size limit with an authenticated user")
    void updateRoomPicture_testAuthenticatedUser_maxSizeLimitExceeded() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          ForbiddenException.class,
          () ->
              roomsApiService.updateRoomPicture(
                  roomId,
                  null,
                  "image/jpeg",
                  (long) (50 * 1024 * 1024),
                  mock(InputStream.class),
                  securityContext));
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
                  roomId, snoopyFileName, null, 256L, mock(InputStream.class), securityContext));
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
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.GROUP));

      List<MemberToInsertDto> memberToInsertDtos =
          List.of(MemberToInsertDto.create().userId(user2Id));
      Response response =
          roomsApiService.insertRoomMembers(roomId, memberToInsertDtos, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);
      verify(membersService, times(1)).insertRoomMembers(roomId, memberToInsertDtos, user1);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room members on a one to one room with an authenticated user")
    void insertRoomMembersOnOneToOneRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.ONE_TO_ONE));

      List<MemberToInsertDto> memberToInsertDtos =
          List.of(MemberToInsertDto.create().userId(user2Id));
      Response response =
          roomsApiService.insertRoomMembers(roomId, memberToInsertDtos, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room members on a group room with an authenticated user")
    void insertRoomMembersOnGroupRoom_testAuthenticatedUser() throws Exception {
      user1.carbonioAttributes(Map.of(CarbonioAttribute.WSC_MAX_GROUP_MEMBERS.getValue(), "3"));
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      UUID user3Id = UUID.randomUUID();
      UUID user4Id = UUID.randomUUID();
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(
              RoomDto.create()
                  .type(RoomTypeDto.GROUP)
                  .members(
                      List.of(
                          MemberDto.create().userId(user1Id).owner(true),
                          MemberDto.create().userId(user3Id),
                          MemberDto.create().userId(user4Id))));

      List<MemberToInsertDto> memberToInsertDtos =
          List.of(MemberToInsertDto.create().userId(user2Id));
      Response response =
          roomsApiService.insertRoomMembers(roomId, memberToInsertDtos, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room members with an empty list returns 400 without touching the room")
    void insertRoomMembers_emptyList() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.insertRoomMembers(roomId, List.of(), securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("A user id listed twice counts once against the group member limit")
    void insertRoomMembers_duplicatesCountOnce() throws Exception {
      user1.carbonioAttributes(Map.of(CarbonioAttribute.WSC_MAX_GROUP_MEMBERS.getValue(), "3"));
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(
              RoomDto.create()
                  .type(RoomTypeDto.GROUP)
                  .members(
                      List.of(
                          MemberDto.create().userId(user1Id).owner(true),
                          MemberDto.create().userId(UUID.randomUUID()))));
      List<MemberToInsertDto> memberToInsertDtos =
          List.of(
              MemberToInsertDto.create().userId(user2Id),
              MemberToInsertDto.create().userId(user2Id));

      Response response =
          roomsApiService.insertRoomMembers(roomId, memberToInsertDtos, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);
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
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.GROUP));

      Response response = roomsApiService.deleteRoomMember(roomId, user2Id, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);
      verify(membersService, times(1)).deleteRoomMember(roomId, user2Id, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Delete room member on one to one room with an authenticated user")
    void deleteRoomMemberOnOneToOneRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.ONE_TO_ONE));

      Response response = roomsApiService.deleteRoomMember(roomId, user2Id, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
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
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.GROUP));

      Response response = roomsApiService.insertOwner(roomId, user2Id, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);
      verify(membersService, times(1)).promoteMemberToOwner(roomId, user2Id, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert owner on one to one room with an authenticated user")
    void insertOwnerOnOneToOneRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.ONE_TO_ONE));

      Response response = roomsApiService.insertOwner(roomId, user2Id, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
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
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.GROUP));

      Response response = roomsApiService.deleteOwner(roomId, user2Id, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);
      verify(membersService, times(1)).demoteOwnerToMember(roomId, user2Id, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Delete owner on one to one room with an authenticated user")
    void deleteOwnerOnOneToOneRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRoomById(roomId, user1))
          .thenReturn(RoomDto.create().type(RoomTypeDto.ONE_TO_ONE));

      Response response = roomsApiService.deleteOwner(roomId, user2Id, securityContext);

      verify(roomService, times(1)).getRoomById(roomId, user1);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
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
          roomsApiService.listRoomAttachmentsInfo(
              roomId,
              10,
              "test",
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              securityContext);

      verify(attachmentService, times(1))
          .getAttachmentInfoByRoomId(
              eq(roomId), eq(10), eq("test"), any(AttachmentFilter.class), eq(user1));

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("List room attachments info with a guest user")
    void listRoomAttachmentsInfo_testGuestUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      Response response =
          roomsApiService.listRoomAttachmentsInfo(
              roomId,
              10,
              "test",
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              securityContext);

      verify(attachmentService, times(1))
          .getAttachmentInfoByRoomId(
              eq(roomId), eq(10), eq("test"), any(AttachmentFilter.class), eq(guest));

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("List room attachments info with a non-valid user")
    void listRoomAttachmentsInfo_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () ->
              roomsApiService.listRoomAttachmentsInfo(
                  roomId,
                  10,
                  "test",
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  securityContext));
    }

    @Test
    @DisplayName("Maps all filter params correctly to AttachmentFilter")
    void listRoomAttachmentsInfo_mapsAllFilterParamsToAttachmentFilter() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      UUID filterUserId = UUID.randomUUID();
      OffsetDateTime after = OffsetDateTime.now().minusDays(1);
      OffsetDateTime before = OffsetDateTime.now();

      roomsApiService.listRoomAttachmentsInfo(
          roomId,
          20,
          "cursor123",
          filterUserId,
          "image/jpeg",
          null,
          after,
          before,
          100L,
          5000L,
          AttachmentSortByDto.SIZE,
          AttachmentOrderDto.ASC,
          securityContext);

      ArgumentCaptor<AttachmentFilter> captor = ArgumentCaptor.forClass(AttachmentFilter.class);
      verify(attachmentService, times(1))
          .getAttachmentInfoByRoomId(
              eq(roomId), eq(20), eq("cursor123"), captor.capture(), eq(user1));

      AttachmentFilter captured = captor.getValue();
      assertEquals(filterUserId.toString(), captured.getUserId());
      assertEquals("image/jpeg", captured.getMimeType());
      assertEquals(after, captured.getCreatedAfter());
      assertEquals(before, captured.getCreatedBefore());
      assertEquals(100L, captured.getMinSize());
      assertEquals(5000L, captured.getMaxSize());
      assertEquals("size", captured.getSortBy());
      assertEquals("asc", captured.getOrder());
    }

    @Test
    @DisplayName("Maps CREATED_AT sortBy enum to createdAt string")
    void listRoomAttachmentsInfo_mapsSortByCreatedAtToString() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      roomsApiService.listRoomAttachmentsInfo(
          roomId,
          10,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          AttachmentSortByDto.CREATED_AT,
          null,
          securityContext);

      ArgumentCaptor<AttachmentFilter> captor = ArgumentCaptor.forClass(AttachmentFilter.class);
      verify(attachmentService, times(1))
          .getAttachmentInfoByRoomId(eq(roomId), eq(10), isNull(), captor.capture(), eq(user1));

      assertEquals("created_at", captor.getValue().getSortBy());
    }

    @Test
    @DisplayName("Defaults sortBy to createdAt and order to desc when not provided")
    void listRoomAttachmentsInfo_defaultsSortParamsWhenNotProvided() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      roomsApiService.listRoomAttachmentsInfo(
          roomId, 10, null, null, null, null, null, null, null, null, null, null, securityContext);

      ArgumentCaptor<AttachmentFilter> captor = ArgumentCaptor.forClass(AttachmentFilter.class);
      verify(attachmentService, times(1))
          .getAttachmentInfoByRoomId(eq(roomId), eq(10), isNull(), captor.capture(), eq(user1));

      assertEquals("created_at", captor.getValue().getSortBy());
      assertEquals("desc", captor.getValue().getOrder());
    }

    @Test
    @DisplayName("Maps IMAGES mimeTypeCategory DTO to the domain enum on the filter")
    void listRoomAttachmentsInfo_mapsImagesMimeTypeCategoryToDomainEnum() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      roomsApiService.listRoomAttachmentsInfo(
          roomId,
          10,
          null,
          null,
          null,
          MimeTypeCategoryDto.IMAGES,
          null,
          null,
          null,
          null,
          null,
          null,
          securityContext);

      ArgumentCaptor<AttachmentFilter> captor = ArgumentCaptor.forClass(AttachmentFilter.class);
      verify(attachmentService, times(1))
          .getAttachmentInfoByRoomId(eq(roomId), eq(10), isNull(), captor.capture(), eq(user1));

      AttachmentFilter captured = captor.getValue();
      assertEquals(MimeTypeCategory.IMAGES, captured.getMimeTypeCategory());
      assertNull(captured.getMimeType());
    }

    @Test
    @DisplayName("Maps VIDEOS mimeTypeCategory DTO to the domain enum on the filter")
    void listRoomAttachmentsInfo_mapsVideosMimeTypeCategoryToDomainEnum() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      roomsApiService.listRoomAttachmentsInfo(
          roomId,
          10,
          null,
          null,
          null,
          MimeTypeCategoryDto.VIDEOS,
          null,
          null,
          null,
          null,
          null,
          null,
          securityContext);

      ArgumentCaptor<AttachmentFilter> captor = ArgumentCaptor.forClass(AttachmentFilter.class);
      verify(attachmentService, times(1))
          .getAttachmentInfoByRoomId(eq(roomId), eq(10), isNull(), captor.capture(), eq(user1));

      AttachmentFilter captured = captor.getValue();
      assertEquals(MimeTypeCategory.VIDEOS, captured.getMimeTypeCategory());
      assertNull(captured.getMimeType());
    }

    @Test
    @DisplayName("Maps DOCUMENTS mimeTypeCategory DTO to the domain enum on the filter")
    void listRoomAttachmentsInfo_mapsDocumentsMimeTypeCategoryToDomainEnum() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      roomsApiService.listRoomAttachmentsInfo(
          roomId,
          10,
          null,
          null,
          null,
          MimeTypeCategoryDto.DOCUMENTS,
          null,
          null,
          null,
          null,
          null,
          null,
          securityContext);

      ArgumentCaptor<AttachmentFilter> captor = ArgumentCaptor.forClass(AttachmentFilter.class);
      verify(attachmentService, times(1))
          .getAttachmentInfoByRoomId(eq(roomId), eq(10), isNull(), captor.capture(), eq(user1));

      assertEquals(MimeTypeCategory.DOCUMENTS, captor.getValue().getMimeTypeCategory());
    }

    @Test
    @DisplayName("Rejects with 400 when both mimeType and mimeTypeCategory are provided")
    void listRoomAttachmentsInfo_rejectsWhenBothMimeTypeAndCategoryProvided() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.listRoomAttachmentsInfo(
                  roomId,
                  10,
                  null,
                  null,
                  "image/png",
                  MimeTypeCategoryDto.IMAGES,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  securityContext));

      verifyNoMoreInteractions(attachmentService);
    }
  }

  @Nested
  @DisplayName("Insert attachment multipart tests")
  class InsertAttachmentMultipartTest {

    InputStream attachment = mock(InputStream.class);

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user with all fields")
    void insertAttachmentMultipart_testAuthenticatedUser_withAllFields() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      Map<String, List<InputPart>> formDataMap = new HashMap<>();
      InputPart filePart = mock(InputPart.class);
      when(filePart.getBody(InputStream.class, null)).thenReturn(attachment);
      when(filePart.getFileName())
          .thenReturn("\\u0066\\u0069\\u006c\\u0065\\u004e\\u0061\\u006d\\u0065");
      when(filePart.getMediaType()).thenReturn(MediaType.valueOf("image/jpeg"));
      formDataMap.put("file", List.of(filePart));
      InputPart descriptionPart = mock(InputPart.class);
      when(descriptionPart.getBody(String.class, null)).thenReturn("test");
      formDataMap.put("description", List.of(descriptionPart));
      InputPart contentLengthPart = mock(InputPart.class);
      when(contentLengthPart.getBody(String.class, null)).thenReturn("1024");
      formDataMap.put("contentLength", List.of(contentLengthPart));
      InputPart replyToIdPart = mock(InputPart.class);
      when(replyToIdPart.getBody(String.class, null)).thenReturn("reply-id");
      formDataMap.put("replyToId", List.of(replyToIdPart));
      InputPart areaPart = mock(InputPart.class);
      when(areaPart.getBody(String.class, null)).thenReturn("10x5");
      formDataMap.put("area", List.of(areaPart));
      when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipartFormDataInput, roomId, securityContext);

      verify(attachmentService, times(1))
          .addAttachment(
              roomId,
              attachment,
              "image/jpeg",
              1024L,
              "fileName",
              "test",
              "reply-id",
              "10x5",
              null,
              user1);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user with feature disabled")
    void insertAttachmentMultipart_testAuthenticatedUser_featureDisabled() {
      when(securityContext.getUserPrincipal()).thenReturn(user2);

      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      assertThrows(
          ForbiddenException.class,
          () ->
              roomsApiService.insertAttachmentMultipart(
                  multipartFormDataInput, roomId, securityContext));
    }

    @Test
    @DisplayName(
        "Insert attachment multipart with an authenticated user with invalid content length format")
    void insertAttachmentMultipart_testAuthenticatedUser_invalidContentLengthFormat()
        throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      Map<String, List<InputPart>> formDataMap = new HashMap<>();
      InputPart filePart = mock(InputPart.class);
      when(filePart.getBody(InputStream.class, null)).thenReturn(attachment);
      when(filePart.getFileName())
          .thenReturn("\\u0066\\u0069\\u006c\\u0065\\u004e\\u0061\\u006d\\u0065");
      when(filePart.getMediaType()).thenReturn(MediaType.valueOf("image/jpeg"));
      formDataMap.put("file", List.of(filePart));
      InputPart contentLengthPart = mock(InputPart.class);
      when(contentLengthPart.getBody(String.class, null)).thenReturn("not-a-number");
      formDataMap.put("contentLength", List.of(contentLengthPart));
      when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.insertAttachmentMultipart(
                  multipartFormDataInput, roomId, securityContext));
    }

    @Test
    @DisplayName(
        "Insert attachment multipart with an authenticated user with negative content length")
    void insertAttachmentMultipart_testAuthenticatedUser_negativeContentLength() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      Map<String, List<InputPart>> formDataMap = new HashMap<>();
      InputPart filePart = mock(InputPart.class);
      when(filePart.getBody(InputStream.class, null)).thenReturn(attachment);
      when(filePart.getFileName())
          .thenReturn("\\u0066\\u0069\\u006c\\u0065\\u004e\\u0061\\u006d\\u0065");
      when(filePart.getMediaType()).thenReturn(MediaType.valueOf("image/jpeg"));
      formDataMap.put("file", List.of(filePart));
      InputPart contentLengthPart = mock(InputPart.class);
      when(contentLengthPart.getBody(String.class, null)).thenReturn("-1");
      formDataMap.put("contentLength", List.of(contentLengthPart));
      when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.insertAttachmentMultipart(
                  multipartFormDataInput, roomId, securityContext));
    }

    @Test
    @DisplayName(
        "Insert attachment multipart that exceeds the max size limit with an authenticated user")
    void insertAttachmentMultipart_testAuthenticatedUser_maxSizeLimitExceeded() throws Exception {
      user2.carbonioAttributes(
          Map.of(
              CarbonioAttribute.WSC_ATTACHMENT_UPLOAD.getValue(),
              "TRUE",
              CarbonioAttribute.WSC_MAX_ATTACHMENT_SIZE.getValue(),
              "1"));
      when(securityContext.getUserPrincipal()).thenReturn(user2);

      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);
      InputPart filePart = mock(InputPart.class);
      when(filePart.getBody(InputStream.class, null)).thenReturn(attachment);
      when(filePart.getFileName())
          .thenReturn("\\u0066\\u0069\\u006c\\u0065\\u004e\\u0061\\u006d\\u0065");
      when(filePart.getMediaType()).thenReturn(MediaType.valueOf("image/jpeg"));
      Map<String, List<InputPart>> formDataMap = new HashMap<>();
      formDataMap.put("file", List.of(filePart));
      InputPart contentLengthPart = mock(InputPart.class);
      when(contentLengthPart.getBody(String.class, null)).thenReturn("268435456");
      formDataMap.put("contentLength", List.of(contentLengthPart));
      when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);

      assertThrows(
          ForbiddenException.class,
          () ->
              roomsApiService.insertAttachmentMultipart(
                  multipartFormDataInput, roomId, securityContext));
    }

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user without file")
    void insertAttachmentMultipart_testAuthenticatedUser_fileNotFound() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      Map<String, List<InputPart>> formDataMap = new HashMap<>();
      when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipartFormDataInput, roomId, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      assertEquals("File not found", response.getEntity().toString());
    }

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user without file name")
    void insertAttachmentMultipart_testAuthenticatedUser_fileNameNotFound() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      Map<String, List<InputPart>> formDataMap = new HashMap<>();
      InputPart filePart = mock(InputPart.class);
      when(filePart.getBody(InputStream.class, null)).thenReturn(attachment);
      when(filePart.getMediaType()).thenReturn(MediaType.valueOf("image/jpeg"));
      formDataMap.put("file", List.of(filePart));
      InputPart contentLengthPart = mock(InputPart.class);
      when(contentLengthPart.getBody(String.class, null)).thenReturn("1024");
      formDataMap.put("contentLength", List.of(contentLengthPart));
      when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipartFormDataInput, roomId, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      assertEquals("File name not found", response.getEntity().toString());
    }

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user without mime type")
    void insertAttachmentMultipart_testAuthenticatedUser_mimeTypeNotFound() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      Map<String, List<InputPart>> formDataMap = new HashMap<>();
      InputPart filePart = mock(InputPart.class);
      when(filePart.getBody(InputStream.class, null)).thenReturn(attachment);
      when(filePart.getFileName()).thenReturn("fileName");
      formDataMap.put("file", List.of(filePart));
      InputPart contentLengthPart = mock(InputPart.class);
      when(contentLengthPart.getBody(String.class, null)).thenReturn("1024");
      formDataMap.put("contentLength", List.of(contentLengthPart));
      when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipartFormDataInput, roomId, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      assertEquals("Mime type not found", response.getEntity().toString());
    }

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user without content length")
    void insertAttachmentMultipart_testAuthenticatedUser_contentLengthNotFound() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      Map<String, List<InputPart>> formDataMap = new HashMap<>();
      InputPart filePart = mock(InputPart.class);
      when(filePart.getBody(InputStream.class, null)).thenReturn(attachment);
      when(filePart.getFileName()).thenReturn("fileName");
      when(filePart.getMediaType()).thenReturn(MediaType.valueOf("image/jpeg"));
      formDataMap.put("file", List.of(filePart));
      when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipartFormDataInput, roomId, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      assertEquals("Content length not found", response.getEntity().toString());
    }

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user with invalid area format")
    void insertAttachmentMultipart_testAuthenticatedUser_invalidAreaFormat() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      Map<String, List<InputPart>> formDataMap = new HashMap<>();
      InputPart filePart = mock(InputPart.class);
      when(filePart.getBody(InputStream.class, null)).thenReturn(attachment);
      when(filePart.getFileName()).thenReturn("fileName");
      when(filePart.getMediaType()).thenReturn(MediaType.valueOf("image/jpeg"));
      formDataMap.put("file", List.of(filePart));
      InputPart contentLengthPart = mock(InputPart.class);
      when(contentLengthPart.getBody(String.class, null)).thenReturn("1024");
      formDataMap.put("contentLength", List.of(contentLengthPart));
      InputPart areaPart = mock(InputPart.class);
      when(areaPart.getBody(String.class, null)).thenReturn("10+5");
      formDataMap.put("area", List.of(areaPart));
      when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipartFormDataInput, roomId, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      assertEquals("Invalid area format", response.getEntity().toString());
    }

    @Test
    @DisplayName(
        "Insert attachment multipart with an authenticated user but unable to get file body")
    void insertAttachmentMultipart_testAuthenticatedUser_returnsInternalServerError()
        throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      Map<String, List<InputPart>> formDataMap = new HashMap<>();
      InputPart filePart = mock(InputPart.class);
      when(filePart.getBody(InputStream.class, null)).thenThrow(RuntimeException.class);
      when(filePart.getFileName()).thenReturn("fileName");
      when(filePart.getMediaType()).thenReturn(MediaType.valueOf("image/jpeg"));
      formDataMap.put("file", List.of(filePart));
      InputPart contentLengthPart = mock(InputPart.class);
      when(contentLengthPart.getBody(String.class, null)).thenReturn("1024");
      formDataMap.put("contentLength", List.of(contentLengthPart));
      when(multipartFormDataInput.getFormDataMap()).thenReturn(formDataMap);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipartFormDataInput, roomId, securityContext);

      assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
      assertEquals("Error processing file", response.getEntity());
    }

    private InputPart textPart(String value, boolean contentTypeFromMessage) throws Exception {
      InputPart part = mock(InputPart.class);
      when(part.getBody(String.class, null)).thenReturn(value);
      when(part.isContentTypeFromMessage()).thenReturn(contentTypeFromMessage);
      return part;
    }

    private MultipartFormDataInput multipart(String fileName, Map<String, InputPart> textParts)
        throws Exception {
      MultipartFormDataInput input = mock(MultipartFormDataInput.class);
      Map<String, List<InputPart>> formDataMap = new HashMap<>();
      InputPart filePart = mock(InputPart.class);
      when(filePart.getBody(InputStream.class, null)).thenReturn(attachment);
      when(filePart.getFileName()).thenReturn(fileName);
      when(filePart.getMediaType()).thenReturn(MediaType.valueOf("image/jpeg"));
      formDataMap.put("file", List.of(filePart));
      formDataMap.put("contentLength", List.of(textPart("1024", true)));
      textParts.forEach((name, part) -> formDataMap.put(name, List.of(part)));
      when(input.getFormDataMap()).thenReturn(formDataMap);
      return input;
    }

    @Test
    @DisplayName("A raw file name is used as-is")
    void insertAttachmentMultipart_rawFileNameIsUsedAsIs() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      for (String name : List.of("foto.jpg", "foto àè.jpg", "cafe", "foto\\u00e0.jpg")) {
        Response response =
            roomsApiService.insertAttachmentMultipart(
                multipart(name, Map.of()), roomId, securityContext);

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus(), name);
        verify(attachmentService)
            .addAttachment(
                roomId, attachment, "image/jpeg", 1024L, name, null, null, null, null, user1);
      }
    }

    @Test
    @DisplayName("A fully escaped file name is decoded")
    void insertAttachmentMultipart_escapedFileNameIsDecoded() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      roomsApiService.insertAttachmentMultipart(
          multipart("\\ud83d\\ude00\\u002e\\u006a\\u0070\\u0067", Map.of()),
          roomId,
          securityContext);

      verify(attachmentService)
          .addAttachment(
              roomId, attachment, "image/jpeg", 1024L, "😀.jpg", null, null, null, null, user1);
    }

    @Test
    @DisplayName("A file name that decodes to a lone surrogate is a bad request")
    void insertAttachmentMultipart_loneSurrogateFileNameIsBadRequest() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput input = multipart("\\ud83d", Map.of());

      assertThrows(
          BadRequestException.class,
          () -> roomsApiService.insertAttachmentMultipart(input, roomId, securityContext));
    }

    @Test
    @DisplayName("Insert attachment multipart with a guest user")
    void insertAttachmentMultipart_testGuestUser() {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      assertThrows(
          ForbiddenException.class,
          () ->
              roomsApiService.insertAttachmentMultipart(
                  multipartFormDataInput, roomId, securityContext));
    }

    @Test
    @DisplayName("Insert attachment multipart with a non-valid user")
    void insertAttachmentMultipart_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      assertThrows(
          UnauthorizedException.class,
          () ->
              roomsApiService.insertAttachmentMultipart(
                  multipartFormDataInput, roomId, securityContext));
    }
  }

  @Test
  @DisplayName("sendMessage notifies the registered message-sent listeners")
  void sendMessage_notifiesMessageSentListeners() throws Exception {
    String generatedMsgId = UUID.randomUUID().toString();
    when(securityContext.getUserPrincipal()).thenReturn(user1);
    when(messageWriteRepository.sendMessage(
            eq(roomId.toString()),
            eq(user1Id.toString()),
            eq("hello"),
            anyString(),
            isNull(),
            isNull()))
        .thenReturn(
            Optional.of(
                new MessageWriteRepository.SendResult(generatedMsgId, OffsetDateTime.now())));

    SendMessageRequestDto dto = SendMessageRequestDto.create().text("hello");
    Response response = roomsApiService.sendMessage(roomId, dto, securityContext);

    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    verify(messageSentListener, times(1)).onMessageSent(user1, roomId, generatedMsgId, "hello");
  }

  @Test
  @DisplayName("sendMessage does not notify the listeners when the message is not stored")
  void sendMessage_notStored_doesNotNotifyMessageSentListeners() throws Exception {
    when(securityContext.getUserPrincipal()).thenReturn(user1);
    when(messageWriteRepository.sendMessage(
            eq(roomId.toString()),
            eq(user1Id.toString()),
            eq("hello"),
            anyString(),
            isNull(),
            isNull()))
        .thenReturn(Optional.empty());

    SendMessageRequestDto dto = SendMessageRequestDto.create().text("hello");
    Response response = roomsApiService.sendMessage(roomId, dto, securityContext);

    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    verify(messageSentListener, never()).onMessageSent(any(), any(), any(), any());
  }

  @Test
  @DisplayName("deleteMessage leaves the attachment file of the deleted message in place")
  void deleteMessage_keepsAttachmentFile() throws Exception {
    UUID messageId = UUID.randomUUID();
    when(securityContext.getUserPrincipal()).thenReturn(user1);
    when(messageWriteRepository.deleteMessage(
            messageId.toString(), roomId.toString(), user1Id.toString()))
        .thenReturn(
            Optional.of(
                new MessageWriteRepository.DeleteResult(
                    messageId.toString(), UUID.randomUUID().toString())));

    Response response = roomsApiService.deleteMessage(roomId, messageId, securityContext);

    assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    verifyNoInteractions(storagesService);
  }

  @Test
  @DisplayName("sendMessage answers 201 even when a message-sent listener fails")
  void sendMessage_listenerFails_stillCreated() throws Exception {
    String generatedMsgId = UUID.randomUUID().toString();
    when(securityContext.getUserPrincipal()).thenReturn(user1);
    when(messageWriteRepository.sendMessage(
            eq(roomId.toString()),
            eq(user1Id.toString()),
            eq("hello"),
            anyString(),
            isNull(),
            isNull()))
        .thenReturn(
            Optional.of(
                new MessageWriteRepository.SendResult(generatedMsgId, OffsetDateTime.now())));
    doThrow(new RuntimeException("listener failure"))
        .when(messageSentListener)
        .onMessageSent(user1, roomId, generatedMsgId, "hello");

    SendMessageRequestDto dto = SendMessageRequestDto.create().text("hello");
    Response response = roomsApiService.sendMessage(roomId, dto, securityContext);

    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
  }

  @Nested
  @DisplayName("Pin, unpin and delete events tests")
  class PinUnpinDeleteEventsTest {

    @Test
    @DisplayName("unpinMessage of a message that is not the room pin answers 204 and sends nothing")
    void unpinMessage_notPinned_sendsNothing() throws Exception {
      UUID messageId = UUID.randomUUID();
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.unpinMessage(
              roomId.toString(), messageId.toString(), user1Id.toString()))
          .thenReturn(Optional.empty());

      Response response = roomsApiService.unpinMessage(roomId, messageId, securityContext);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      verify(chatMessageService).validateRoomMembership(roomId.toString(), user1Id.toString());
      verifyNoInteractions(eventDispatcher);
    }

    @Test
    @DisplayName("unpinMessage of the room pin sends MessageUnpinned with the persisted row")
    void unpinMessage_pinned_sendsUnpinned() throws Exception {
      UUID messageId = UUID.randomUUID();
      List<String> members = List.of(user1Id.toString(), user2Id.toString());
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(subscriptionRepository.getUserIdsByRoomId(roomId.toString())).thenReturn(members);
      when(chatMessageService.unpinMessage(
              roomId.toString(), messageId.toString(), user1Id.toString()))
          .thenReturn(
              Optional.of(
                  SystemEvent.create()
                      .id("unpin-event")
                      .roomId(roomId.toString())
                      .type("MESSAGE_UNPINNED")
                      .actorId(user1Id.toString())
                      .createdAt(OffsetDateTime.parse("2026-10-30T10:00:00Z"))));

      Response response = roomsApiService.unpinMessage(roomId, messageId, securityContext);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
      verify(eventDispatcher).sendRawToUserExchange(eq(members), payload.capture());
      JsonNode event = objectMapper.readTree(payload.getValue());
      assertEquals("MessageUnpinned", event.get("type").asText());
      assertEquals(messageId.toString(), event.get("messageId").asText());
      assertEquals(user1Id.toString(), event.get("unpinnedBy").asText());
      assertEquals("unpin-event", event.get("systemEventId").asText());
    }

    @Test
    @DisplayName("pinMessage announces the pin it committed without re-reading the room pin")
    void pinMessage_broadcastsCommittedPin() throws Exception {
      UUID messageId = UUID.randomUUID();
      OffsetDateTime pinnedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");
      Message message =
          Message.create()
              .id(messageId.toString())
              .roomId(roomId.toString())
              .senderId(user2Id.toString())
              .text("pinned text")
              .createdAt(pinnedAt.minusMinutes(1));
      SystemEvent systemEvent =
          SystemEvent.create()
              .id("pin-event")
              .roomId(roomId.toString())
              .type("MESSAGE_PINNED")
              .actorId(user1Id.toString())
              .createdAt(pinnedAt);
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.pinMessage(
              roomId.toString(), messageId.toString(), user1Id.toString()))
          .thenReturn(new PinResult(message, systemEvent));

      Response response = roomsApiService.pinMessage(roomId, messageId, securityContext);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      verify(chatMessageService, never()).getPinnedMessages(anyString());
      ArgumentCaptor<WsEvent> event = ArgumentCaptor.forClass(WsEvent.class);
      ArgumentCaptor<TimelineMessageDto> broadcastMessage =
          ArgumentCaptor.forClass(TimelineMessageDto.class);
      verify(messageReceivedBroadcaster)
          .broadcastWithReplyVisibility(
              eq(roomId.toString()), event.capture(), broadcastMessage.capture());
      MessagePinned pinned = (MessagePinned) event.getValue();
      assertEquals(messageId.toString(), pinned.getMessageId());
      assertEquals(user1Id.toString(), pinned.getPinnedBy());
      assertEquals("pin-event", pinned.getSystemEventId());
      assertEquals(pinnedAt, pinned.getSystemEventCreatedAt());
      assertEquals("pinned text", broadcastMessage.getValue().getText());
    }

    @Test
    @DisplayName("deleteMessage of the room pin sends MessageDeleted, then MessageUnpinned")
    void deleteMessage_pinned_sendsDeletedThenUnpinned() throws Exception {
      UUID messageId = UUID.randomUUID();
      OffsetDateTime unpinnedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");
      List<String> members = List.of(user1Id.toString(), user2Id.toString());
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(subscriptionRepository.getUserIdsByRoomId(roomId.toString())).thenReturn(members);
      when(messageWriteRepository.deleteMessage(
              messageId.toString(), roomId.toString(), user1Id.toString()))
          .thenReturn(
              Optional.of(
                  new MessageWriteRepository.DeleteResult(
                      messageId.toString(),
                      null,
                      new MessageWriteRepository.Unpinned("unpin-event", unpinnedAt))));

      Response response = roomsApiService.deleteMessage(roomId, messageId, securityContext);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      ArgumentCaptor<String> payloads = ArgumentCaptor.forClass(String.class);
      verify(eventDispatcher, times(2)).sendRawToUserExchange(eq(members), payloads.capture());
      JsonNode deleted = objectMapper.readTree(payloads.getAllValues().get(0));
      JsonNode unpinned = objectMapper.readTree(payloads.getAllValues().get(1));
      assertEquals("MessageDeleted", deleted.get("type").asText());
      assertEquals("MessageUnpinned", unpinned.get("type").asText());
      assertEquals(messageId.toString(), unpinned.get("messageId").asText());
      assertEquals(user1Id.toString(), unpinned.get("unpinnedBy").asText());
      assertEquals("unpin-event", unpinned.get("systemEventId").asText());
      assertEquals(
          unpinnedAt.toInstant(),
          OffsetDateTime.parse(unpinned.get("systemEventCreatedAt").asText()).toInstant());
    }

    @Test
    @DisplayName("deleteMessage of a message that is not the room pin sends only MessageDeleted")
    void deleteMessage_notPinned_sendsOnlyDeleted() throws Exception {
      UUID messageId = UUID.randomUUID();
      List<String> members = List.of(user1Id.toString(), user2Id.toString());
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(subscriptionRepository.getUserIdsByRoomId(roomId.toString())).thenReturn(members);
      when(messageWriteRepository.deleteMessage(
              messageId.toString(), roomId.toString(), user1Id.toString()))
          .thenReturn(
              Optional.of(new MessageWriteRepository.DeleteResult(messageId.toString(), null)));

      Response response = roomsApiService.deleteMessage(roomId, messageId, securityContext);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
      verify(eventDispatcher, times(1)).sendRawToUserExchange(eq(members), payload.capture());
      assertEquals(
          "MessageDeleted", objectMapper.readTree(payload.getValue()).get("type").asText());
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

  @Nested
  @DisplayName("Bulk delete room attachments tests")
  class BulkDeleteRoomAttachmentsTests {

    @Test
    @DisplayName("Authenticated user can bulk delete room attachments, returns 200 with result")
    void bulkDeleteRoomAttachments_authenticatedUserSuccess() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      UUID file1Id = UUID.randomUUID();
      UUID file2Id = UUID.randomUUID();
      BulkDeleteAttachmentsRequestDto request =
          BulkDeleteAttachmentsRequestDto.create().attachmentIds(List.of(file1Id, file2Id));
      BulkDeleteAttachmentsResponseDto serviceResult =
          BulkDeleteAttachmentsResponseDto.create()
              .successIds(List.of(file1Id, file2Id))
              .failedIds(List.of());
      when(attachmentService.bulkDeleteRoomAttachments(roomId, List.of(file1Id, file2Id), user1))
          .thenReturn(serviceResult);

      Response response =
          roomsApiService.bulkDeleteRoomAttachments(roomId, request, securityContext);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertEquals(serviceResult, response.getEntity());
      verify(attachmentService, times(1))
          .bulkDeleteRoomAttachments(roomId, List.of(file1Id, file2Id), user1);
    }

    @Test
    @DisplayName("Unauthenticated user cannot bulk delete room attachments")
    void bulkDeleteRoomAttachments_testUnauthorizedUser() {
      when(securityContext.getUserPrincipal()).thenReturn(null);
      BulkDeleteAttachmentsRequestDto request =
          BulkDeleteAttachmentsRequestDto.create().attachmentIds(List.of(UUID.randomUUID()));

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.bulkDeleteRoomAttachments(roomId, request, securityContext));
    }
  }

  @Nested
  @DisplayName("Get timeline tests")
  class GetTimelineTest {

    private List<Message> makeMessages(int count, OffsetDateTime newestTs) {
      List<Message> list = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        list.add(
            Message.create()
                .id("msg-" + String.format("%04d", count - i))
                .roomId(roomId.toString())
                .senderId(user1Id.toString())
                .createdAt(newestTs.minusSeconds(i)));
      }
      return list;
    }

    private SystemEvent makeRoomCreatedEvent(OffsetDateTime ts) {
      return SystemEvent.create()
          .id("evt-room-created")
          .roomId(roomId.toString())
          .type("ROOM_CREATED")
          .actorId(user1.getId())
          .createdAt(ts);
    }

    @Test
    @DisplayName(
        "Default branch: 50 messages + ROOM_CREATED → 50 items total, hasMoreBefore=true, oldest"
            + " item is MESSAGE")
    void defaultBranch_50MessagesAndRoomCreated_returns50ItemsWithHasMore() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      OffsetDateTime newestMsg = OffsetDateTime.parse("2026-01-01T12:00:50Z");
      OffsetDateTime roomCreatedAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");

      List<Message> messages = makeMessages(50, newestMsg);
      when(messageRepository.getByRoomId(eq(roomId.toString()), isNull(), isNull(), eq(51)))
          .thenReturn(messages);

      SystemEvent roomCreated = makeRoomCreatedEvent(roomCreatedAt);
      when(systemEventRepository.findByRoomIdBounded(
              eq(roomId.toString()), isNull(), isNull(), anyInt()))
          .thenReturn(List.of(roomCreated));

      when(chatMessageService.getRoomReadMarkers(eq(roomId.toString()))).thenReturn(List.of());

      Response response =
          roomsApiService.getTimeline(roomId, null, null, null, null, 50, securityContext);

      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      TimelineDto timeline = (TimelineDto) response.getEntity();
      assertEquals(50, timeline.getItems().size(), "merged+trimmed list must be exactly 50");
      assertEquals(
          Boolean.TRUE,
          timeline.isHasMoreBefore(),
          "hasMoreBefore must be true when merged exceeded limit");
      assertEquals(
          TimelineItemTypeDto.MESSAGE,
          timeline.getItems().get(0).getItemType(),
          "oldest kept item must be a MESSAGE (ROOM_CREATED trimmed off)");
    }

    private SystemEvent makeSe(String id, OffsetDateTime ts) {
      return SystemEvent.create().id(id).roomId(roomId.toString()).type("PIN").createdAt(ts);
    }

    @Test
    @DisplayName(
        "After branch: forward-skip — when more events exist than fetchLimit, oldest events after"
            + " anchor are returned (not newest)")
    void afterBranch_forwardSkip_oldestEventsReturnedNotNewest() throws Exception {
      OffsetDateTime anchor = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      OffsetDateTime t1 = anchor.plusSeconds(1);
      OffsetDateTime t2 = anchor.plusSeconds(2);
      OffsetDateTime t3 = anchor.plusSeconds(3);
      OffsetDateTime t4 = anchor.plusSeconds(4);
      OffsetDateTime t5 = anchor.plusSeconds(5);

      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.getRoomReadMarkers(anyString())).thenReturn(List.of());

      when(systemEventRepository.findByRoomIdBounded(
              eq(roomId.toString()), isNull(), eq(anchor), eq(3)))
          .thenReturn(List.of(makeSe("evt-t5", t5), makeSe("evt-t4", t4), makeSe("evt-t3", t3)));

      when(systemEventRepository.findByRoomIdForward(
              eq(roomId.toString()), eq(anchor), isNull(), isNull(), eq(3)))
          .thenReturn(List.of(makeSe("evt-t1", t1), makeSe("evt-t2", t2), makeSe("evt-t3", t3)));

      Response response =
          roomsApiService.getTimeline(
              roomId, null, null, anchor.toString(), null, 2, securityContext);

      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      TimelineDto timeline = (TimelineDto) response.getEntity();
      assertEquals(2, timeline.getItems().size());
      assertEquals(
          t1,
          timeline.getItems().get(0).getCreatedAt(),
          "oldest event (T1) after anchor must be first — DESC fetch returns newest (T3+) instead");
    }

    @Test
    @DisplayName("After branch: equal-timestamp items are ordered stably by id (ASC tiebreak)")
    void afterBranch_equalTimestamp_idTiebreakDeterminesOrder() throws Exception {
      OffsetDateTime anchor = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      OffsetDateTime t1 = anchor.plusSeconds(1);

      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.getRoomReadMarkers(anyString())).thenReturn(List.of());

      Message msg =
          Message.create()
              .id("msg-zzz")
              .roomId(roomId.toString())
              .senderId(user1Id.toString())
              .createdAt(t1);
      when(messageRepository.getByRoomId(
              eq(roomId.toString()), isNull(), eq(anchor), eq(3), eq(true)))
          .thenReturn(List.of(msg));

      SystemEvent se = makeSe("evt-aaa", t1);
      when(systemEventRepository.findByRoomIdBounded(
              eq(roomId.toString()), isNull(), eq(anchor), eq(3)))
          .thenReturn(List.of(se));
      when(systemEventRepository.findByRoomIdForward(
              eq(roomId.toString()), eq(anchor), isNull(), isNull(), eq(3)))
          .thenReturn(List.of(se));

      Response response =
          roomsApiService.getTimeline(
              roomId, null, null, anchor.toString(), null, 2, securityContext);

      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      TimelineDto timeline = (TimelineDto) response.getEntity();
      assertEquals(2, timeline.getItems().size());
      assertEquals(
          TimelineItemTypeDto.SYSTEM_EVENT,
          timeline.getItems().get(0).getItemType(),
          "id 'evt-aaa' < 'msg-zzz' so SYSTEM_EVENT must sort first — fails without id tiebreak");
    }

    @Test
    @DisplayName(
        "Around branch: only events closest to aroundTs are kept on each side, not arbitrary"
            + " room-wide events")
    void aroundBranch_closestEventsKept_notArbitraryRoomWide() throws Exception {
      OffsetDateTime aroundTs = OffsetDateTime.parse("2026-01-01T12:00:05Z");
      OffsetDateTime t2 = OffsetDateTime.parse("2026-01-01T12:00:02Z");
      OffsetDateTime t3 = OffsetDateTime.parse("2026-01-01T12:00:03Z");
      OffsetDateTime t4 = OffsetDateTime.parse("2026-01-01T12:00:04Z");
      OffsetDateTime t6 = OffsetDateTime.parse("2026-01-01T12:00:06Z");
      OffsetDateTime t7 = OffsetDateTime.parse("2026-01-01T12:00:07Z");
      OffsetDateTime t8 = OffsetDateTime.parse("2026-01-01T12:00:08Z");

      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.getRoomReadMarkers(anyString())).thenReturn(List.of());

      when(systemEventRepository.findByRoomIdBounded(
              eq(roomId.toString()), isNull(), isNull(), anyInt()))
          .thenReturn(
              List.of(makeSe("e8", t8), makeSe("e7", t7), makeSe("e6", t6), makeSe("e4", t4)));

      when(systemEventRepository.findByRoomIdBounded(
              eq(roomId.toString()), any(OffsetDateTime.class), isNull(), eq(3)))
          .thenReturn(List.of(makeSe("e4", t4), makeSe("e3", t3), makeSe("e2", t2)));

      when(systemEventRepository.findByRoomIdForward(
              eq(roomId.toString()), eq(aroundTs), isNull(), isNull(), eq(3)))
          .thenReturn(List.of(makeSe("e6", t6), makeSe("e7", t7), makeSe("e8", t8)));

      Response response =
          roomsApiService.getTimeline(
              roomId, null, null, null, aroundTs.toString(), 4, securityContext);

      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      TimelineDto timeline = (TimelineDto) response.getEntity();
      assertEquals(
          4,
          timeline.getItems().size(),
          "around(limit=4,half=2): 2 closest older + 2 closest newer = 4 items");
      assertEquals(
          t3,
          timeline.getItems().get(0).getCreatedAt(),
          "oldest kept older event must be T3 (closest: T4, T3)");
      assertEquals(
          t6,
          timeline.getItems().get(2).getCreatedAt(),
          "first newer event must be T6 (closest newer to aroundTs)");
    }

    @Test
    @DisplayName(
        "Default branch: 51 messages + ROOM_CREATED → exactly 50 items total, no over-count")
    void defaultBranch_51MessagesAndRoomCreated_noOverCount() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      OffsetDateTime newestMsg = OffsetDateTime.parse("2026-01-01T12:00:51Z");
      OffsetDateTime roomCreatedAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");

      List<Message> messages = makeMessages(51, newestMsg);
      when(messageRepository.getByRoomId(eq(roomId.toString()), isNull(), isNull(), eq(51)))
          .thenReturn(messages);

      SystemEvent roomCreated = makeRoomCreatedEvent(roomCreatedAt);
      when(systemEventRepository.findByRoomIdBounded(
              eq(roomId.toString()), isNull(), isNull(), anyInt()))
          .thenReturn(List.of(roomCreated));

      when(chatMessageService.getRoomReadMarkers(eq(roomId.toString()))).thenReturn(List.of());

      Response response =
          roomsApiService.getTimeline(roomId, null, null, null, null, 50, securityContext);

      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      TimelineDto timeline = (TimelineDto) response.getEntity();
      assertEquals(50, timeline.getItems().size(), "must never return more than limit items");
      assertEquals(Boolean.TRUE, timeline.isHasMoreBefore());
    }

    @Test
    @DisplayName("Attachments are batch-fetched via getByIds once, never per-message getById")
    void attachments_batchedNotPerMessage() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.getRoomReadMarkers(anyString())).thenReturn(List.of());

      OffsetDateTime t = OffsetDateTime.parse("2026-01-01T12:00:00Z");
      Message m1 =
          Message.create()
              .id("m1")
              .roomId(roomId.toString())
              .senderId(user1Id.toString())
              .createdAt(t)
              .attachmentId("att-1");
      Message m2 =
          Message.create()
              .id("m2")
              .roomId(roomId.toString())
              .senderId(user1Id.toString())
              .createdAt(t.plusSeconds(1))
              .attachmentId("att-2");
      when(messageRepository.getByRoomId(eq(roomId.toString()), isNull(), isNull(), eq(51)))
          .thenReturn(List.of(m2, m1));
      when(systemEventRepository.findByRoomIdBounded(
              eq(roomId.toString()), isNull(), isNull(), anyInt()))
          .thenReturn(List.of());
      when(fileMetadataRepository.getByIds(any()))
          .thenReturn(
              List.of(
                  FileMetadata.create()
                      .id("att-1")
                      .name("a.png")
                      .mimeType("image/png")
                      .originalSize(10L),
                  FileMetadata.create()
                      .id("att-2")
                      .name("b.png")
                      .mimeType("image/png")
                      .originalSize(20L)));

      Response response =
          roomsApiService.getTimeline(roomId, null, null, null, null, 50, securityContext);

      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      verify(fileMetadataRepository, times(1)).getByIds(any());
      verify(fileMetadataRepository, times(0)).getById(anyString());
      assertEquals(2, ((TimelineDto) response.getEntity()).getItems().size());
    }

    @Test
    @DisplayName("Reply is exploded: original embedded depth-1, batch-fetched once")
    void reply_isExplodedDepth1() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.getRoomReadMarkers(anyString())).thenReturn(List.of());

      OffsetDateTime t = OffsetDateTime.parse("2026-01-01T12:00:00Z");
      Message original =
          Message.create()
              .id("orig")
              .roomId(roomId.toString())
              .senderId(user2Id.toString())
              .text("original text")
              .createdAt(t);
      Message reply =
          Message.create()
              .id("reply")
              .roomId(roomId.toString())
              .senderId(user1Id.toString())
              .text("the reply")
              .createdAt(t.plusSeconds(5))
              .replyToId("orig");
      when(messageRepository.getByRoomId(eq(roomId.toString()), isNull(), isNull(), eq(51)))
          .thenReturn(List.of(reply));
      when(systemEventRepository.findByRoomIdBounded(
              eq(roomId.toString()), isNull(), isNull(), anyInt()))
          .thenReturn(List.of());
      when(messageRepository.getByRoomIdAndIds(eq(roomId.toString()), any()))
          .thenReturn(List.of(original));

      Response response =
          roomsApiService.getTimeline(roomId, null, null, null, null, 50, securityContext);

      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      verify(messageRepository, times(1)).getByRoomIdAndIds(eq(roomId.toString()), any());
      ChatMessageTimelineItemDto item =
          (ChatMessageTimelineItemDto) ((TimelineDto) response.getEntity()).getItems().get(0);
      assertEquals("orig", item.getMessage().getReplyToId());
      assertEquals("original text", item.getMessage().getRepliedMessage().getText());
      assertEquals(user2Id.toString(), item.getMessage().getRepliedMessage().getSenderId());
      assertNull(
          item.getMessage().getRepliedMessage().getRepliedMessage(),
          "depth-1: the embedded original must not carry its own repliedMessage");
    }

    @Test
    @DisplayName("Reply target at or before the caller's cleared-at is NOT exploded")
    void reply_belowClearedAt_notExploded() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      OffsetDateTime t = OffsetDateTime.parse("2026-01-01T12:00:00Z");
      OffsetDateTime clearedAt = t.plusSeconds(3);
      when(chatMessageService.getVisibilityFloor(anyString(), anyString()))
          .thenReturn(new VisibilityFloor(clearedAt));
      when(chatMessageService.getRoomReadMarkers(anyString())).thenReturn(List.of());

      Message original = original(t);
      Message reply = reply(t.plusSeconds(10));
      when(messageRepository.getByRoomId(eq(roomId.toString()), isNull(), eq(clearedAt), eq(51)))
          .thenReturn(List.of(reply));
      when(systemEventRepository.findByRoomIdBounded(
              eq(roomId.toString()), isNull(), eq(clearedAt), anyInt()))
          .thenReturn(List.of());
      when(messageRepository.getByRoomIdAndIds(eq(roomId.toString()), any()))
          .thenReturn(List.of(original));

      Response response =
          roomsApiService.getTimeline(roomId, null, null, null, null, 50, securityContext);

      ChatMessageTimelineItemDto item =
          (ChatMessageTimelineItemDto) ((TimelineDto) response.getEntity()).getItems().get(0);
      assertNull(item.getMessage().getReplyToId());
      assertNull(item.getMessage().getRepliedMessage());
    }

    @Test
    @DisplayName("Reply target after the caller's cleared-at is exploded")
    void reply_afterClearedAt_exploded() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      OffsetDateTime t = OffsetDateTime.parse("2026-01-01T12:00:00Z");
      OffsetDateTime clearedAt = t.minusSeconds(3);
      when(chatMessageService.getVisibilityFloor(anyString(), anyString()))
          .thenReturn(new VisibilityFloor(clearedAt));
      when(chatMessageService.getRoomReadMarkers(anyString())).thenReturn(List.of());

      Message original = original(t);
      Message reply = reply(t.plusSeconds(10));
      when(messageRepository.getByRoomId(eq(roomId.toString()), isNull(), eq(clearedAt), eq(51)))
          .thenReturn(List.of(reply));
      when(systemEventRepository.findByRoomIdBounded(
              eq(roomId.toString()), isNull(), eq(clearedAt), anyInt()))
          .thenReturn(List.of());
      when(messageRepository.getByRoomIdAndIds(eq(roomId.toString()), any()))
          .thenReturn(List.of(original));

      Response response =
          roomsApiService.getTimeline(roomId, null, null, null, null, 50, securityContext);

      ChatMessageTimelineItemDto item =
          (ChatMessageTimelineItemDto) ((TimelineDto) response.getEntity()).getItems().get(0);
      assertEquals("original text", item.getMessage().getRepliedMessage().getText());
    }

    private Message original(OffsetDateTime createdAt) {
      return Message.create()
          .id("orig")
          .roomId(roomId.toString())
          .senderId(user2Id.toString())
          .text("original text")
          .createdAt(createdAt);
    }

    private Message reply(OffsetDateTime createdAt) {
      return Message.create()
          .id("reply")
          .roomId(roomId.toString())
          .senderId(user1Id.toString())
          .text("the reply")
          .createdAt(createdAt)
          .replyToId("orig");
    }
  }

  @Nested
  @DisplayName("Forward attachment preview tests")
  class ForwardAttachmentPreviewTests {

    private final UUID sourceRoomId = UUID.randomUUID();
    private final UUID targetRoomId = UUID.randomUUID();
    private final UUID messageId = UUID.randomUUID();
    private final String attachmentId = UUID.randomUUID().toString();
    private FileMetadata video;

    @BeforeEach
    void setUpForward() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(messageRepository.getById(messageId.toString()))
          .thenReturn(
              Optional.of(
                  Message.create()
                      .id(messageId.toString())
                      .roomId(sourceRoomId.toString())
                      .senderId(user2Id.toString())
                      .attachmentId(attachmentId)
                      .createdAt(OffsetDateTime.now())));
      video =
          FileMetadata.create()
              .id(attachmentId)
              .name("clip.mp4")
              .mimeType("video/mp4")
              .originalSize(1024L)
              .type(FileMetadataType.ATTACHMENT)
              .userId(user2Id.toString())
              .roomId(sourceRoomId.toString());
      when(fileMetadataRepository.getById(attachmentId)).thenReturn(Optional.of(video));
    }

    private ForwardMessagesRequestDto request() {
      return ForwardMessagesRequestDto.create()
          .messages(
              List.of(
                  ForwardMessagesRequestMessagesInnerDto.create()
                      .sourceRoomId(sourceRoomId)
                      .messageId(messageId)));
    }

    @Test
    @DisplayName("Forwarding a video copies its preview to the forwarded copy")
    void forwardMessages_video_copiesPreviewToTheCopy() throws Exception {
      ArgumentCaptor<String> copyId = ArgumentCaptor.forClass(String.class);
      when(messageWriteRepository.forwardMessage(
              eq(sourceRoomId.toString()),
              eq(targetRoomId.toString()),
              eq(user1Id.toString()),
              eq(messageId.toString()),
              anyString(),
              copyId.capture()))
          .thenAnswer(
              invocation ->
                  Optional.of(
                      new MessageWriteRepository.ForwardResult(
                          invocation.getArgument(4),
                          OffsetDateTime.now(),
                          "",
                          user2Id.toString(),
                          OffsetDateTime.now(),
                          invocation.getArgument(5),
                          "clip.mp4",
                          "video/mp4",
                          1024L)));

      roomsApiService.forwardMessages(targetRoomId, request(), securityContext);

      verify(attachmentService).copyVideoPreview(video, copyId.getValue(), user1Id.toString());
    }

    @Test
    @DisplayName("No preview copy when the forward fails and the copied file is removed")
    void forwardMessages_forwardFails_noPreviewCopy() {
      when(messageWriteRepository.forwardMessage(
              anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
          .thenReturn(Optional.empty());

      assertThrows(
          RuntimeException.class,
          () -> roomsApiService.forwardMessages(targetRoomId, request(), securityContext));

      verify(attachmentService, never()).copyVideoPreview(any(), anyString(), anyString());
    }
  }
}
