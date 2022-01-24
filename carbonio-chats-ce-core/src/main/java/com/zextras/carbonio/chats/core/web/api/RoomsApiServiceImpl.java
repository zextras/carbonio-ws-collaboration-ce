package com.zextras.carbonio.chats.core.web.api;


import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.MockSecurityContext;
import com.zextras.carbonio.chats.core.web.security.MockUserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import java.io.File;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class RoomsApiServiceImpl implements RoomsApiService {

  private final RoomService         roomService;
  private final MembersService      membersService;
  private final AttachmentService   attachmentService;
  private final MockSecurityContext mockSecurityContext;

  @Inject
  public RoomsApiServiceImpl(
    RoomService roomService, MembersService membersService,
    AttachmentService attachmentService,
    MockSecurityContext mockSecurityContext
  ) {
    this.roomService = roomService;
    this.membersService = membersService;
    this.attachmentService = attachmentService;
    this.mockSecurityContext = mockSecurityContext;
  }

  @Override
  public Response listRoom(SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.OK)
      .entity(roomService.getRooms(currentUser))
      .build();
  }

  @Override
  public Response getRoom(UUID roomId, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(roomService.getRoomById(roomId, currentUser))
      .build();
  }

  @Override
  public Response insertRoom(RoomCreationFieldsDto insertRoomRequestDto, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.CREATED)
      .entity(roomService.createRoom(insertRoomRequestDto, currentUser))
      .build();
  }

  @Override
  public Response deleteRoom(UUID roomId, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    roomService.deleteRoom(roomId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response updateRoom(UUID roomId, RoomEditableFieldsDto updateRoomRequestDto, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(roomService.updateRoom(roomId, updateRoomRequestDto, currentUser))
      .build();
  }

  @Override
  public Response getRoomPicture(UUID roomId, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
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
  public Response updateRoomPicture(UUID roomId, String xContentDisposition, File body, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
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
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.OK)
      .entity(roomService.resetRoomHash(roomId, currentUser))
      .build();
  }

  @Override
  public Response muteRoom(UUID roomId, SecurityContext securityContext) {
    // TODO: 07/01/22  
    return Response.ok().build();
  }

  @Override
  public Response unmuteRoom(UUID roomId, SecurityContext securityContext) {
    // TODO: 07/01/22  
    return Response.ok().build();
  }

  @Override
  public Response listRoomMember(UUID roomId, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK)
      .entity(membersService.getRoomMembers(roomId, currentUser))
      .build();
  }

  @Override
  public Response insertRoomMember(UUID roomId, MemberDto memberDto, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.CREATED)
      .entity(membersService.insertRoomMember(roomId, memberDto, currentUser))
      .build();
  }

  @Override
  public Response deleteRoomMember(UUID roomId, UUID userId, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    membersService.deleteRoomMember(roomId, userId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response updateToOwner(UUID roomId, UUID userId, SecurityContext securityContext) {
    modifyOwner(roomId, userId, true);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response deleteOwner(UUID roomId, UUID userId, SecurityContext securityContext) {
    modifyOwner(roomId, userId, false);
    return Response.status(Status.NO_CONTENT).build();
  }

  private void modifyOwner(UUID roomId, UUID userId, boolean isOwner) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
      .orElseThrow(UnauthorizedException::new);
    membersService.setOwner(roomId, userId, isOwner, currentUser);
  }

  @Override
  public Response insertAttachment(UUID roomId, String xContentDisposition, File body, SecurityContext securityContext) {
    MockUserPrincipal currentUser = (MockUserPrincipal) mockSecurityContext.getUserPrincipal()
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
