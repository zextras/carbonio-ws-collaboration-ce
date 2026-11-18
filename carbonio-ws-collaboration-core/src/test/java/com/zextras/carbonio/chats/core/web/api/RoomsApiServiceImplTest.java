// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zextras.carbonio.chats.api.RFC3339DateFormat;
import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.FileMetadataBuilder;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.ReadMarker;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.AttachmentFilter;
import com.zextras.carbonio.chats.core.data.model.AttachmentsPage;
import com.zextras.carbonio.chats.core.data.model.BulkDeleteResult;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.model.InsertedMember;
import com.zextras.carbonio.chats.core.data.model.MessageContext;
import com.zextras.carbonio.chats.core.data.model.MessageSearchPage;
import com.zextras.carbonio.chats.core.data.model.PinnedMessagesView;
import com.zextras.carbonio.chats.core.data.model.ReadMarkersView;
import com.zextras.carbonio.chats.core.data.model.TimelineItem;
import com.zextras.carbonio.chats.core.data.model.TimelinePage;
import com.zextras.carbonio.chats.core.data.model.TimelineView;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.data.type.MimeTypeCategory;
import com.zextras.carbonio.chats.core.data.type.RoomType;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.event.mapper.TimelineItemMapper;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.ChatExportService;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomAccess;
import com.zextras.carbonio.chats.core.service.RoomLifecycleService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.core.web.api.mapper.impl.AttachmentMapperImpl;
import com.zextras.carbonio.chats.core.web.api.mapper.impl.MeetingMapperImpl;
import com.zextras.carbonio.chats.core.web.api.mapper.impl.ParticipantMapperImpl;
import com.zextras.carbonio.chats.core.web.api.mapper.impl.RoomMapperImpl;
import com.zextras.carbonio.chats.core.web.api.mapper.impl.SubscriptionMapperImpl;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentOrderDto;
import com.zextras.carbonio.chats.model.AttachmentSortByDto;
import com.zextras.carbonio.chats.model.AttachmentsPaginationDto;
import com.zextras.carbonio.chats.model.BulkDeleteAttachmentsRequestDto;
import com.zextras.carbonio.chats.model.BulkDeleteAttachmentsResponseDto;
import com.zextras.carbonio.chats.model.ChatMessageTimelineItemDto;
import com.zextras.carbonio.chats.model.DeletedInfoDto;
import com.zextras.carbonio.chats.model.EditedInfoDto;
import com.zextras.carbonio.chats.model.ForwardMessagesRequestDto;
import com.zextras.carbonio.chats.model.ForwardMessagesRequestMessagesInnerDto;
import com.zextras.carbonio.chats.model.ForwardedInfoDto;
import com.zextras.carbonio.chats.model.MeetingDto;
import com.zextras.carbonio.chats.model.MeetingTypeDto;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberInsertedDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.MessageDto;
import com.zextras.carbonio.chats.model.MessageSearchResultDto;
import com.zextras.carbonio.chats.model.MimeTypeCategoryDto;
import com.zextras.carbonio.chats.model.PinnedMessageDto;
import com.zextras.carbonio.chats.model.ReadMarkerDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.chats.model.SendMessageRequestDto;
import com.zextras.carbonio.chats.model.TimelineDto;
import com.zextras.carbonio.chats.model.TimelineItemTypeDto;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.time.OffsetDateTime;
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
import org.mockito.InOrder;

@UnitTest
class RoomsApiServiceImplTest {

  private final RoomsApiService roomsApiService;
  private final RoomService roomService;
  private final RoomAccess roomAccess;
  private final RoomLifecycleService roomLifecycleService;
  private final MembersService membersService;
  private final AttachmentService attachmentService;
  private final MeetingService meetingService;
  private final SecurityContext securityContext;
  private final ChatMessageService chatMessageService;
  private final ObjectMapper objectMapper;

  public RoomsApiServiceImplTest() {
    this.securityContext = mock(SecurityContext.class);
    this.roomService = mock(RoomService.class);
    this.roomAccess = mock(RoomAccess.class);
    this.roomLifecycleService = mock(RoomLifecycleService.class);
    this.membersService = mock(MembersService.class);
    this.attachmentService = mock(AttachmentService.class);
    this.meetingService = mock(MeetingService.class);
    this.chatMessageService = mock(ChatMessageService.class);
    this.objectMapper =
        JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build()
            .setDateFormat(new RFC3339DateFormat());
    ChatExportService chatExportService = mock(ChatExportService.class);
    this.roomsApiService =
        new RoomsApiServiceImpl(
            roomService,
            roomAccess,
            roomLifecycleService,
            membersService,
            attachmentService,
            meetingService,
            chatMessageService,
            chatExportService,
            new TimelineItemMapper(objectMapper),
            new RoomMapperImpl(new SubscriptionMapperImpl()),
            new SubscriptionMapperImpl(),
            new AttachmentMapperImpl(),
            new MeetingMapperImpl(new ParticipantMapperImpl()));
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
    verifyNoMoreInteractions(
        roomService,
        roomAccess,
        roomLifecycleService,
        membersService,
        attachmentService,
        meetingService,
        chatMessageService);
    reset(
        roomService,
        roomAccess,
        roomLifecycleService,
        membersService,
        attachmentService,
        meetingService,
        securityContext,
        chatMessageService);
  }

  @Nested
  @DisplayName("Response mapping tests")
  class ResponseMappingTest {

    private Room groupRoom(UUID ownerId, UUID memberId) {
      Room room =
          Room.create()
              .id(roomId.toString())
              .type(RoomType.GROUP)
              .name("room")
              .description("Room")
              .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z"));
      return room.subscriptions(
          List.of(
              Subscription.create(room, ownerId.toString()).owner(true),
              Subscription.create(room, memberId.toString()).owner(false)));
    }

    @Test
    @DisplayName("Get room answers the room with its members and the muted flag of the user")
    void getRoom_mapsMembersAndSettings() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1)).thenReturn(groupRoom(user1Id, user2Id));
      OffsetDateTime clearedAt = OffsetDateTime.parse("2022-01-02T00:00:00Z");
      when(roomService.getRoomUserSettings(roomId, user1))
          .thenReturn(
              Optional.of(
                  RoomUserSettings.create(Room.create().id(roomId.toString()), user1Id.toString())
                      .mutedUntil(OffsetDateTime.now())
                      .clearedAt(clearedAt)));

      Response response = roomsApiService.getRoom(roomId, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(roomService, times(1)).getRoomUserSettings(roomId, user1);
      RoomDto room = (RoomDto) response.getEntity();
      assertEquals(roomId, room.getId());
      assertEquals(RoomTypeDto.GROUP, room.getType());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room.getPictureUpdatedAt());
      assertEquals(
          List.of(
              MemberDto.create().userId(user1Id).owner(true),
              MemberDto.create().userId(user2Id).owner(false)),
          room.getMembers());
      assertTrue(room.getUserSettings().isMuted());
      assertEquals(clearedAt, room.getUserSettings().getClearedAt());
    }

    @Test
    @DisplayName("Get room answers default user settings when the user never changed them")
    void getRoom_mapsDefaultSettings() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1)).thenReturn(groupRoom(user1Id, user2Id));
      when(roomService.getRoomUserSettings(roomId, user1)).thenReturn(Optional.empty());

      Response response = roomsApiService.getRoom(roomId, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(roomService, times(1)).getRoomUserSettings(roomId, user1);
      RoomDto room = (RoomDto) response.getEntity();
      assertEquals(2, room.getMembers().size());
      assertFalse(room.getUserSettings().isMuted());
      assertNull(room.getUserSettings().getClearedAt());
    }

    @Test
    @DisplayName("List rooms with the extra fields answers members and user settings")
    void listRooms_mapsExtraFields() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      Room group = groupRoom(user1Id, user2Id);
      Room other = Room.create().id(UUID.randomUUID().toString()).type(RoomType.ONE_TO_ONE);
      other.subscriptions(List.of());
      List<RoomExtraFieldDto> extraFields =
          List.of(RoomExtraFieldDto.MEMBERS, RoomExtraFieldDto.SETTINGS);
      when(roomService.getRooms(extraFields, user1)).thenReturn(List.of(group, other));
      when(roomService.getRoomsUserSettings(user1))
          .thenReturn(
              Map.of(
                  group.getId(),
                  RoomUserSettings.create(group, user1Id.toString())
                      .mutedUntil(OffsetDateTime.now())));

      Response response = roomsApiService.listRooms(extraFields, securityContext);

      verify(roomService, times(1)).getRooms(extraFields, user1);
      verify(roomService, times(1)).getRoomsUserSettings(user1);
      @SuppressWarnings("unchecked")
      List<RoomDto> rooms = (List<RoomDto>) response.getEntity();
      assertEquals(2, rooms.size());
      assertEquals(RoomTypeDto.GROUP, rooms.get(0).getType());
      assertEquals(RoomTypeDto.ONE_TO_ONE, rooms.get(1).getType());
      assertEquals(2, rooms.get(0).getMembers().size());
      assertTrue(rooms.get(0).getUserSettings().isMuted());
      assertFalse(rooms.get(1).getUserSettings().isMuted());
    }

    @Test
    @DisplayName("List rooms without extra fields answers neither members nor user settings")
    void listRooms_mapsWithoutExtraFields() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomService.getRooms(null, user1)).thenReturn(List.of(groupRoom(user1Id, user2Id)));

      Response response = roomsApiService.listRooms(null, securityContext);

      verify(roomService, times(1)).getRooms(null, user1);
      @SuppressWarnings("unchecked")
      List<RoomDto> rooms = (List<RoomDto>) response.getEntity();
      assertEquals(1, rooms.size());
      assertNull(rooms.get(0).getMembers());
      assertNull(rooms.get(0).getUserSettings());
    }

    @Test
    @DisplayName("Insert room answers the created room with the settings of the creator")
    void insertRoom_mapsCreatedRoom() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      Room created = groupRoom(user1Id, user2Id);
      created.userSettings(
          List.of(
              RoomUserSettings.create(created, user2Id.toString()).mutedUntil(OffsetDateTime.now()),
              RoomUserSettings.create(created, user1Id.toString())));
      RoomCreationFieldsDto fields =
          RoomCreationFieldsDto.create()
              .type(RoomTypeDto.GROUP)
              .name("room")
              .members(
                  List.of(
                      MemberDto.create().userId(user2Id),
                      MemberDto.create().userId(UUID.randomUUID())));
      when(roomService.createRoom(fields, user1)).thenReturn(created);

      Response response = roomsApiService.insertRoom(fields, securityContext);

      verify(roomService, times(1)).createRoom(fields, user1);
      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
      RoomDto room = (RoomDto) response.getEntity();
      assertEquals(roomId, room.getId());
      assertEquals(RoomTypeDto.GROUP, room.getType());
      assertEquals(2, room.getMembers().size());
      assertFalse(room.getUserSettings().isMuted());
    }

    @Test
    @DisplayName("Update room answers the updated room without members nor user settings")
    void updateRoom_mapsUpdatedRoom() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1)).thenReturn(Room.create().type(RoomType.GROUP));
      RoomEditableFieldsDto fields = RoomEditableFieldsDto.create().name("changed");
      Room updated = groupRoom(user1Id, user2Id).name("changed");
      when(roomService.updateRoom(roomId, fields, user1)).thenReturn(updated);

      Response response = roomsApiService.updateRoom(roomId, fields, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(roomService, times(1)).updateRoom(roomId, fields, user1);
      RoomDto room = (RoomDto) response.getEntity();
      assertEquals(roomId, room.getId());
      assertEquals("changed", room.getName());
      assertNull(room.getMembers());
      assertNull(room.getUserSettings());
    }

    @Test
    @DisplayName("List room members answers the user and the owner flag of every subscription")
    void listRoomMembers_mapsSubscriptions() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(membersService.getRoomMembers(roomId, user1))
          .thenReturn(groupRoom(user1Id, user2Id).getSubscriptions());

      Response response = roomsApiService.listRoomMembers(roomId, securityContext);

      verify(membersService, times(1)).getRoomMembers(roomId, user1);
      assertEquals(
          List.of(
              MemberDto.create().userId(user1Id).owner(true),
              MemberDto.create().userId(user2Id).owner(false)),
          response.getEntity());
    }

    @Test
    @DisplayName("Update room owners answers the updated members")
    void updateRoomOwners_mapsSubscriptions() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1)).thenReturn(Room.create().type(RoomType.GROUP));
      List<MemberDto> requested = List.of(MemberDto.create().userId(user2Id).owner(true));
      when(membersService.updateRoomOwners(roomId, requested, user1))
          .thenReturn(List.of(Subscription.create().userId(user2Id.toString()).owner(true)));

      Response response = roomsApiService.updateRoomOwners(roomId, requested, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(membersService, times(1)).updateRoomOwners(roomId, requested, user1);
      assertEquals(requested, response.getEntity());
    }

    @Test
    @DisplayName("Insert room members answers every inserted member with its cleared history")
    void insertRoomMembers_mapsInsertedMembers() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1))
          .thenReturn(groupRoom(user1Id, UUID.randomUUID()));
      OffsetDateTime clearedAt = OffsetDateTime.parse("2022-01-02T00:00:00Z");
      UUID user3Id = UUID.randomUUID();
      List<MemberToInsertDto> toInsert =
          List.of(
              MemberToInsertDto.create().userId(user2Id).historyCleared(true),
              MemberToInsertDto.create().userId(user3Id).owner(true));
      when(membersService.insertRoomMembers(roomId, toInsert, user1))
          .thenReturn(
              List.of(
                  new InsertedMember(user2Id, false, clearedAt),
                  new InsertedMember(user3Id, true, null)));

      Response response = roomsApiService.insertRoomMembers(roomId, toInsert, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(membersService, times(1)).insertRoomMembers(roomId, toInsert, user1);
      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
      assertEquals(
          List.of(
              MemberInsertedDto.create().userId(user2Id).owner(false).clearedAt(clearedAt),
              MemberInsertedDto.create().userId(user3Id).owner(true)),
          response.getEntity());
    }

    @Test
    @DisplayName("List room attachments info answers the page with total, cursor and attachments")
    void listRoomAttachmentsInfo_mapsPage() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      UUID fileId = UUID.randomUUID();
      OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      FileMetadata metadata =
          FileMetadataBuilder.create()
              .id(fileId.toString())
              .name("test.pdf")
              .originalSize(2048L)
              .mimeType("application/pdf")
              .type(FileMetadataType.ATTACHMENT)
              .userId(user2Id.toString())
              .roomId(roomId.toString())
              .createdAt(createdAt)
              .updatedAt(createdAt)
              .build();
      when(attachmentService.getAttachmentInfoByRoomId(
              eq(roomId), eq(10), isNull(), any(AttachmentFilter.class), eq(user1)))
          .thenReturn(new AttachmentsPage(42L, List.of(metadata), "next-cursor"));

      Response response =
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
              null,
              null,
              securityContext);

      verify(attachmentService, times(1))
          .getAttachmentInfoByRoomId(
              eq(roomId), eq(10), isNull(), any(AttachmentFilter.class), eq(user1));
      AttachmentsPaginationDto page = (AttachmentsPaginationDto) response.getEntity();
      assertEquals(42L, page.getTotal());
      assertEquals("next-cursor", page.getCursor());
      assertEquals(1, page.getAttachments().size());
      assertEquals(fileId, page.getAttachments().get(0).getId());
      assertEquals(2048L, page.getAttachments().get(0).getSize());
      assertEquals(user2Id, page.getAttachments().get(0).getUserId());
      assertEquals(roomId, page.getAttachments().get(0).getRoomId());
      assertEquals(createdAt, page.getAttachments().get(0).getCreatedAt());
    }

    @Test
    @DisplayName("Get the meeting of a room answers the meeting with its participants")
    void getMeetingByRoomId_mapsMeeting() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      UUID meetingId = UUID.randomUUID();
      Meeting meeting =
          Meeting.create()
              .id(meetingId.toString())
              .roomId(roomId.toString())
              .name("meeting")
              .meetingType(MeetingType.SCHEDULED)
              .active(true)
              .participants(
                  List.of(
                      Participant.create()
                          .userId(user1Id.toString())
                          .queueId("queue")
                          .audioStreamOn(true)));
      when(meetingService.getMeetingByRoomId(roomId, user1)).thenReturn(meeting);

      Response response = roomsApiService.getMeetingByRoomId(roomId, securityContext);

      verify(meetingService, times(1)).getMeetingByRoomId(roomId, user1);
      MeetingDto dto = (MeetingDto) response.getEntity();
      assertEquals(meetingId, dto.getId());
      assertEquals(roomId, dto.getRoomId());
      assertEquals("meeting", dto.getName());
      assertEquals(MeetingTypeDto.SCHEDULED, dto.getMeetingType());
      assertTrue(dto.isActive());
      assertEquals(1, dto.getParticipants().size());
      assertEquals(user1Id, dto.getParticipants().get(0).getUserId());
      assertEquals("queue", dto.getParticipants().get(0).getQueueId());
      assertTrue(dto.getParticipants().get(0).isAudioStreamEnabled());
      assertFalse(dto.getParticipants().get(0).isVideoStreamEnabled());
    }
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

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(roomService, times(1)).getRoomUserSettings(roomId, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Get room with with a guest user")
    void getRoom_testGuestUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(guest);

      Response response = roomsApiService.getRoom(roomId, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, guest);
      verify(roomService, times(1)).getRoomUserSettings(roomId, guest);

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

    @BeforeEach
    void stubCreateRoom() {
      when(roomService.createRoom(any(RoomCreationFieldsDto.class), any(UserPrincipal.class)))
          .thenReturn(
              Room.create()
                  .id(UUID.randomUUID().toString())
                  .type(RoomType.GROUP)
                  .subscriptions(List.of()));
    }

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
      when(roomAccess.loadForMember(roomId, user1)).thenReturn(Room.create().type(RoomType.GROUP));

      Response response = roomsApiService.deleteRoom(roomId, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(roomLifecycleService, times(1)).deleteRoom(roomId, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Delete room with with an authenticated user for one to one")
    void deleteRoom_testAuthenticatedUser_oneToOne() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1))
          .thenReturn(Room.create().type(RoomType.ONE_TO_ONE));

      Response response = roomsApiService.deleteRoom(roomId, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Delete room with with an authenticated user but it's not found")
    void deleteRoom_testAuthenticatedUser_notFound() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1)).thenThrow(new NotFoundException());

      assertThrows(
          NotFoundException.class, () -> roomsApiService.deleteRoom(roomId, securityContext));

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
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
      when(roomAccess.loadForMember(roomId, user1)).thenReturn(Room.create().type(RoomType.GROUP));

      RoomEditableFieldsDto roomEditableFieldsDto = RoomEditableFieldsDto.create().name("test123");
      Response response =
          roomsApiService.updateRoom(roomId, roomEditableFieldsDto, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(roomService, times(1)).updateRoom(roomId, roomEditableFieldsDto, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Update room with with an authenticated user for one to one")
    void updateRoom_testAuthenticatedUser_oneToOne() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1))
          .thenReturn(Room.create().type(RoomType.ONE_TO_ONE));

      RoomEditableFieldsDto roomEditableFieldsDto = RoomEditableFieldsDto.create().name("test123");
      Response response =
          roomsApiService.updateRoom(roomId, roomEditableFieldsDto, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Update room with with an authenticated user without name and description")
    void updateRoom_testAuthenticatedUser_withoutNameAndDescription() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1)).thenReturn(Room.create().type(RoomType.GROUP));

      RoomEditableFieldsDto roomEditableFieldsDto = RoomEditableFieldsDto.create();
      Response response =
          roomsApiService.updateRoom(roomId, roomEditableFieldsDto, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Update room with with an authenticated user but it's not found")
    void updateRoom_testAuthenticatedUser_notFound() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1)).thenThrow(new NotFoundException());

      RoomEditableFieldsDto roomEditableFieldsDto = RoomEditableFieldsDto.create().name("test123");
      assertThrows(
          NotFoundException.class,
          () -> roomsApiService.updateRoom(roomId, roomEditableFieldsDto, securityContext));

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
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
      when(roomAccess.loadForMember(roomId, user1)).thenReturn(Room.create().type(RoomType.GROUP));

      List<MemberDto> memberDtos = List.of(MemberDto.create().userId(user2Id).owner(true));
      Response response = roomsApiService.updateRoomOwners(roomId, memberDtos, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(membersService, times(1)).updateRoomOwners(roomId, memberDtos, user1);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Update room owners on one to one room with with an authenticated user")
    void updateRoomOwnersOnOneToOneRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1))
          .thenReturn(Room.create().type(RoomType.ONE_TO_ONE));

      List<MemberDto> memberDtos = List.of(MemberDto.create().userId(user2Id).owner(true));
      Response response = roomsApiService.updateRoomOwners(roomId, memberDtos, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);

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
      when(roomAccess.loadForMember(roomId, user1)).thenReturn(Room.create().type(RoomType.GROUP));

      List<MemberToInsertDto> memberToInsertDtos =
          List.of(MemberToInsertDto.create().userId(user2Id));
      Response response =
          roomsApiService.insertRoomMembers(roomId, memberToInsertDtos, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(membersService, times(1)).insertRoomMembers(roomId, memberToInsertDtos, user1);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room members on a one to one room with an authenticated user")
    void insertRoomMembersOnOneToOneRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1))
          .thenReturn(Room.create().type(RoomType.ONE_TO_ONE));

      List<MemberToInsertDto> memberToInsertDtos =
          List.of(MemberToInsertDto.create().userId(user2Id));
      Response response =
          roomsApiService.insertRoomMembers(roomId, memberToInsertDtos, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room members on a group room with an authenticated user")
    void insertRoomMembersOnGroupRoom_testAuthenticatedUser() throws Exception {
      user1.carbonioAttributes(Map.of(CarbonioAttribute.WSC_MAX_GROUP_MEMBERS.getValue(), "3"));
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      UUID user3Id = UUID.randomUUID();
      UUID user4Id = UUID.randomUUID();
      when(roomAccess.loadForMember(roomId, user1))
          .thenReturn(
              Room.create()
                  .type(RoomType.GROUP)
                  .subscriptions(
                      List.of(
                          Subscription.create().userId(user1Id.toString()).owner(true),
                          Subscription.create().userId(user3Id.toString()),
                          Subscription.create().userId(user4Id.toString()))));

      List<MemberToInsertDto> memberToInsertDtos =
          List.of(MemberToInsertDto.create().userId(user2Id));
      Response response =
          roomsApiService.insertRoomMembers(roomId, memberToInsertDtos, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert room members with an empty list returns 400 without touching the room")
    void insertRoomMembers_emptyList() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.insertRoomMembers(roomId, List.of(), securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      verifyNoInteractions(roomService, membersService);
    }

    @Test
    @DisplayName("A user id listed twice counts once against the group member limit")
    void insertRoomMembers_duplicatesCountOnce() throws Exception {
      user1.carbonioAttributes(Map.of(CarbonioAttribute.WSC_MAX_GROUP_MEMBERS.getValue(), "3"));
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1))
          .thenReturn(
              Room.create()
                  .type(RoomType.GROUP)
                  .subscriptions(
                      List.of(
                          Subscription.create().userId(user1Id.toString()).owner(true),
                          Subscription.create().userId(UUID.randomUUID().toString()))));
      List<MemberToInsertDto> memberToInsertDtos =
          List.of(
              MemberToInsertDto.create().userId(user2Id),
              MemberToInsertDto.create().userId(user2Id));

      Response response =
          roomsApiService.insertRoomMembers(roomId, memberToInsertDtos, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
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
      when(roomAccess.loadForMember(roomId, user1)).thenReturn(Room.create().type(RoomType.GROUP));

      Response response = roomsApiService.deleteRoomMember(roomId, user2Id, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(roomLifecycleService, times(1)).removeMember(roomId, user2Id, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Delete room member on one to one room with an authenticated user")
    void deleteRoomMemberOnOneToOneRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1))
          .thenReturn(Room.create().type(RoomType.ONE_TO_ONE));

      Response response = roomsApiService.deleteRoomMember(roomId, user2Id, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);

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
      when(roomAccess.loadForMember(roomId, user1)).thenReturn(Room.create().type(RoomType.GROUP));

      Response response = roomsApiService.insertOwner(roomId, user2Id, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(membersService, times(1)).promoteMemberToOwner(roomId, user2Id, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert owner on one to one room with an authenticated user")
    void insertOwnerOnOneToOneRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1))
          .thenReturn(Room.create().type(RoomType.ONE_TO_ONE));

      Response response = roomsApiService.insertOwner(roomId, user2Id, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);

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
      when(roomAccess.loadForMember(roomId, user1)).thenReturn(Room.create().type(RoomType.GROUP));

      Response response = roomsApiService.deleteOwner(roomId, user2Id, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);
      verify(membersService, times(1)).demoteOwnerToMember(roomId, user2Id, user1);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Delete owner on one to one room with an authenticated user")
    void deleteOwnerOnOneToOneRoom_testAuthenticatedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(roomAccess.loadForMember(roomId, user1))
          .thenReturn(Room.create().type(RoomType.ONE_TO_ONE));

      Response response = roomsApiService.deleteOwner(roomId, user2Id, securityContext);

      verify(roomAccess, times(1)).loadForMember(roomId, user1);

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

    @BeforeEach
    void stubPage() {
      when(attachmentService.getAttachmentInfoByRoomId(any(), any(), any(), any(), any()))
          .thenReturn(new AttachmentsPage(0L, List.of(), null));
    }

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
      BulkDeleteResult serviceResult = new BulkDeleteResult(List.of(file1Id, file2Id), List.of());
      when(attachmentService.bulkDeleteRoomAttachments(roomId, List.of(file1Id, file2Id), user1))
          .thenReturn(serviceResult);

      Response response =
          roomsApiService.bulkDeleteRoomAttachments(roomId, request, securityContext);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertEquals(
          BulkDeleteAttachmentsResponseDto.create()
              .successIds(List.of(file1Id, file2Id))
              .failedIds(List.of()),
          response.getEntity());
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

  private final OffsetDateTime messageCreatedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");

  private Message storedMessage(UUID id, String text) {
    return Message.create()
        .id(id.toString())
        .roomId(roomId.toString())
        .senderId(user1Id.toString())
        .text(text)
        .createdAt(messageCreatedAt);
  }

  private MessageDto messageDto(UUID id, String text) {
    return MessageDto.create()
        .id(id)
        .roomId(roomId)
        .senderId(user1Id)
        .text(text)
        .createdAt(messageCreatedAt);
  }

  @Nested
  @DisplayName("Insert attachment multipart tests")
  class InsertAttachmentMultipartTest {

    InputStream attachment = mock(InputStream.class);

    private final OffsetDateTime attachmentCreatedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");

    private Message createdMessage(UUID id, String text) {
      return Message.create()
          .id(id.toString())
          .createdAt(attachmentCreatedAt)
          .roomId(roomId.toString())
          .senderId(user1Id.toString())
          .text(text)
          .attachmentId(UUID.randomUUID().toString());
    }

    @BeforeEach
    void stubUploadWithACreatedMessage() throws Exception {
      when(chatMessageService.uploadAttachment(
              any(), any(), any(), anyLong(), any(), any(), any(), any(), any(), any()))
          .thenReturn(createdMessage(UUID.randomUUID(), ""));
    }

    private void verifyUpload(
        String fileName, String description, String replyToId, String area, String tempId) {
      verify(chatMessageService, times(1))
          .uploadAttachment(
              eq(roomId.toString()),
              eq(attachment),
              eq("image/jpeg"),
              eq(1024L),
              eq(fileName),
              eq(description),
              eq(replyToId),
              eq(area),
              eq(tempId),
              eq(user1));
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

    private MultipartFormDataInput formWithFilePartOnly(InputPart filePart, String contentLength)
        throws Exception {
      MultipartFormDataInput input = mock(MultipartFormDataInput.class);
      Map<String, List<InputPart>> formDataMap = new HashMap<>();
      formDataMap.put("file", List.of(filePart));
      if (contentLength != null) {
        formDataMap.put("contentLength", List.of(textPart(contentLength, true)));
      }
      when(input.getFormDataMap()).thenReturn(formDataMap);
      return input;
    }

    private InputPart filePart(String fileName, MediaType mediaType) throws Exception {
      InputPart filePart = mock(InputPart.class);
      when(filePart.getBody(InputStream.class, null)).thenReturn(attachment);
      when(filePart.getFileName()).thenReturn(fileName);
      when(filePart.getMediaType()).thenReturn(mediaType);
      return filePart;
    }

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user with all fields")
    void insertAttachmentMultipart_testAuthenticatedUser_withAllFields() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      UUID createdId = UUID.randomUUID();
      Message created = createdMessage(createdId, "test").replyToId(UUID.randomUUID().toString());
      when(chatMessageService.uploadAttachment(
              eq(roomId.toString()),
              eq(attachment),
              eq("image/jpeg"),
              eq(1024L),
              eq("fileName"),
              eq("test"),
              eq("reply-id"),
              eq("10x5"),
              isNull(),
              eq(user1)))
          .thenReturn(created);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipart(
                  "\\u0066\\u0069\\u006c\\u0065\\u004e\\u0061\\u006d\\u0065",
                  Map.of(
                      "description", textPart("test", true),
                      "replyToId", textPart("reply-id", true),
                      "area", textPart("10x5", true))),
              roomId,
              securityContext);

      verifyUpload("fileName", "test", "reply-id", "10x5", null);
      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
      assertEquals(
          messageDto(createdId, "test")
              .replyToId(UUID.fromString(created.getReplyToId()))
              .attachmentId(UUID.fromString(created.getAttachmentId())),
          response.getEntity());
    }

    @Test
    @DisplayName("The created message is mapped with its edited, deleted and forwarded info")
    void insertAttachmentMultipart_mapsTheReturnedMessage() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      UUID createdId = UUID.randomUUID();
      OffsetDateTime updatedAt = OffsetDateTime.parse("2026-10-30T11:00:00Z");
      OffsetDateTime deletedAt = OffsetDateTime.parse("2026-10-30T12:00:00Z");
      OffsetDateTime forwardedAt = OffsetDateTime.parse("2026-10-29T12:00:00Z");
      String originalSender = UUID.randomUUID().toString();
      Message duplicate =
          createdMessage(createdId, "hello")
              .edited(true)
              .updatedAt(updatedAt)
              .deleted(true)
              .deletedAt(deletedAt)
              .forwarded(true)
              .forwardedSenderId(originalSender)
              .forwardedAt(forwardedAt);
      when(chatMessageService.uploadAttachment(
              any(), any(), any(), anyLong(), any(), any(), any(), any(), any(), any()))
          .thenReturn(duplicate);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipart(genericFileName, Map.of("tempId", textPart("t-1", false))),
              roomId,
              securityContext);

      verifyUpload("fileName", null, null, null, "t-1");
      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
      assertEquals(
          messageDto(createdId, "hello")
              .attachmentId(UUID.fromString(duplicate.getAttachmentId()))
              .editedInfo(EditedInfoDto.create().editedAt(updatedAt))
              .deletedInfo(
                  DeletedInfoDto.create().deletedBy(user1Id.toString()).deletedAt(deletedAt))
              .forwardedInfo(
                  ForwardedInfoDto.create()
                      .originalSenderId(originalSender)
                      .originalSentAt(forwardedAt)),
          response.getEntity());
    }

    @Test
    @DisplayName("Empty description, reply id and tempId are passed to the service as null")
    void insertAttachmentMultipart_emptyOptionalPartsAreNull() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipart(
                  genericFileName,
                  Map.of(
                      "description", textPart("", true),
                      "replyToId", textPart("", true),
                      "tempId", textPart("", true))),
              roomId,
              securityContext);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
      verifyUpload("fileName", null, null, null, null);
    }

    @Test
    @DisplayName("An empty area is not a valid area")
    void insertAttachmentMultipart_emptyAreaIsInvalid() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipart(genericFileName, Map.of("area", textPart("", true))),
              roomId,
              securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      assertEquals("Invalid area format", response.getEntity().toString());
    }

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user with feature disabled")
    void insertAttachmentMultipart_testAuthenticatedUser_featureDisabled() throws Exception {
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
      MultipartFormDataInput input =
          formWithFilePartOnly(
              filePart(
                  "\\u0066\\u0069\\u006c\\u0065\\u004e\\u0061\\u006d\\u0065",
                  MediaType.valueOf("image/jpeg")),
              "not-a-number");

      assertThrows(
          BadRequestException.class,
          () -> roomsApiService.insertAttachmentMultipart(input, roomId, securityContext));
    }

    @Test
    @DisplayName(
        "Insert attachment multipart with an authenticated user with negative content length")
    void insertAttachmentMultipart_testAuthenticatedUser_negativeContentLength() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput input =
          formWithFilePartOnly(
              filePart(
                  "\\u0066\\u0069\\u006c\\u0065\\u004e\\u0061\\u006d\\u0065",
                  MediaType.valueOf("image/jpeg")),
              "-1");

      assertThrows(
          BadRequestException.class,
          () -> roomsApiService.insertAttachmentMultipart(input, roomId, securityContext));
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
      MultipartFormDataInput input =
          formWithFilePartOnly(
              filePart(
                  "\\u0066\\u0069\\u006c\\u0065\\u004e\\u0061\\u006d\\u0065",
                  MediaType.valueOf("image/jpeg")),
              "268435456");

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.insertAttachmentMultipart(input, roomId, securityContext));
    }

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user without file")
    void insertAttachmentMultipart_testAuthenticatedUser_fileNotFound() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);
      when(multipartFormDataInput.getFormDataMap()).thenReturn(new HashMap<>());

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
      MultipartFormDataInput input =
          formWithFilePartOnly(filePart(null, MediaType.valueOf("image/jpeg")), "1024");

      Response response = roomsApiService.insertAttachmentMultipart(input, roomId, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      assertEquals("File name not found", response.getEntity().toString());
    }

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user without mime type")
    void insertAttachmentMultipart_testAuthenticatedUser_mimeTypeNotFound() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput input = formWithFilePartOnly(filePart("fileName", null), "1024");

      Response response = roomsApiService.insertAttachmentMultipart(input, roomId, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      assertEquals("Mime type not found", response.getEntity().toString());
    }

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user without content length")
    void insertAttachmentMultipart_testAuthenticatedUser_contentLengthNotFound() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput input =
          formWithFilePartOnly(filePart("fileName", MediaType.valueOf("image/jpeg")), null);

      Response response = roomsApiService.insertAttachmentMultipart(input, roomId, securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      assertEquals("Content length not found", response.getEntity().toString());
    }

    @Test
    @DisplayName("Insert attachment multipart with an authenticated user with invalid area format")
    void insertAttachmentMultipart_testAuthenticatedUser_invalidAreaFormat() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipart("fileName", Map.of("area", textPart("10+5", true))),
              roomId,
              securityContext);

      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      assertEquals("Invalid area format", response.getEntity().toString());
    }

    @Test
    @DisplayName(
        "Insert attachment multipart with an authenticated user but unable to get file body")
    void insertAttachmentMultipart_testAuthenticatedUser_returnsInternalServerError()
        throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      InputPart filePart = mock(InputPart.class);
      when(filePart.getBody(InputStream.class, null)).thenThrow(RuntimeException.class);
      when(filePart.getFileName()).thenReturn("fileName");
      when(filePart.getMediaType()).thenReturn(MediaType.valueOf("image/jpeg"));
      MultipartFormDataInput input = formWithFilePartOnly(filePart, "1024");

      Response response = roomsApiService.insertAttachmentMultipart(input, roomId, securityContext);

      assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
      assertEquals("Error processing file", response.getEntity());
    }

    @Test
    @DisplayName("A non-Chats failure of the service answers 500 \"Error processing file\"")
    void insertAttachmentMultipart_unexpectedFailure_returnsInternalServerError() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.uploadAttachment(
              any(), any(), any(), anyLong(), any(), any(), any(), any(), any(), any()))
          .thenThrow(new RuntimeException("boom"));

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipart(genericFileName, Map.of()), roomId, securityContext);

      assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
      assertEquals("Error processing file", response.getEntity());
      verifyUpload("fileName", null, null, null, null);
    }

    @Test
    @DisplayName("A Chats exception of the service propagates with its status")
    void insertAttachmentMultipart_chatsHttpExceptionPropagates() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.uploadAttachment(
              any(), any(), any(), anyLong(), any(), any(), any(), any(), any(), any()))
          .thenThrow(new ForbiddenException("not a member"))
          .thenThrow(new NotFoundException("Room not found"))
          .thenThrow(new ConflictException("not completed"))
          .thenThrow(new StorageException("storage down"));

      assertThrows(
          ForbiddenException.class,
          () ->
              roomsApiService.insertAttachmentMultipart(
                  multipart(genericFileName, Map.of()), roomId, securityContext));
      assertThrows(
          NotFoundException.class,
          () ->
              roomsApiService.insertAttachmentMultipart(
                  multipart(genericFileName, Map.of()), roomId, securityContext));
      assertThrows(
          ConflictException.class,
          () ->
              roomsApiService.insertAttachmentMultipart(
                  multipart(genericFileName, Map.of()), roomId, securityContext));
      assertThrows(
          StorageException.class,
          () ->
              roomsApiService.insertAttachmentMultipart(
                  multipart(genericFileName, Map.of()), roomId, securityContext));
      verify(chatMessageService, times(4))
          .uploadAttachment(
              any(), any(), any(), anyLong(), any(), any(), any(), any(), any(), any());
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
        verifyUpload(name, null, null, null, null);
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

      verifyUpload("😀.jpg", null, null, null, null);
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
    @DisplayName("Text parts without a content type are read as UTF-8, explicit ones are kept")
    void insertAttachmentMultipart_textPartsWithoutContentTypeAreUtf8() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      InputPart descriptionPart = textPart("àè", false);
      InputPart areaPart = textPart("10x5", true);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipart(genericFileName, Map.of("description", descriptionPart, "area", areaPart)),
              roomId,
              securityContext);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
      verify(descriptionPart).setMediaType(MediaType.TEXT_PLAIN_TYPE.withCharset("UTF-8"));
      verify(areaPart, never()).setMediaType(any());
      verifyUpload("fileName", "àè", null, "10x5", null);
    }

    @Test
    @DisplayName("A tempId longer than 64 characters is a bad request before the service is called")
    void insertAttachmentMultipart_tempIdTooLong() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput input =
          multipart(genericFileName, Map.of("tempId", textPart("t".repeat(65), false)));

      assertThrows(
          BadRequestException.class,
          () -> roomsApiService.insertAttachmentMultipart(input, roomId, securityContext));
    }

    @Test
    @DisplayName(
        "A description longer than 8192 characters is a bad request before the service is called")
    void insertAttachmentMultipart_descriptionTooLong() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      MultipartFormDataInput input =
          multipart(genericFileName, Map.of("description", textPart("d".repeat(8193), false)));

      assertThrows(
          BadRequestException.class,
          () -> roomsApiService.insertAttachmentMultipart(input, roomId, securityContext));
    }

    @Test
    @DisplayName("A tempId of 64 characters and a description of 8192 characters are accepted")
    void insertAttachmentMultipart_limitsAreInclusive() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      String tempId = "t".repeat(64);
      String description = "d".repeat(8192);

      Response response =
          roomsApiService.insertAttachmentMultipart(
              multipart(
                  genericFileName,
                  Map.of(
                      "tempId",
                      textPart(tempId, false),
                      "description",
                      textPart(description, false))),
              roomId,
              securityContext);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
      verifyUpload("fileName", description, null, null, tempId);
    }

    @Test
    @DisplayName("Insert attachment multipart with a guest user")
    void insertAttachmentMultipart_testGuestUser() throws Exception {
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
    void insertAttachmentMultipart_testUnauthorizedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

      assertThrows(
          UnauthorizedException.class,
          () ->
              roomsApiService.insertAttachmentMultipart(
                  multipartFormDataInput, roomId, securityContext));
    }
  }

  @Nested
  @DisplayName("Send message tests")
  class SendMessageTest {

    @Test
    @DisplayName("sendMessage answers 201 with the sent message")
    void sendMessage_created() throws Exception {
      UUID messageId = UUID.randomUUID();
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.sendMessage(roomId.toString(), user1, "hello", null, null))
          .thenReturn(storedMessage(messageId, "hello"));

      Response response =
          roomsApiService.sendMessage(
              roomId, SendMessageRequestDto.create().text("hello"), securityContext);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
      assertEquals(messageDto(messageId, "hello"), response.getEntity());
      verify(chatMessageService, times(1))
          .sendMessage(roomId.toString(), user1, "hello", null, null);
    }

    @Test
    @DisplayName("sendMessage passes the reply id and the tempId and maps the reply")
    void sendMessage_withReplyAndTempId() throws Exception {
      UUID messageId = UUID.randomUUID();
      UUID replyToId = UUID.randomUUID();
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.sendMessage(
              roomId.toString(), user1, "hello", replyToId.toString(), "t-1"))
          .thenReturn(storedMessage(messageId, "hello").replyToId(replyToId.toString()));

      Response response =
          roomsApiService.sendMessage(
              roomId,
              SendMessageRequestDto.create().text("hello").replyToId(replyToId).tempId("t-1"),
              securityContext);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
      assertEquals(messageDto(messageId, "hello").replyToId(replyToId), response.getEntity());
      verify(chatMessageService, times(1))
          .sendMessage(roomId.toString(), user1, "hello", replyToId.toString(), "t-1");
    }

    @Test
    @DisplayName("sendMessage maps the message returned for a duplicate tempId like a new one")
    void sendMessage_duplicate_mapsTheOriginal() throws Exception {
      UUID originalId = UUID.randomUUID();
      OffsetDateTime updatedAt = OffsetDateTime.parse("2026-10-30T11:00:00Z");
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.sendMessage(roomId.toString(), user1, "hello", null, "t-1"))
          .thenReturn(storedMessage(originalId, "hello").edited(true).updatedAt(updatedAt));

      Response response =
          roomsApiService.sendMessage(
              roomId, SendMessageRequestDto.create().text("hello").tempId("t-1"), securityContext);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
      assertEquals(
          messageDto(originalId, "hello").editedInfo(EditedInfoDto.create().editedAt(updatedAt)),
          response.getEntity());
      verify(chatMessageService, times(1))
          .sendMessage(roomId.toString(), user1, "hello", null, "t-1");
    }

    @Test
    @DisplayName("sendMessage answers the status and the text of a Forbidden rejection")
    void sendMessage_forbidden_rejectionBody() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.sendMessage(any(), any(), any(), any(), any()))
          .thenThrow(new ForbiddenException("Not a room member or message ID conflict"));

      Response response =
          roomsApiService.sendMessage(
              roomId, SendMessageRequestDto.create().text("hello"), securityContext);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals("Not a room member or message ID conflict", response.getEntity());
      verify(chatMessageService, times(1))
          .sendMessage(roomId.toString(), user1, "hello", null, null);
    }

    @Test
    @DisplayName("sendMessage answers the status and the text of a NotFound rejection")
    void sendMessage_notFound_rejectionBody() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.sendMessage(any(), any(), any(), any(), any()))
          .thenThrow(new NotFoundException("Reply target not found"));

      Response response =
          roomsApiService.sendMessage(
              roomId, SendMessageRequestDto.create().text("hello"), securityContext);

      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
      assertEquals("Reply target not found", response.getEntity());
      verify(chatMessageService, times(1))
          .sendMessage(roomId.toString(), user1, "hello", null, null);
    }

    @Test
    @DisplayName("sendMessage lets a blank text (400) and a Conflict (409) propagate")
    void sendMessage_otherExceptionsPropagate() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.sendMessage(any(), any(), any(), any(), any()))
          .thenThrow(new BadRequestException("Message text must not be blank"))
          .thenThrow(
              new ConflictException("The first request with this tempId has not completed yet"));
      SendMessageRequestDto dto = SendMessageRequestDto.create().text(" \n\t ");

      assertThrows(
          BadRequestException.class,
          () -> roomsApiService.sendMessage(roomId, dto, securityContext));
      assertThrows(
          ConflictException.class, () -> roomsApiService.sendMessage(roomId, dto, securityContext));
      verify(chatMessageService, times(2))
          .sendMessage(roomId.toString(), user1, " \n\t ", null, null);
    }

    @Test
    @DisplayName("sendMessage with a non-valid user")
    void sendMessage_testUnauthorizedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () ->
              roomsApiService.sendMessage(
                  roomId, SendMessageRequestDto.create().text("hello"), securityContext));
    }
  }

  @Nested
  @DisplayName("Edit message tests")
  class EditMessageTest {

    private final com.zextras.carbonio.chats.model.EditMessageRequestDto request =
        com.zextras.carbonio.chats.model.EditMessageRequestDto.create().text("new text");

    @Test
    @DisplayName("editMessage answers 200 with the edited message")
    void editMessage_ok() throws Exception {
      UUID messageId = UUID.randomUUID();
      OffsetDateTime updatedAt = OffsetDateTime.parse("2026-10-30T11:00:00Z");
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.editMessage(
              roomId.toString(), messageId.toString(), user1, "new text"))
          .thenReturn(storedMessage(messageId, "new text").edited(true).updatedAt(updatedAt));

      Response response = roomsApiService.editMessage(roomId, messageId, request, securityContext);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertEquals(
          messageDto(messageId, "new text").editedInfo(EditedInfoDto.create().editedAt(updatedAt)),
          response.getEntity());
      verify(chatMessageService, times(1))
          .editMessage(roomId.toString(), messageId.toString(), user1, "new text");
    }

    @Test
    @DisplayName("editMessage answers the status and the text of a Forbidden rejection")
    void editMessage_forbidden_rejectionBody() throws Exception {
      UUID messageId = UUID.randomUUID();
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.editMessage(any(), any(), any(), any()))
          .thenThrow(
              new ForbiddenException("Not a room member, not the sender, or message is deleted"));

      Response response = roomsApiService.editMessage(roomId, messageId, request, securityContext);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals(
          "Not a room member, not the sender, or message is deleted", response.getEntity());
      verify(chatMessageService, times(1))
          .editMessage(roomId.toString(), messageId.toString(), user1, "new text");
    }

    @Test
    @DisplayName("editMessage lets a NotFound propagate without a body")
    void editMessage_notFound_propagates() throws Exception {
      UUID messageId = UUID.randomUUID();
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.editMessage(any(), any(), any(), any()))
          .thenThrow(new NotFoundException(String.format("Message '%s' not found", messageId)));

      assertThrows(
          NotFoundException.class,
          () -> roomsApiService.editMessage(roomId, messageId, request, securityContext));
      verify(chatMessageService, times(1))
          .editMessage(roomId.toString(), messageId.toString(), user1, "new text");
    }

    @Test
    @DisplayName("editMessage with a non-valid user")
    void editMessage_testUnauthorizedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.editMessage(roomId, UUID.randomUUID(), request, securityContext));
    }
  }

  @Nested
  @DisplayName("Delete message tests")
  class DeleteMessageTest {

    @Test
    @DisplayName("deleteMessage answers 204")
    void deleteMessage_noContent() throws Exception {
      UUID messageId = UUID.randomUUID();
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.deleteMessage(roomId, messageId, securityContext);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      assertNull(response.getEntity());
      verify(chatMessageService, times(1))
          .deleteMessage(roomId.toString(), messageId.toString(), user1);
    }

    @Test
    @DisplayName("deleteMessage answers the status and the text of a Forbidden rejection")
    void deleteMessage_forbidden_rejectionBody() throws Exception {
      UUID messageId = UUID.randomUUID();
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      doThrow(
              new ForbiddenException(
                  "Not a room member, not the sender, or message already deleted"))
          .when(chatMessageService)
          .deleteMessage(roomId.toString(), messageId.toString(), user1);

      Response response = roomsApiService.deleteMessage(roomId, messageId, securityContext);

      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals(
          "Not a room member, not the sender, or message already deleted", response.getEntity());
      verify(chatMessageService, times(1))
          .deleteMessage(roomId.toString(), messageId.toString(), user1);
    }

    @Test
    @DisplayName("deleteMessage lets a NotFound propagate without a body")
    void deleteMessage_notFound_propagates() throws Exception {
      UUID messageId = UUID.randomUUID();
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      doThrow(new NotFoundException(String.format("Message '%s' not found", messageId)))
          .when(chatMessageService)
          .deleteMessage(roomId.toString(), messageId.toString(), user1);

      assertThrows(
          NotFoundException.class,
          () -> roomsApiService.deleteMessage(roomId, messageId, securityContext));
      verify(chatMessageService, times(1))
          .deleteMessage(roomId.toString(), messageId.toString(), user1);
    }

    @Test
    @DisplayName("deleteMessage with a non-valid user")
    void deleteMessage_testUnauthorizedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.deleteMessage(roomId, UUID.randomUUID(), securityContext));
    }
  }

  @Nested
  @DisplayName("Forward message tests")
  class ForwardMessageTest {

    private final UUID sourceRoomId = UUID.randomUUID();
    private final UUID targetRoomId = UUID.randomUUID();
    private final UUID messageId = UUID.randomUUID();

    private Message forwarded(UUID id, String originalSender, OffsetDateTime forwardedAt) {
      return Message.create()
          .id(id.toString())
          .roomId(targetRoomId.toString())
          .senderId(user1Id.toString())
          .text("")
          .createdAt(messageCreatedAt)
          .forwarded(true)
          .forwardedSenderId(originalSender)
          .forwardedAt(forwardedAt);
    }

    private MessageDto forwardedDto(UUID id, String originalSender, OffsetDateTime forwardedAt) {
      return MessageDto.create()
          .id(id)
          .roomId(targetRoomId)
          .senderId(user1Id)
          .text("")
          .createdAt(messageCreatedAt)
          .forwardedInfo(
              ForwardedInfoDto.create()
                  .originalSenderId(originalSender)
                  .originalSentAt(forwardedAt));
    }

    @Test
    @DisplayName("forwardMessage answers 201 with the forwarded message")
    void forwardMessage_created() throws Exception {
      UUID newId = UUID.randomUUID();
      String originalSender = UUID.randomUUID().toString();
      OffsetDateTime forwardedAt = OffsetDateTime.parse("2026-10-29T10:00:00Z");
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.forwardMessage(
              sourceRoomId.toString(), messageId.toString(), targetRoomId.toString(), user1))
          .thenReturn(forwarded(newId, originalSender, forwardedAt));

      Response response =
          roomsApiService.forwardMessage(
              sourceRoomId,
              messageId,
              com.zextras.carbonio.chats.model.ForwardMessageRequestDto.create()
                  .toRoomId(targetRoomId),
              securityContext);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
      assertEquals(forwardedDto(newId, originalSender, forwardedAt), response.getEntity());
      verify(chatMessageService, times(1))
          .forwardMessage(
              sourceRoomId.toString(), messageId.toString(), targetRoomId.toString(), user1);
    }

    @Test
    @DisplayName("forwardMessage lets the rejections of the service propagate")
    void forwardMessage_rejectionsPropagate() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.forwardMessage(any(), any(), any(), any()))
          .thenThrow(
              new ForbiddenException("Not a member of source/target room or message not found"))
          .thenThrow(new NotFoundException("Message not found"));
      com.zextras.carbonio.chats.model.ForwardMessageRequestDto request =
          com.zextras.carbonio.chats.model.ForwardMessageRequestDto.create().toRoomId(targetRoomId);

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.forwardMessage(sourceRoomId, messageId, request, securityContext));
      assertThrows(
          NotFoundException.class,
          () -> roomsApiService.forwardMessage(sourceRoomId, messageId, request, securityContext));
      verify(chatMessageService, times(2))
          .forwardMessage(
              sourceRoomId.toString(), messageId.toString(), targetRoomId.toString(), user1);
    }

    @Test
    @DisplayName("forwardMessages forwards every message in order and answers 201 with the copies")
    void forwardMessages_loopInOrder() throws Exception {
      UUID otherSourceRoomId = UUID.randomUUID();
      UUID secondMessageId = UUID.randomUUID();
      UUID firstCopy = UUID.randomUUID();
      UUID secondCopy = UUID.randomUUID();
      OffsetDateTime forwardedAt = OffsetDateTime.parse("2026-10-29T10:00:00Z");
      String originalSender = UUID.randomUUID().toString();
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.forwardMessage(
              sourceRoomId.toString(), messageId.toString(), targetRoomId.toString(), user1))
          .thenReturn(forwarded(firstCopy, originalSender, forwardedAt));
      when(chatMessageService.forwardMessage(
              otherSourceRoomId.toString(),
              secondMessageId.toString(),
              targetRoomId.toString(),
              user1))
          .thenReturn(forwarded(secondCopy, originalSender, forwardedAt));

      Response response =
          roomsApiService.forwardMessages(
              targetRoomId,
              ForwardMessagesRequestDto.create()
                  .messages(
                      List.of(
                          ForwardMessagesRequestMessagesInnerDto.create()
                              .sourceRoomId(sourceRoomId)
                              .messageId(messageId),
                          ForwardMessagesRequestMessagesInnerDto.create()
                              .sourceRoomId(otherSourceRoomId)
                              .messageId(secondMessageId))),
              securityContext);

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
      assertEquals(
          List.of(
              forwardedDto(firstCopy, originalSender, forwardedAt),
              forwardedDto(secondCopy, originalSender, forwardedAt)),
          response.getEntity());
      InOrder order = inOrder(chatMessageService);
      order
          .verify(chatMessageService)
          .forwardMessage(
              sourceRoomId.toString(), messageId.toString(), targetRoomId.toString(), user1);
      order
          .verify(chatMessageService)
          .forwardMessage(
              otherSourceRoomId.toString(),
              secondMessageId.toString(),
              targetRoomId.toString(),
              user1);
    }

    @Test
    @DisplayName("forwardMessages stops at the first failure and forwards nothing else")
    void forwardMessages_stopsAtFirstFailure() throws Exception {
      UUID secondMessageId = UUID.randomUUID();
      UUID thirdMessageId = UUID.randomUUID();
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.forwardMessage(
              sourceRoomId.toString(), messageId.toString(), targetRoomId.toString(), user1))
          .thenReturn(forwarded(UUID.randomUUID(), user2Id.toString(), messageCreatedAt));
      when(chatMessageService.forwardMessage(
              sourceRoomId.toString(), secondMessageId.toString(), targetRoomId.toString(), user1))
          .thenThrow(
              new ForbiddenException("Not a member of source/target room or message not found"));
      ForwardMessagesRequestDto request =
          ForwardMessagesRequestDto.create()
              .messages(
                  List.of(
                      ForwardMessagesRequestMessagesInnerDto.create()
                          .sourceRoomId(sourceRoomId)
                          .messageId(messageId),
                      ForwardMessagesRequestMessagesInnerDto.create()
                          .sourceRoomId(sourceRoomId)
                          .messageId(secondMessageId),
                      ForwardMessagesRequestMessagesInnerDto.create()
                          .sourceRoomId(sourceRoomId)
                          .messageId(thirdMessageId)));

      assertThrows(
          ForbiddenException.class,
          () -> roomsApiService.forwardMessages(targetRoomId, request, securityContext));

      verify(chatMessageService, times(1))
          .forwardMessage(
              sourceRoomId.toString(), messageId.toString(), targetRoomId.toString(), user1);
      verify(chatMessageService, times(1))
          .forwardMessage(
              sourceRoomId.toString(), secondMessageId.toString(), targetRoomId.toString(), user1);
      verify(chatMessageService, never())
          .forwardMessage(
              sourceRoomId.toString(), thirdMessageId.toString(), targetRoomId.toString(), user1);
    }

    @Test
    @DisplayName("forwardMessages with a non-valid user")
    void forwardMessages_testUnauthorizedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () ->
              roomsApiService.forwardMessages(
                  targetRoomId,
                  ForwardMessagesRequestDto.create().messages(List.of()),
                  securityContext));
    }
  }

  @Nested
  @DisplayName("Search messages tests")
  class SearchMessagesTest {

    @Test
    @DisplayName("searchMessages with a malformed before date is a bad request")
    void searchMessages_malformedBefore_isBadRequest() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.searchMessages(
                  roomId, "hello", null, "not-a-date", null, securityContext));

      verifyNoInteractions(chatMessageService);
    }

    @Test
    @DisplayName("searchMessages maps hasMore and keeps replyToId only for the visible replies")
    void searchMessages_mapsHasMoreAndVisibleReplies() throws Exception {
      UUID firstId = UUID.randomUUID();
      UUID secondId = UUID.randomUUID();
      UUID visibleReply = UUID.randomUUID();
      UUID hiddenReply = UUID.randomUUID();
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.searchVisibleMessages(
              roomId.toString(), user1Id.toString(), "hello", null, null, 50))
          .thenReturn(
              new MessageSearchPage(
                  List.of(
                      storedMessage(firstId, "hello 1").replyToId(visibleReply.toString()),
                      storedMessage(secondId, "hello 2").replyToId(hiddenReply.toString())),
                  true,
                  Set.of(visibleReply.toString())));

      Response response =
          roomsApiService.searchMessages(roomId, "hello", null, null, null, securityContext);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertEquals(
          MessageSearchResultDto.create()
              .messages(
                  List.of(
                      messageDto(firstId, "hello 1").replyToId(visibleReply),
                      messageDto(secondId, "hello 2")))
              .hasMore(true),
          response.getEntity());
      verify(chatMessageService, times(1))
          .searchVisibleMessages(roomId.toString(), user1Id.toString(), "hello", null, null, 50);
    }

    @Test
    @DisplayName("searchMessages parses the cursor, ignores a blank beforeId and keeps the limit")
    void searchMessages_parsesTheCursor() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.searchVisibleMessages(any(), any(), any(), any(), any(), anyInt()))
          .thenReturn(new MessageSearchPage(List.of(), false, Set.of()));

      Response first =
          roomsApiService.searchMessages(
              roomId, "hello", 20, "2026-10-30T10:00:00Z", "  ", securityContext);
      Response second =
          roomsApiService.searchMessages(
              roomId, "hello", 20, "2026-10-30T10:00:00Z", "anchor-id", securityContext);

      assertEquals(
          MessageSearchResultDto.create().messages(List.of()).hasMore(false), first.getEntity());
      assertEquals(
          MessageSearchResultDto.create().messages(List.of()).hasMore(false), second.getEntity());
      verify(chatMessageService, times(1))
          .searchVisibleMessages(
              roomId.toString(),
              user1Id.toString(),
              "hello",
              OffsetDateTime.parse("2026-10-30T10:00:00Z"),
              null,
              20);
      verify(chatMessageService, times(1))
          .searchVisibleMessages(
              roomId.toString(),
              user1Id.toString(),
              "hello",
              OffsetDateTime.parse("2026-10-30T10:00:00Z"),
              "anchor-id",
              20);
    }

    @Test
    @DisplayName("searchMessages with a non-valid user")
    void searchMessages_testUnauthorizedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.searchMessages(roomId, "hello", null, null, null, securityContext));
    }
  }

  private TimelineView emptyTimelineView() {
    return new TimelineView(
        new TimelinePage(List.of(), false, false),
        new MessageContext(Map.of(), Map.of()),
        new ReadMarkersView(List.of(), new VisibilityFloor(null)));
  }

  @Nested
  @DisplayName("Get timeline tests")
  class GetTimelineTest {

    @Test
    @DisplayName("getTimeline with a malformed before, after or around date is a bad request")
    void getTimeline_malformedDate_isBadRequest() {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.getTimeline(
                  roomId, "nope", null, null, null, null, null, 50, securityContext));
      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.getTimeline(
                  roomId, null, null, "2026-13-45", null, null, null, 50, securityContext));
      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.getTimeline(
                  roomId, null, null, null, null, "yesterday", null, 50, securityContext));

      verifyNoInteractions(chatMessageService);
    }

    @Test
    @DisplayName("getTimeline maps the items, the flags, the reply, the attachment and the markers")
    void getTimeline_mapsTheView() throws Exception {
      OffsetDateTime clearedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");
      OffsetDateTime eventAt = OffsetDateTime.parse("2026-10-30T11:00:00Z");
      Message original =
          Message.create()
              .id("orig")
              .roomId(roomId.toString())
              .senderId(user2Id.toString())
              .text("original text")
              .createdAt(clearedAt.plusMinutes(1));
      Message reply =
          storedMessage(UUID.randomUUID(), "the reply")
              .replyToId("orig")
              .attachmentId("att-1")
              .createdAt(clearedAt.plusMinutes(5));
      SystemEvent event =
          SystemEvent.create()
              .id("evt-1")
              .roomId(roomId.toString())
              .type("MESSAGE_PINNED")
              .actorId(user1Id.toString())
              .createdAt(eventAt);
      ReadMarker before =
          ReadMarker.create().userId(user1Id.toString()).itemCreatedAt(clearedAt.minusHours(1));
      ReadMarker after =
          ReadMarker.create().userId(user2Id.toString()).itemCreatedAt(clearedAt.plusHours(1));
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.getTimeline(
              any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
          .thenReturn(
              new TimelineView(
                  new TimelinePage(
                      List.of(
                          new TimelineItem.MessageItem(reply),
                          new TimelineItem.SystemEventItem(event)),
                      true,
                      false),
                  new MessageContext(
                      Map.of("orig", original),
                      Map.of(
                          "att-1",
                          FileMetadata.create()
                              .id("att-1")
                              .name("a.png")
                              .mimeType("image/png")
                              .originalSize(10L))),
                  new ReadMarkersView(List.of(before, after), new VisibilityFloor(clearedAt))));

      Response response =
          roomsApiService.getTimeline(
              roomId, null, null, null, null, null, null, 50, securityContext);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      TimelineDto timeline = (TimelineDto) response.getEntity();
      assertEquals(2, timeline.getItems().size());
      assertEquals(Boolean.TRUE, timeline.isHasMoreBefore());
      assertEquals(Boolean.FALSE, timeline.isHasMoreAfter());
      ChatMessageTimelineItemDto messageItem =
          (ChatMessageTimelineItemDto) timeline.getItems().get(0);
      assertEquals(TimelineItemTypeDto.MESSAGE, messageItem.getItemType());
      assertEquals("the reply", messageItem.getMessage().getText());
      assertEquals("orig", messageItem.getMessage().getReplyToId());
      assertEquals("original text", messageItem.getMessage().getRepliedMessage().getText());
      assertEquals("a.png", messageItem.getMessage().getAttachment().getName());
      assertEquals(TimelineItemTypeDto.SYSTEM_EVENT, timeline.getItems().get(1).getItemType());
      assertEquals(eventAt, timeline.getItems().get(1).getCreatedAt());
      assertEquals(
          List.of(
              ReadMarkerDto.create().userId(user1Id).itemCreatedAt(clearedAt),
              ReadMarkerDto.create().userId(user2Id).itemCreatedAt(clearedAt.plusHours(1))),
          timeline.getMarkers());
      verify(chatMessageService, times(1))
          .getTimeline(
              roomId.toString(), user1Id.toString(), null, null, null, null, null, null, 50);
    }

    @Test
    @DisplayName("A reply whose target is not in the context is mapped without reply info")
    void getTimeline_replyOutsideTheContext_notExploded() throws Exception {
      Message reply = storedMessage(UUID.randomUUID(), "the reply").replyToId("orig");
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.getTimeline(
              any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
          .thenReturn(
              new TimelineView(
                  new TimelinePage(List.of(new TimelineItem.MessageItem(reply)), false, false),
                  new MessageContext(Map.of(), Map.of()),
                  new ReadMarkersView(List.of(), new VisibilityFloor(null))));

      Response response =
          roomsApiService.getTimeline(
              roomId, null, null, null, null, null, null, 50, securityContext);

      ChatMessageTimelineItemDto item =
          (ChatMessageTimelineItemDto) ((TimelineDto) response.getEntity()).getItems().get(0);
      assertNull(item.getMessage().getReplyToId());
      assertNull(item.getMessage().getRepliedMessage());
      verify(chatMessageService, times(1))
          .getTimeline(
              roomId.toString(), user1Id.toString(), null, null, null, null, null, null, 50);
    }

    @Test
    @DisplayName("Blank query parameters become null and the limit defaults to 50")
    void getTimeline_blankParametersAreNull() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.getTimeline(
              any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
          .thenReturn(emptyTimelineView());

      Response response =
          roomsApiService.getTimeline(
              roomId, "", " ", "  ", "", "   ", null, null, securityContext);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      verify(chatMessageService, times(1))
          .getTimeline(
              roomId.toString(), user1Id.toString(), null, null, null, null, null, null, 50);
    }

    @Test
    @DisplayName("The timestamps, the ids and the limit are passed to the service")
    void getTimeline_passesTheParameters() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.getTimeline(
              any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
          .thenReturn(emptyTimelineView());

      roomsApiService.getTimeline(
          roomId,
          "2026-10-30T10:00:00Z",
          "before-id",
          "2026-10-30T11:00:00Z",
          "after-id",
          "2026-10-30T12:00:00Z",
          "around-id",
          20,
          securityContext);

      verify(chatMessageService, times(1))
          .getTimeline(
              roomId.toString(),
              user1Id.toString(),
              OffsetDateTime.parse("2026-10-30T10:00:00Z"),
              "before-id",
              OffsetDateTime.parse("2026-10-30T11:00:00Z"),
              "after-id",
              OffsetDateTime.parse("2026-10-30T12:00:00Z"),
              "around-id",
              20);
    }

    @Test
    @DisplayName("getTimeline with a non-valid user")
    void getTimeline_testUnauthorizedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () ->
              roomsApiService.getTimeline(
                  roomId, null, null, null, null, null, null, 50, securityContext));
    }
  }

  @Nested
  @DisplayName("Pinned messages and read markers tests")
  class PinnedMessagesAndReadMarkersTest {

    @Test
    @DisplayName("getPinnedMessages maps the pins with their rendered message")
    void getPinnedMessages_mapsThePins() throws Exception {
      OffsetDateTime pinnedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");
      UUID pinnedMessageId = UUID.randomUUID();
      UUID unpinnedAtMessageId = UUID.randomUUID();
      Message original =
          Message.create()
              .id("orig")
              .roomId(roomId.toString())
              .senderId(user2Id.toString())
              .text("original text")
              .createdAt(pinnedAt.minusDays(1));
      Message pinned =
          storedMessage(pinnedMessageId, "pinned text").replyToId("orig").attachmentId("att-1");
      Message other = storedMessage(unpinnedAtMessageId, "other text");
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.getPinnedMessagesView(roomId.toString(), user1Id.toString()))
          .thenReturn(
              new PinnedMessagesView(
                  List.of(
                      MessagePin.create(
                              roomId.toString(), pinnedMessageId.toString(), user2Id.toString())
                          .pinnedAt(pinnedAt)
                          .message(pinned),
                      MessagePin.create(
                              roomId.toString(), unpinnedAtMessageId.toString(), user1Id.toString())
                          .message(other)),
                  new MessageContext(
                      Map.of("orig", original),
                      Map.of(
                          "att-1",
                          FileMetadata.create()
                              .id("att-1")
                              .name("a.png")
                              .mimeType("image/png")
                              .originalSize(10L)))));

      Response response = roomsApiService.getPinnedMessages(roomId, securityContext);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      @SuppressWarnings("unchecked")
      List<PinnedMessageDto> pins = (List<PinnedMessageDto>) response.getEntity();
      assertEquals(2, pins.size());
      assertEquals(pinnedMessageId.toString(), pins.get(0).getMessageId());
      assertEquals(roomId.toString(), pins.get(0).getRoomId());
      assertEquals(user2Id.toString(), pins.get(0).getPinnedBy());
      assertEquals(pinnedAt, pins.get(0).getPinnedAt());
      assertEquals("pinned text", pins.get(0).getMessage().getText());
      assertEquals("original text", pins.get(0).getMessage().getRepliedMessage().getText());
      assertEquals("a.png", pins.get(0).getMessage().getAttachment().getName());
      assertEquals(unpinnedAtMessageId.toString(), pins.get(1).getMessageId());
      assertNull(pins.get(1).getPinnedAt());
      assertNull(pins.get(1).getMessage().getRepliedMessage());
      verify(chatMessageService, times(1))
          .getPinnedMessagesView(roomId.toString(), user1Id.toString());
    }

    @Test
    @DisplayName("getPinnedMessages with a non-valid user")
    void getPinnedMessages_testUnauthorizedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.getPinnedMessages(roomId, securityContext));
    }

    @Test
    @DisplayName("getRoomReadMarkers clamps the markers to the floor of the requester")
    void getRoomReadMarkers_clampsTheMarkers() throws Exception {
      OffsetDateTime clearedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.getRoomReadMarkers(roomId.toString(), user1Id.toString()))
          .thenReturn(
              new ReadMarkersView(
                  List.of(
                      ReadMarker.create()
                          .userId(user1Id.toString())
                          .itemCreatedAt(clearedAt.minusHours(1)),
                      ReadMarker.create()
                          .userId(user2Id.toString())
                          .itemCreatedAt(clearedAt.plusHours(1))),
                  new VisibilityFloor(clearedAt)));

      Response response = roomsApiService.getRoomReadMarkers(roomId, securityContext);

      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertEquals(
          List.of(
              ReadMarkerDto.create().userId(user1Id).itemCreatedAt(clearedAt),
              ReadMarkerDto.create().userId(user2Id).itemCreatedAt(clearedAt.plusHours(1))),
          response.getEntity());
      verify(chatMessageService, times(1))
          .getRoomReadMarkers(roomId.toString(), user1Id.toString());
    }

    @Test
    @DisplayName("getRoomReadMarkers without a floor keeps the markers as they are")
    void getRoomReadMarkers_withoutFloor() throws Exception {
      OffsetDateTime itemCreatedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.getRoomReadMarkers(roomId.toString(), user1Id.toString()))
          .thenReturn(
              new ReadMarkersView(
                  List.of(
                      ReadMarker.create().userId(user2Id.toString()).itemCreatedAt(itemCreatedAt)),
                  new VisibilityFloor(null)));

      Response response = roomsApiService.getRoomReadMarkers(roomId, securityContext);

      assertEquals(
          List.of(ReadMarkerDto.create().userId(user2Id).itemCreatedAt(itemCreatedAt)),
          response.getEntity());
      verify(chatMessageService, times(1))
          .getRoomReadMarkers(roomId.toString(), user1Id.toString());
    }

    @Test
    @DisplayName("getRoomReadMarkers with a non-valid user")
    void getRoomReadMarkers_testUnauthorizedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.getRoomReadMarkers(roomId, securityContext));
    }
  }

  @Nested
  @DisplayName("Pin, reaction and read marker tests")
  class PinReactionAndReadMarkerTest {

    private final UUID messageId = UUID.randomUUID();

    @Test
    @DisplayName("pinMessage answers 204")
    void pinMessage_noContent() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response = roomsApiService.pinMessage(roomId, messageId, securityContext);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      verify(chatMessageService, times(1))
          .pinMessage(roomId.toString(), messageId.toString(), user1Id.toString());
    }

    @Test
    @DisplayName("unpinMessage answers 204 whether or not the message was the room pin")
    void unpinMessage_noContent() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.unpinMessage(
              roomId.toString(), messageId.toString(), user1Id.toString()))
          .thenReturn(Optional.empty());

      Response response = roomsApiService.unpinMessage(roomId, messageId, securityContext);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      verify(chatMessageService, times(1))
          .unpinMessage(roomId.toString(), messageId.toString(), user1Id.toString());
    }

    @Test
    @DisplayName("setReaction answers 204 and sets the reaction of the user")
    void setReaction_noContent() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response =
          roomsApiService.setReaction(
              roomId,
              messageId,
              com.zextras.carbonio.chats.model.SetReactionRequestDto.create().reaction("👍"),
              securityContext);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      verify(chatMessageService, times(1))
          .setReaction(roomId.toString(), messageId.toString(), user1, "👍");
    }

    @Test
    @DisplayName("setReaction rejects a blank reaction with 400 and calls nothing")
    void setReaction_blankReaction_badRequest() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      assertThrows(
          BadRequestException.class,
          () ->
              roomsApiService.setReaction(
                  roomId,
                  messageId,
                  com.zextras.carbonio.chats.model.SetReactionRequestDto.create().reaction("  "),
                  securityContext));

      verifyNoInteractions(chatMessageService);
    }

    @Test
    @DisplayName("removeReaction answers 204 whether or not the user had a reaction")
    void removeReaction_noContent() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);
      when(chatMessageService.removeReaction(
              roomId.toString(), messageId.toString(), user1Id.toString()))
          .thenReturn(Optional.empty());

      Response response = roomsApiService.removeReaction(roomId, messageId, securityContext);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      verify(chatMessageService, times(1))
          .removeReaction(roomId.toString(), messageId.toString(), user1Id.toString());
    }

    @Test
    @DisplayName("markRead answers 204 and moves the marker of the user")
    void markRead_noContent() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(user1);

      Response response =
          roomsApiService.markRead(
              roomId,
              com.zextras.carbonio.chats.model.MarkReadRequestDto.create().itemId("item-id"),
              securityContext);

      assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
      verify(chatMessageService, times(1))
          .setReadMarker(roomId.toString(), user1Id.toString(), "item-id");
    }

    @Test
    @DisplayName("markRead without an itemId answers 400 \"itemId is required\" and calls nothing")
    void markRead_withoutItemId_badRequest() throws Exception {
      for (com.zextras.carbonio.chats.model.MarkReadRequestDto request :
          java.util.Arrays.asList(
              null,
              com.zextras.carbonio.chats.model.MarkReadRequestDto.create(),
              com.zextras.carbonio.chats.model.MarkReadRequestDto.create().itemId("  "))) {
        Response response = roomsApiService.markRead(roomId, request, securityContext);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("itemId is required", response.getEntity());
      }

      verifyNoInteractions(chatMessageService);
    }

    @Test
    @DisplayName("The message endpoints reject a non-valid user")
    void messageEndpoints_testUnauthorizedUser() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(null);

      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.pinMessage(roomId, messageId, securityContext));
      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.unpinMessage(roomId, messageId, securityContext));
      assertThrows(
          UnauthorizedException.class,
          () ->
              roomsApiService.setReaction(
                  roomId,
                  messageId,
                  com.zextras.carbonio.chats.model.SetReactionRequestDto.create().reaction("👍"),
                  securityContext));
      assertThrows(
          UnauthorizedException.class,
          () -> roomsApiService.removeReaction(roomId, messageId, securityContext));
      assertThrows(
          UnauthorizedException.class,
          () ->
              roomsApiService.markRead(
                  roomId,
                  com.zextras.carbonio.chats.model.MarkReadRequestDto.create().itemId("item-id"),
                  securityContext));
      verifyNoInteractions(chatMessageService);
    }
  }
}
