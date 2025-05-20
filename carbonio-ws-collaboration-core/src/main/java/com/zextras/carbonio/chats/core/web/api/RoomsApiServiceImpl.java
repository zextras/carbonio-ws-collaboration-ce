// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.ClearedDateDto;
import com.zextras.carbonio.chats.model.ForwardMessageDto;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Singleton
public class RoomsApiServiceImpl implements RoomsApiService {

  private final RoomService roomService;
  private final MembersService membersService;
  private final AttachmentService attachmentService;
  private final MeetingService meetingService;

  @Inject
  public RoomsApiServiceImpl(
      RoomService roomService,
      MembersService membersService,
      AttachmentService attachmentService,
      MeetingService meetingService) {
    this.roomService = roomService;
    this.membersService = membersService;
    this.attachmentService = attachmentService;
    this.meetingService = meetingService;
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response listRooms(List<RoomExtraFieldDto> extraFields, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
        .entity(roomService.getRooms(extraFields, currentUser))
        .build();
  }

  @Override
  @TimedCall
  public Response getRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK).entity(roomService.getRoomById(roomId, currentUser)).build();
  }

  @Override
  @TimedCall
  public Response insertRoom(
      RoomCreationFieldsDto insertRoomRequestDto, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    if (insertRoomRequestDto.getType().equals(RoomTypeDto.ONE_TO_ONE)
        && (insertRoomRequestDto.getName() != null
            || insertRoomRequestDto.getDescription() != null)) {
      return Response.status(Status.BAD_REQUEST).build();
    } else if ((insertRoomRequestDto.getType().equals(RoomTypeDto.GROUP)
            || insertRoomRequestDto.getType().equals(RoomTypeDto.TEMPORARY))
        && insertRoomRequestDto.getName() == null) {
      return Response.status(Status.BAD_REQUEST).build();
    } else {
      return Response.status(Status.CREATED)
          .entity(roomService.createRoom(insertRoomRequestDto, currentUser))
          .build();
    }
  }

  @Override
  @TimedCall
  public Response deleteRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    Optional<RoomDto> room = Optional.ofNullable(roomService.getRoomById(roomId, currentUser));
    return room.map(
            r -> {
              if (room.get().getType().equals(RoomTypeDto.ONE_TO_ONE)) {
                return Response.status(Status.FORBIDDEN).build();
              } else {
                roomService.deleteRoom(roomId, currentUser);
                return Response.status(Status.NO_CONTENT).build();
              }
            })
        .orElse(Response.status(Status.NOT_FOUND).build());
  }

  @Override
  @TimedCall
  public Response updateRoom(
      UUID roomId, RoomEditableFieldsDto updateRoomRequestDto, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    Optional<RoomDto> room = Optional.ofNullable(roomService.getRoomById(roomId, currentUser));
    return room.map(
            r -> {
              if (room.get().getType().equals(RoomTypeDto.ONE_TO_ONE)) {
                return Response.status(Status.BAD_REQUEST).build();
              } else if (room.get().getType().equals(RoomTypeDto.GROUP)
                  && (updateRoomRequestDto.getName() == null
                      && updateRoomRequestDto.getDescription() == null)) {
                return Response.status(Status.BAD_REQUEST).build();
              } else {
                return Response.status(Status.OK)
                    .entity(roomService.updateRoom(roomId, updateRoomRequestDto, currentUser))
                    .build();
              }
            })
        .orElse(Response.status(Status.NOT_FOUND).build());
  }

  @Override
  public Response updateRoomOwners(
      UUID roomId, List<@Valid MemberDto> memberDto, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
        .entity(membersService.updateRoomOwners(roomId, memberDto, currentUser))
        .build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response getRoomPicture(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    FileContentAndMetadata roomPicture = roomService.getRoomPicture(roomId, currentUser);
    return Response.status(Status.OK)
        .entity(roomPicture)
        .header("Content-Type", roomPicture.getMetadata().getMimeType())
        .header("Content-Length", roomPicture.getMetadata().getOriginalSize())
        .header(
            "Content-Disposition",
            String.format("inline; filename=\"%s\"", roomPicture.getMetadata().getName()))
        .build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response updateRoomPicture(
      UUID roomId,
      String headerFileName,
      String headerMimeType,
      Long contentLength,
      InputStream body,
      SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    String filename;
    try {
      filename =
          StringFormatUtils.decodeFromUtf8(
              Optional.of(headerFileName)
                  .orElseThrow(() -> new BadRequestException("File name not found")));
    } catch (UnsupportedEncodingException e) {
      throw new BadRequestException("Unable to decode the file name", e);
    }
    roomService.setRoomPicture(
        roomId,
        body,
        Optional.of(headerMimeType)
            .orElseThrow(() -> new BadRequestException("Mime type not found")),
        contentLength,
        filename,
        currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response deleteRoomPicture(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    roomService.deleteRoomPicture(roomId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response forwardMessages(
      UUID roomId, List<ForwardMessageDto> forwardMessageDto, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    roomService.forwardMessages(roomId, forwardMessageDto, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response muteRoom(UUID roomId, SecurityContext securityContext) {
    roomService.muteRoom(
        roomId,
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new));
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response unmuteRoom(UUID roomId, SecurityContext securityContext) {
    roomService.unmuteRoom(
        roomId,
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new));
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response clearRoomHistory(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
        .entity(
            ClearedDateDto.create().clearedAt(roomService.clearRoomHistory(roomId, currentUser)))
        .build();
  }

  @Override
  @TimedCall
  public Response listRoomMembers(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
        .entity(membersService.getRoomMembers(roomId, currentUser))
        .build();
  }

  @Override
  @TimedCall
  public Response insertRoomMembers(
      UUID roomId,
      List<@Valid MemberToInsertDto> memberToInsertDto,
      SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.CREATED)
        .entity(membersService.insertRoomMembers(roomId, memberToInsertDto, currentUser))
        .build();
  }

  @Override
  @TimedCall
  public Response deleteRoomMember(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    membersService.deleteRoomMember(roomId, userId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response insertOwner(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    membersService.setOwner(roomId, userId, true, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response deleteOwner(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    membersService.setOwner(roomId, userId, false, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response listRoomAttachmentsInfo(
      UUID roomId, Integer itemsNumber, String filter, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
        .entity(
            attachmentService.getAttachmentInfoByRoomId(roomId, itemsNumber, filter, currentUser))
        .build();
  }

  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response insertAttachment(
      UUID roomId,
      String fileName,
      String mimeType,
      Long contentLength,
      InputStream body,
      String description,
      String messageId,
      String replyId,
      String area,
      SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    String name;
    try {
      name =
          StringFormatUtils.decodeFromUtf8(
              Optional.of(fileName)
                  .orElseThrow(() -> new BadRequestException("File name not found")));
    } catch (UnsupportedEncodingException e) {
      throw new BadRequestException("Unable to decode the file name", e);
    }
    String desc;
    try {
      desc = StringFormatUtils.decodeFromUtf8(Optional.ofNullable(description).orElse(""));
    } catch (UnsupportedEncodingException e) {
      throw new BadRequestException("Unable to decode the description", e);
    }
    if (area == null || Pattern.compile("^(\\s)|^\\w|^\\d*+x+\\d*").matcher(area).matches()) {
      return Response.status(Status.CREATED)
          .entity(
              attachmentService.addAttachment(
                  roomId,
                  body,
                  Optional.of(mimeType)
                      .orElseThrow(() -> new BadRequestException("Mime type not found")),
                  contentLength,
                  name,
                  desc,
                  "".equals(messageId) ? null : messageId,
                  "".equals(replyId) ? null : replyId,
                  "".equals(area) ? null : area,
                  currentUser))
          .build();
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  @Override
  public Response getMeetingByRoomId(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser =
        Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
            .orElseThrow(UnauthorizedException::new);
    return Response.ok().entity(meetingService.getMeetingByRoomId(roomId, currentUser)).build();
  }
}
