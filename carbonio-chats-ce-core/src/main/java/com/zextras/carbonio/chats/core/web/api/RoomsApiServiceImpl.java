// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.NotFoundException;
import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

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
  public Response listRoom(SecurityContext securityContext) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response
      .status(Status.OK)
      .entity(roomService.getRooms(currentUser))
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
  public Response updateRoomPictureWithFile(
    UUID roomId,String xContentDisposition,Integer contentLength,File body,SecurityContext securityContext
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
  public Response updateRoomPictureWithStream(
    MultipartFormDataInput input, UUID roomId, String xContentDisposition, Integer contentLength,
    SecurityContext securityContext
  ) {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    InputPart inputPart = input.getFormDataMap().get("picture").get(0);
    String fileName = getFileName(inputPart.getHeaders())
      .orElseThrow(() -> new BadRequestException("File name not found"));
    String mimeType = inputPart.getMediaType().toString();
    InputStream inputStream = null;
    try {
      inputStream = inputPart.getBody(InputStream.class, null);
    } catch (IOException e) {
      throw new BadRequestException("Unable to get the file content", e);
    }
    roomService.setRoomPicture(roomId, inputStream, mimeType, fileName, contentLength, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }


  @Override
  public Response fakeUpdateRoomPicture(
    MultipartFormDataInput input, UUID roomId, String xContentDisposition, Integer contentLength,
    SecurityContext securityContext
  ) {
    InputPart inputPart = input.getFormDataMap().get("picture").get(0);
    String fileName = getFileName(inputPart.getHeaders())
      .orElseThrow(() -> new BadRequestException("File name not found"));
    InputStream inputStream;
    try {
      inputStream = inputPart.getBody(InputStream.class, null);
    } catch (IOException e) {
      throw new BadRequestException("Unable to get the file content", e);
    }
    byte[] bytes = new byte[0];
    try {
      bytes = IOUtils.toByteArray(inputStream);
    } catch (IOException e) {
      throw new InternalErrorException("Unable to convert file input stream to a byte array", e);
    }
    String folder = String.format("%s/chats-tests", System.getProperty("user.home"));
    try {
      Files.createDirectories(Paths.get(folder));
    } catch (IOException e) {
      throw new InternalErrorException(String.format("Unable to create the folder %s", folder), e);
    }
    String fileFullName = String.join("/", folder, fileName);
    try {
      File file = new File(fileFullName);
      if (!file.exists()) {
        file.createNewFile();
      }
      FileOutputStream fop = new FileOutputStream(file);
      fop.write(bytes);
      fop.flush();
      fop.close();
    } catch (IOException e) {
      throw new InternalErrorException(String.format("Unable to create the file %s", fileFullName), e);
    }
    return Response.status(Status.NO_CONTENT).build();
  }

  private Optional<String> getFileName(MultivaluedMap<String, String> header) {
    String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
    for (String filename : contentDisposition) {
      if ((filename.trim().startsWith("filename"))) {
        String[] name = filename.split("=");
        String finalFileName = name[1].trim().replaceAll("\"", "");
        return Optional.of(finalFileName);
      }
    }
    return Optional.empty();
  }

  private InputStream getBinaryArtifact(MultipartFormDataInput input) {
    if (input == null || input.getParts() == null || input.getParts().isEmpty()) {
      throw new IllegalArgumentException("Multipart request is empty");
    }

    try {
      final InputStream result = input.getFormDataPart("picture", InputStream.class, null);

      if (result == null) {
        throw new IllegalArgumentException("Can't find a valid 'file' part in the multipart request");
      }

      return result;
    } catch (IOException e) {
      throw new BadRequestException("Error while reading multipart request", e);
    }
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
  public Response listRoomAttachmentInfo(UUID roomId, SecurityContext securityContext) throws NotFoundException {
    UserPrincipal currentUser = Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
      .orElseThrow(UnauthorizedException::new);
    return Response.status(Status.OK).entity(attachmentService.getAttachmentInfoByRoomId(roomId, currentUser)).build();
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
