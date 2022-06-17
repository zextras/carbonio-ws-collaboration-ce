// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import com.zextras.carbonio.chats.model.RoomRankDto;
import java.io.File;
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

  private final RoomService       roomService;
  private final MembersService    membersService;
  private final AttachmentService attachmentService;

  @Inject
  public RoomsApiServiceImpl(
    RoomService roomService, MembersService membersService,
    AttachmentService attachmentService
  ) {
    this.roomService = roomService;
    this.membersService = membersService;
    this.attachmentService = attachmentService;
  }

  @Override
  public Response listRoom(List<RoomExtraFieldDto> extraFields, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.OK)
      .entity(roomService.getRooms(extraFields, currentUser))
      .build();
  }

  @Override
  public Response getRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(roomService.getRoomById(roomId, currentUser))
      .build();
  }

  @Override
  public Response insertRoom(RoomCreationFieldsDto insertRoomRequestDto, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.CREATED)
      .entity(roomService.createRoom(insertRoomRequestDto, currentUser))
      .build();
  }

  @Override
  public Response deleteRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    roomService.deleteRoom(roomId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response updateRoom(UUID roomId, RoomEditableFieldsDto updateRoomRequestDto, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(roomService.updateRoom(roomId, updateRoomRequestDto, currentUser))
      .build();
  }

  @Override
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
  public Response updateRoomPicture(
    UUID roomId, String xContentDisposition, File body, SecurityContext securityContext
  ) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    roomService.setRoomPicture(roomId, body,
      getFilePropertyFromContentDisposition(xContentDisposition, "mimeType")
        .orElseThrow(() -> new BadRequestException("Mime type not found in X-Content-Disposition header")),
      getFilePropertyFromContentDisposition(xContentDisposition, "fileName")
        .orElseThrow(() -> new BadRequestException("File name not found in X-Content-Disposition header")),
      currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response resetRoomHash(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.OK)
      .entity(roomService.resetRoomHash(roomId, currentUser))
      .build();
  }

  @Override
  public Response muteRoom(UUID roomId, SecurityContext securityContext) {
    roomService.muteRoom(roomId, Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new));
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response unmuteRoom(UUID roomId, SecurityContext securityContext) {
    roomService.unmuteRoom(roomId, Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new));
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response listRoomMember(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(membersService.getRoomMembers(roomId, currentUser))
      .build();
  }

  @Override
  public Response insertRoomMember(UUID roomId, MemberDto memberDto, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.CREATED)
      .entity(membersService.insertRoomMember(roomId, memberDto, currentUser))
      .build();
  }

  @Override
  public Response deleteRoomMember(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    membersService.deleteRoomMember(roomId, userId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response updateToOwner(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    modifyOwner(roomId, userId, true, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response updateWorkspacesRank(List<RoomRankDto> roomRankDto, SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    roomService.updateWorkspacesRank(roomRankDto, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
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
  public Response listRoomAttachmentInfo(
    UUID roomId, Integer itemsNumber, String filter, SecurityContext securityContext
  ) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(attachmentService.getAttachmentInfoByRoomId(roomId, itemsNumber, filter, currentUser)).build();
  }

  @Override
  public Response insertAttachment(
    UUID roomId, String xContentDisposition, File body, SecurityContext securityContext
  ) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.CREATED)
      .entity(attachmentService.addAttachment(
        roomId,
        body,
        getFilePropertyFromContentDisposition(xContentDisposition, "mimeType")
          .orElseThrow(() -> new BadRequestException("Mime type not found in X-Content-Disposition header")),
        getFilePropertyFromContentDisposition(xContentDisposition, "fileName")
          .orElseThrow(() -> new BadRequestException("File name not found in X-Content-Disposition header")),
        currentUser))
      .build();
  }

  private Optional<String> getFilePropertyFromContentDisposition(String xContentDisposition, String property) {
    if (xContentDisposition.contains(property)) {
      String value = xContentDisposition.substring(xContentDisposition.indexOf(property) + property.length() + 1);
      if (value.contains(";")) {
        value = value.substring(0, value.indexOf(";"));
      }
      return Optional.of(value.trim());
    } else {
      return Optional.empty();
    }
  }

}
