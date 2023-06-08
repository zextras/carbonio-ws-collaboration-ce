// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.ClearedDateDto;
import com.zextras.carbonio.chats.model.ForwardMessageDto;
import com.zextras.carbonio.chats.model.JoinSettingsByRoomDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.meeting.model.MeetingDto;
import java.io.File;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class RoomsApiServiceImpl implements RoomsApiService {

  private final RoomService        roomService;
  private final MembersService     membersService;
  private final AttachmentService  attachmentService;
  private final MeetingService     meetingService;
  private final ParticipantService participantService;

  @Inject
  public RoomsApiServiceImpl(
    RoomService roomService, MembersService membersService,
    AttachmentService attachmentService,
    MeetingService meetingService,
    ParticipantService participantService
  ) {
    this.roomService = roomService;
    this.membersService = membersService;
    this.attachmentService = attachmentService;
    this.meetingService = meetingService;
    this.participantService = participantService;
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response listRoom(List<RoomExtraFieldDto> extraFields, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.OK)
      .entity(roomService.getRooms(extraFields, currentUser))
      .build();
  }

  @Override
  @TimedCall
  public Response getRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(roomService.getRoomById(roomId, currentUser))
      .build();
  }

  @Override
  @TimedCall
  public Response insertRoom(RoomCreationFieldsDto insertRoomRequestDto, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    if (insertRoomRequestDto.getType().equals(RoomTypeDto.ONE_TO_ONE) &&
      (insertRoomRequestDto.getName() != null || insertRoomRequestDto.getDescription() != null)) {
      return Response.status(Status.BAD_REQUEST).build();
    } else {
      return Response
        .status(Status.CREATED)
        .entity(roomService.createRoom(insertRoomRequestDto, currentUser))
        .build();
    }
  }

  @Override
  @TimedCall
  public Response deleteRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    roomService.deleteRoom(roomId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response updateRoom(UUID roomId, RoomEditableFieldsDto updateRoomRequestDto, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    Optional<RoomDto> room = Optional.ofNullable(roomService.getRoomById(roomId, currentUser));
    return room.map(r -> {
      if (room.get().getType().equals(RoomTypeDto.ONE_TO_ONE)) {
        return Response.status(Status.BAD_REQUEST).build();
      } else if (room.get().getType().equals(RoomTypeDto.GROUP) &&
        (updateRoomRequestDto.getName() == null && updateRoomRequestDto.getDescription() == null)) {
        return Response.status(Status.BAD_REQUEST).build();
      } else {
        return Response.status(Status.OK)
          .entity(roomService.updateRoom(roomId, updateRoomRequestDto, currentUser))
          .build();
      }
    }).orElse(Response.status(Status.NOT_FOUND).build());
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response getRoomPicture(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    FileContentAndMetadata roomPicture = roomService.getRoomPicture(roomId, currentUser);
    return Response
      .status(Status.OK)
      .entity(roomPicture.getFile())
      .header("Content-Type", roomPicture.getMetadata().getMimeType())
      .header("Content-Length", roomPicture.getMetadata().getOriginalSize())
      .header("Content-Disposition", String.format("inline; filename=\"%s\"", roomPicture.getMetadata().getName()))
      .build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response updateRoomPicture(
    UUID roomId, String headerFileName, String headerMimeType, File body, SecurityContext securityContext
  ) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    roomService.setRoomPicture(
      roomId,
      body,
      Optional.of(headerMimeType).orElseThrow(() -> new BadRequestException("Mime type not found")),
      Optional.of(new String(Base64.getDecoder().decode(headerFileName)))
        .orElseThrow(() -> new BadRequestException("File name not found")),
      currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response deleteRoomPicture(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    roomService.deleteRoomPicture(roomId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response forwardMessages(
    UUID roomId, List<ForwardMessageDto> forwardMessageDto, SecurityContext securityContext
  ) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    roomService.forwardMessages(roomId, forwardMessageDto, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response muteRoom(UUID roomId, SecurityContext securityContext) {
    roomService.muteRoom(roomId, Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new));
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response unmuteRoom(UUID roomId, SecurityContext securityContext) {
    roomService.unmuteRoom(roomId, Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new));
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response clearRoomHistory(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(ClearedDateDto.create().clearedAt(roomService.clearRoomHistory(roomId, currentUser)))
      .build();
  }

  @Override
  @TimedCall
  public Response listRoomMember(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(membersService.getRoomMembers(roomId, currentUser))
      .build();
  }

  @Override
  @TimedCall
  public Response insertRoomMember(UUID roomId, MemberToInsertDto memberToInsertDto, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.CREATED)
      .entity(membersService.insertRoomMember(roomId, memberToInsertDto, currentUser))
      .build();
  }

  @Override
  @TimedCall
  public Response deleteRoomMember(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    membersService.deleteRoomMember(roomId, userId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response updateToOwner(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    modifyOwner(roomId, userId, true, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response deleteOwner(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    modifyOwner(roomId, userId, false, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  private void modifyOwner(UUID roomId, UUID userId, boolean isOwner, UserPrincipal currentUser) {
    membersService.setOwner(roomId, userId, isOwner, currentUser);
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response listRoomAttachmentInfo(
    UUID roomId, Integer itemsNumber, String filter, SecurityContext securityContext
  ) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(attachmentService.getAttachmentInfoByRoomId(roomId, itemsNumber, filter, currentUser)).build();
  }

  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response insertAttachment(
    UUID roomId, String fileName, String mimeType, File body, String description, String messageId, String replyId,
    SecurityContext securityContext
  ) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.CREATED)
      .entity(attachmentService.addAttachment(
        roomId,
        body,
        Optional.of(mimeType).orElseThrow(() -> new BadRequestException("Mime type not found")),
        Optional.of(new String(Base64.getDecoder().decode(fileName)))
          .orElseThrow(() -> new BadRequestException("File name not found")),
        Optional.ofNullable(description).map(d -> new String(Base64.getDecoder().decode(d))).orElse(""),
        "".equals(messageId) ? null : messageId,
        "".equals(replyId) ? null : replyId, currentUser))
      .build();
  }

  /**
   * Gets the meeting of requested room
   *
   * @param roomId          room identifier
   * @param securityContext security context created by the authentication filter {@link SecurityContext}
   * @return a response {@link Response) with status 200 and the requested meeting {@link MeetingDto } in the body
   */
  @Override
  public Response getMeetingByRoomId(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.ok().entity(meetingService.getMeetingByRoomId(roomId, currentUser)).build();
  }

  @Override
  public Response joinRoomMeeting(
    UUID roomId, JoinSettingsByRoomDto joinSettingsByRoomDto, SecurityContext securityContext
  ) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    Optional<MeetingDto> meeting = participantService.insertMeetingParticipantByRoomId(roomId,
      JoinSettingsDto.create()
        .videoStreamOn(joinSettingsByRoomDto.isVideoStreamOn())
        .audioStreamOn(joinSettingsByRoomDto.isAudioStreamOn()),
      currentUser);
    if (meeting.isPresent()) {
      return Response.ok().entity(meeting.get()).build();
    } else {
      return Response.status(Status.NO_CONTENT).build();
    }
  }


}
