// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.data.model.AttachmentFilter;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentOrderDto;
import com.zextras.carbonio.chats.model.AttachmentSortByDto;
import com.zextras.carbonio.chats.model.BulkDeleteAttachmentsRequestDto;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Singleton
public class RoomsApiServiceImpl implements RoomsApiService {

  public static final String MIME_TYPE_NOT_FOUND = "Mime type not found";
  public static final String FILE_NAME_NOT_FOUND = "File name not found";
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

  private static UserPrincipal getCurrentUser(SecurityContext securityContext) {
    return Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new);
  }

  @Override
  @TimedCall
  public Response listRooms(List<RoomExtraFieldDto> extraFields, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    return Response.status(Status.OK)
        .entity(roomService.getRooms(extraFields, currentUser))
        .build();
  }

  @Override
  @TimedCall
  public Response getRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    return Response.status(Status.OK).entity(roomService.getRoomById(roomId, currentUser)).build();
  }

  @Override
  @TimedCall
  public Response insertRoom(
      RoomCreationFieldsDto insertRoomRequestDto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    List<MemberDto> members =
        Optional.ofNullable(insertRoomRequestDto.getMembers()).orElse(List.of());
    if (RoomTypeDto.ONE_TO_ONE.equals(insertRoomRequestDto.getType())) {
      if (!currentUser.hasEnabled(CarbonioAttribute.WSC_PRIVATE_CHAT_CREATION)) {
        throw new ForbiddenException("Private chat creation is disabled for user");
      }
      if (members.size() != 1) {
        return Response.status(Status.BAD_REQUEST).build();
      }
      if (insertRoomRequestDto.getName() != null || insertRoomRequestDto.getDescription() != null) {
        return Response.status(Status.BAD_REQUEST).build();
      }
    }
    if (RoomTypeDto.GROUP.equals(insertRoomRequestDto.getType())) {
      if (!currentUser.hasEnabled(CarbonioAttribute.WSC_GROUP_CHAT_CREATION)) {
        throw new ForbiddenException("Group chat creation is disabled for user");
      }
      if (members.size() < 2) {
        return Response.status(Status.BAD_REQUEST).build();
      }
      int maxMembers = currentUser.getCountLimit(CarbonioAttribute.WSC_MAX_GROUP_MEMBERS, 0);
      if (members.size() > maxMembers) {
        throw new ForbiddenException("Exceeded the max number of group members for user");
      }
    }
    if (List.of(RoomTypeDto.GROUP, RoomTypeDto.TEMPORARY)
        .contains(insertRoomRequestDto.getType())) {
      if (insertRoomRequestDto.getName() == null) {
        return Response.status(Status.BAD_REQUEST).build();
      }
    }
    return Response.status(Status.CREATED)
        .entity(roomService.createRoom(insertRoomRequestDto, currentUser))
        .build();
  }

  @Override
  @TimedCall
  public Response deleteRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    Optional<RoomDto> room = Optional.ofNullable(roomService.getRoomById(roomId, currentUser));
    return room.map(
            r -> {
              if (RoomTypeDto.ONE_TO_ONE.equals(room.get().getType())) {
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
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    Optional<RoomDto> room = Optional.ofNullable(roomService.getRoomById(roomId, currentUser));
    return room.map(
            r -> {
              if (RoomTypeDto.ONE_TO_ONE.equals(room.get().getType())) {
                return Response.status(Status.BAD_REQUEST).build();
              }
              if (RoomTypeDto.GROUP.equals(room.get().getType())) {
                if (updateRoomRequestDto.getName() == null
                    && updateRoomRequestDto.getDescription() == null) {
                  return Response.status(Status.BAD_REQUEST).build();
                }
              }
              return Response.status(Status.OK)
                  .entity(roomService.updateRoom(roomId, updateRoomRequestDto, currentUser))
                  .build();
            })
        .orElse(Response.status(Status.NOT_FOUND).build());
  }

  @Override
  public Response updateRoomOwners(
      UUID roomId, List<@Valid MemberDto> memberDto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    Optional<RoomDto> room = Optional.ofNullable(roomService.getRoomById(roomId, currentUser));
    return room.map(
            r -> {
              if (RoomTypeDto.ONE_TO_ONE.equals(room.get().getType())) {
                return Response.status(Status.BAD_REQUEST).build();
              }
              return Response.status(Status.OK)
                  .entity(membersService.updateRoomOwners(roomId, memberDto, currentUser))
                  .build();
            })
        .orElse(Response.status(Status.NOT_FOUND).build());
  }

  @Override
  @TimedCall
  public Response getRoomPicture(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
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
  @TimedCall
  public Response updateRoomPicture(
      UUID roomId,
      String headerFileName,
      String headerMimeType,
      Long contentLength,
      InputStream body,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }

    long maxSizeBytes =
        currentUser.getSizeLimit(CarbonioAttribute.WSC_MAX_ROOM_PICTURE_SIZE, 0).toBytes();

    if (contentLength == null || contentLength < 0) {
      throw new BadRequestException("Content length is missing or invalid");
    }

    if (contentLength > maxSizeBytes) {
      throw new ForbiddenException("Room picture size exceeds the max limit");
    }

    String filename;
    try {
      filename =
          StringFormatUtils.decodeFromUtf8(
              Optional.ofNullable(headerFileName)
                  .orElseThrow(() -> new BadRequestException(FILE_NAME_NOT_FOUND)));
    } catch (UnsupportedEncodingException e) {
      throw new BadRequestException("Unable to decode the file name", e);
    }
    roomService.setRoomPicture(
        roomId,
        body,
        Optional.ofNullable(headerMimeType)
            .orElseThrow(() -> new BadRequestException(MIME_TYPE_NOT_FOUND)),
        contentLength,
        filename,
        currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response deleteRoomPicture(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    roomService.deleteRoomPicture(roomId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response forwardMessages(
      UUID roomId, List<ForwardMessageDto> forwardMessageDto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    roomService.forwardMessages(roomId, forwardMessageDto, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response muteRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    roomService.muteRoom(roomId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response unmuteRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    roomService.unmuteRoom(roomId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response clearRoomHistory(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    return Response.status(Status.OK)
        .entity(
            ClearedDateDto.create().clearedAt(roomService.clearRoomHistory(roomId, currentUser)))
        .build();
  }

  @Override
  @TimedCall
  public Response listRoomMembers(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
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
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    Optional<RoomDto> room = Optional.ofNullable(roomService.getRoomById(roomId, currentUser));
    return room.map(
            r -> {
              if (RoomTypeDto.ONE_TO_ONE.equals(room.get().getType())) {
                return Response.status(Status.BAD_REQUEST).build();
              }
              if (RoomTypeDto.GROUP.equals(room.get().getType())) {
                int maxMembers =
                    currentUser.getCountLimit(CarbonioAttribute.WSC_MAX_GROUP_MEMBERS, 0);
                if ((r.getMembers().size() + memberToInsertDto.size()) > maxMembers) {
                  return Response.status(Status.BAD_REQUEST).build();
                }
              }
              return Response.status(Status.CREATED)
                  .entity(membersService.insertRoomMembers(roomId, memberToInsertDto, currentUser))
                  .build();
            })
        .orElse(Response.status(Status.NOT_FOUND).build());
  }

  @Override
  @TimedCall
  public Response deleteRoomMember(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    Optional<RoomDto> room = Optional.ofNullable(roomService.getRoomById(roomId, currentUser));
    return room.map(
            r -> {
              if (RoomTypeDto.ONE_TO_ONE.equals(room.get().getType())) {
                return Response.status(Status.BAD_REQUEST).build();
              }
              membersService.deleteRoomMember(roomId, userId, currentUser);
              return Response.status(Status.NO_CONTENT).build();
            })
        .orElse(Response.status(Status.NOT_FOUND).build());
  }

  @Override
  @TimedCall
  public Response insertOwner(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    Optional<RoomDto> room = Optional.ofNullable(roomService.getRoomById(roomId, currentUser));
    return room.map(
            r -> {
              if (RoomTypeDto.ONE_TO_ONE.equals(room.get().getType())) {
                return Response.status(Status.BAD_REQUEST).build();
              }
              membersService.promoteMemberToOwner(roomId, userId, currentUser);
              return Response.status(Status.NO_CONTENT).build();
            })
        .orElse(Response.status(Status.NOT_FOUND).build());
  }

  @Override
  @TimedCall
  public Response deleteOwner(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    Optional<RoomDto> room = Optional.ofNullable(roomService.getRoomById(roomId, currentUser));
    return room.map(
            r -> {
              if (RoomTypeDto.ONE_TO_ONE.equals(room.get().getType())) {
                return Response.status(Status.BAD_REQUEST).build();
              }
              membersService.demoteOwnerToMember(roomId, userId, currentUser);
              return Response.status(Status.NO_CONTENT).build();
            })
        .orElse(Response.status(Status.NOT_FOUND).build());
  }

  @Override
  @TimedCall
  public Response listRoomAttachmentsInfo(
      UUID roomId,
      Integer limit,
      String cursor,
      UUID userId,
      String mimeType,
      OffsetDateTime createdAfter,
      OffsetDateTime createdBefore,
      Long minSize,
      Long maxSize,
      AttachmentSortByDto sortBy,
      AttachmentOrderDto order,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    AttachmentFilter attachmentFilter =
        AttachmentFilter.create()
            .userId(userId != null ? userId.toString() : null)
            .mimeType(mimeType)
            .createdAfter(createdAfter)
            .createdBefore(createdBefore)
            .minSize(minSize)
            .maxSize(maxSize)
            .sortBy(sortBy != null ? sortBy.toString() : null)
            .order(order != null ? order.toString() : null);
    return Response.status(Status.OK)
        .entity(
            attachmentService.getAttachmentInfoByRoomId(
                roomId, limit, cursor, attachmentFilter, currentUser))
        .build();
  }

  @Override
  @TimedCall
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
    UserPrincipal currentUser = getCurrentUser(securityContext);

    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }
    if (!currentUser.hasEnabled(CarbonioAttribute.WSC_ATTACHMENT_UPLOAD)) {
      throw new ForbiddenException("Attachment upload is disabled for user");
    }

    long maxSizeBytes =
        currentUser.getSizeLimit(CarbonioAttribute.WSC_MAX_ATTACHMENT_SIZE, 0).toBytes();

    if (contentLength == null || contentLength < 0) {
      throw new BadRequestException("Content length is missing or invalid");
    }

    if (maxSizeBytes > 0 && contentLength > maxSizeBytes) {
      throw new ForbiddenException("Attachment size exceeds the max limit");
    }

    String name;
    try {
      name =
          StringFormatUtils.decodeFromUtf8(
              Optional.ofNullable(fileName)
                  .orElseThrow(() -> new BadRequestException(FILE_NAME_NOT_FOUND)));
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
                  Optional.ofNullable(mimeType)
                      .orElseThrow(() -> new BadRequestException(MIME_TYPE_NOT_FOUND)),
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
  @TimedCall
  public Response insertAttachmentMultipart(
      MultipartFormDataInput input, UUID roomId, SecurityContext securityContext) {

    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (UserType.GUEST.equals(currentUser.getUserType())) {
      throw new ForbiddenException();
    }

    if (!currentUser.hasEnabled(CarbonioAttribute.WSC_ATTACHMENT_UPLOAD)) {
      throw new ForbiddenException("Attachment upload is disabled for user");
    }

    Map<String, List<InputPart>> formDataMap = input.getFormDataMap();

    InputPart filePart = getFormPart(formDataMap, "file");
    if (filePart == null) {
      return Response.status(Status.BAD_REQUEST).entity("File not found").build();
    }

    String contentLength = getStringFromPart(formDataMap, "contentLength");

    if (contentLength == null || contentLength.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity("Content length not found").build();
    }

    long maxSizeBytes =
        currentUser.getSizeLimit(CarbonioAttribute.WSC_MAX_ATTACHMENT_SIZE, 0).toBytes();
    long contentLengthBytes;
    try {
      contentLengthBytes = Long.parseLong(contentLength);
    } catch (NumberFormatException e) {
      throw new BadRequestException("Invalid content length format");
    }

    if (contentLengthBytes < 0) {
      throw new BadRequestException("Content length cannot be negative");
    }

    if (maxSizeBytes > 0 && contentLengthBytes > maxSizeBytes) {
      throw new ForbiddenException("Attachment size exceeds the max limit");
    }

    String fileName = filePart.getFileName();
    MediaType mediaType = filePart.getMediaType();

    if (fileName == null || fileName.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity(FILE_NAME_NOT_FOUND).build();
    }

    if (mediaType == null || mediaType.toString().isEmpty()) {
      return Response.status(Status.BAD_REQUEST).entity(MIME_TYPE_NOT_FOUND).build();
    }

    String mimeType = mediaType.toString();
    String description = getStringFromPart(formDataMap, "description");
    String messageId = getStringFromPart(formDataMap, "messageId");
    String replyId = getStringFromPart(formDataMap, "replyId");
    String area = getStringFromPart(formDataMap, "area");

    if (area != null && !isValidArea(area)) {
      return Response.status(Status.BAD_REQUEST).entity("Invalid area format").build();
    }

    try {
      return Response.status(Status.CREATED)
          .entity(
              attachmentService.addAttachment(
                  roomId,
                  filePart.getBody(InputStream.class, null),
                  mimeType,
                  Long.parseLong(contentLength),
                  StringFormatUtils.decodeFromUtf8(fileName),
                  normalizeStringValue(description),
                  normalizeStringValue(messageId),
                  normalizeStringValue(replyId),
                  normalizeStringValue(area),
                  currentUser))
          .build();
    } catch (Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error processing file").build();
    }
  }

  private InputPart getFormPart(Map<String, List<InputPart>> formParts, String fieldName) {
    List<InputPart> parts = formParts.get(fieldName);
    return (parts != null && !parts.isEmpty()) ? parts.get(0) : null;
  }

  private String getStringFromPart(Map<String, List<InputPart>> formParts, String fieldName) {
    try {
      InputPart part = getFormPart(formParts, fieldName);
      return part != null ? part.getBody(String.class, null) : null;
    } catch (Exception e) {
      return null;
    }
  }

  private boolean isValidArea(String area) {
    return !area.isEmpty() && Pattern.compile("^(\\s)|^\\w|^\\d*+x+\\d*").matcher(area).matches();
  }

  private String normalizeStringValue(String value) {
    return (value == null || value.isEmpty()) ? null : value;
  }

  @Override
  @TimedCall
  public Response bulkDeleteRoomAttachments(
      UUID roomId,
      BulkDeleteAttachmentsRequestDto bulkDeleteAttachmentsRequestDto,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    return Response.ok()
        .entity(
            attachmentService.bulkDeleteRoomAttachments(
                roomId, bulkDeleteAttachmentsRequestDto.getAttachmentIds(), currentUser))
        .build();
  }

  @Override
  public Response getMeetingByRoomId(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    return Response.ok().entity(meetingService.getMeetingByRoomId(roomId, currentUser)).build();
  }
}
