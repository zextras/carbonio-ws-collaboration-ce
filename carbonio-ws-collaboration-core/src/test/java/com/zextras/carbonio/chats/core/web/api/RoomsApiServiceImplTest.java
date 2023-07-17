// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class RoomsApiServiceImplTest {

  private final RoomsApiService    roomsApiService;
  private final RoomService        roomService;
  private final MembersService     membersService;
  private final AttachmentService  attachmentService;
  private final MeetingService     meetingService;
  private final ParticipantService participantService;
  private final SecurityContext    securityContext;

  public RoomsApiServiceImplTest() {
    this.securityContext = mock(SecurityContext.class);
    this.roomService = mock(RoomService.class);
    this.membersService = mock(MembersService.class);
    this.attachmentService = mock(AttachmentService.class);
    this.meetingService = mock(MeetingService.class);
    this.participantService = mock(ParticipantService.class);
    this.roomsApiService = new RoomsApiServiceImpl(roomService, membersService, attachmentService, meetingService,
      participantService);
  }

  private UUID user1Id;
  private UUID user2Id;
  private UUID roomOneToOne1Id;
  private Room roomOneToOne1;

  @BeforeEach
  void init() {
    user1Id = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a723");
    user2Id = UUID.fromString("82735f6d-4c6c-471e-99d9-4eef91b1ec45");

    roomOneToOne1Id = UUID.fromString("86327874-40f4-47cb-914d-f0ce706d1611");

    roomOneToOne1 = Room.create();
    roomOneToOne1.id(roomOneToOne1Id.toString()).type(RoomTypeDto.ONE_TO_ONE).name("").description("").subscriptions(
      List.of(Subscription.create(roomOneToOne1, user1Id.toString()).owner(true),
        Subscription.create(roomOneToOne1, user2Id.toString()).owner(false)));
  }

  @AfterEach
  public void afterEach() {
    reset(this.roomService, this.membersService, this.attachmentService, this.meetingService, this.participantService);
  }

  @Nested
  @DisplayName("Insert attachment tests")
  class InsertAttachmentTest {

    File attachment = mock(File.class);

    @Test
    @DisplayName("Insert attachment with area, correct format")
    void insertAttachment_areaCorrectFormat() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(UserPrincipal.create(user1Id));

      Response response = roomsApiService.insertAttachment(roomOneToOne1Id, StringFormatUtils.encodeToUtf8("fileName"), "image/jpeg",
        attachment, null, "message-id", "reply-id", "10x5", securityContext);

      verify(attachmentService, times(1)).addAttachment(roomOneToOne1Id, attachment, "image/jpeg",
        "fileName", "", "message-id", "reply-id", "10x5",
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal()).get());

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert attachment without area")
    void insertAttachment_areaNull() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(UserPrincipal.create(user1Id));

      Response response = roomsApiService.insertAttachment(roomOneToOne1Id, StringFormatUtils.encodeToUtf8("fileName"), "image/jpeg",
        attachment, null, "message-id", "reply-id", null, securityContext);

      verify(attachmentService, times(1)).addAttachment(roomOneToOne1Id, attachment, "image/jpeg",
        "fileName", "", "message-id", "reply-id", null,
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal()).get());

      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Insert attachment with area, wrong format")
    void insertAttachment_areaWrongFormat() throws Exception {
      when(securityContext.getUserPrincipal()).thenReturn(UserPrincipal.create(user1Id));

      Response response = roomsApiService.insertAttachment(roomOneToOne1Id, StringFormatUtils.encodeToUtf8("fileName"), "image/jpeg",
        attachment, null, "message-id", "reply-id", "wrong_format", securityContext);

      verifyNoInteractions(attachmentService);
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
  }
}