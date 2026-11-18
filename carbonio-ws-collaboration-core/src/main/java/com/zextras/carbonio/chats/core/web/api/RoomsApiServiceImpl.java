// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.model.AttachmentFilter;
import com.zextras.carbonio.chats.core.data.model.AttachmentsPage;
import com.zextras.carbonio.chats.core.data.model.BulkDeleteResult;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.model.InsertedMember;
import com.zextras.carbonio.chats.core.data.model.MessageContext;
import com.zextras.carbonio.chats.core.data.model.MessageSearchPage;
import com.zextras.carbonio.chats.core.data.model.PinnedMessagesView;
import com.zextras.carbonio.chats.core.data.model.ReadMarkersView;
import com.zextras.carbonio.chats.core.data.model.TimelineView;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.data.type.MimeTypeCategory;
import com.zextras.carbonio.chats.core.data.type.RoomType;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.event.mapper.TimelineItemMapper;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.ChatExportService;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomAccess;
import com.zextras.carbonio.chats.core.service.RoomLifecycleService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.core.web.api.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.web.api.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.web.api.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.web.api.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentOrderDto;
import com.zextras.carbonio.chats.model.AttachmentSortByDto;
import com.zextras.carbonio.chats.model.AttachmentsPaginationDto;
import com.zextras.carbonio.chats.model.BulkDeleteAttachmentsRequestDto;
import com.zextras.carbonio.chats.model.BulkDeleteAttachmentsResponseDto;
import com.zextras.carbonio.chats.model.ClearedDateDto;
import com.zextras.carbonio.chats.model.DeletedInfoDto;
import com.zextras.carbonio.chats.model.EditedInfoDto;
import com.zextras.carbonio.chats.model.ForwardMessageRequestDto;
import com.zextras.carbonio.chats.model.ForwardMessagesRequestDto;
import com.zextras.carbonio.chats.model.ForwardedInfoDto;
import com.zextras.carbonio.chats.model.MarkReadRequestDto;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberInsertedDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.MessageDto;
import com.zextras.carbonio.chats.model.MessageSearchResultDto;
import com.zextras.carbonio.chats.model.MimeTypeCategoryDto;
import com.zextras.carbonio.chats.model.PinnedMessageDto;
import com.zextras.carbonio.chats.model.ReadMarkerDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.chats.model.SendMessageRequestDto;
import com.zextras.carbonio.chats.model.SetReactionRequestDto;
import com.zextras.carbonio.chats.model.TimelineDto;
import com.zextras.carbonio.chats.model.TimelineItemDto;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Singleton
public class RoomsApiServiceImpl implements RoomsApiService {

  public static final String MIME_TYPE_NOT_FOUND = "Mime type not found";
  public static final String FILE_NAME_NOT_FOUND = "File name not found";
  private final RoomService roomService;
  private final RoomAccess roomAccess;
  private final RoomLifecycleService roomLifecycleService;
  private final MembersService membersService;
  private final AttachmentService attachmentService;
  private final MeetingService meetingService;
  private final ChatMessageService chatMessageService;
  private final ChatExportService chatExportService;
  private final TimelineItemMapper timelineItemMapper;
  private final RoomMapper roomMapper;
  private final SubscriptionMapper subscriptionMapper;
  private final AttachmentMapper attachmentMapper;
  private final MeetingMapper meetingMapper;

  @Inject
  public RoomsApiServiceImpl(
      RoomService roomService,
      RoomAccess roomAccess,
      RoomLifecycleService roomLifecycleService,
      MembersService membersService,
      AttachmentService attachmentService,
      MeetingService meetingService,
      ChatMessageService chatMessageService,
      ChatExportService chatExportService,
      TimelineItemMapper timelineItemMapper,
      RoomMapper roomMapper,
      SubscriptionMapper subscriptionMapper,
      AttachmentMapper attachmentMapper,
      MeetingMapper meetingMapper) {
    this.roomService = roomService;
    this.roomAccess = roomAccess;
    this.roomLifecycleService = roomLifecycleService;
    this.membersService = membersService;
    this.attachmentService = attachmentService;
    this.meetingService = meetingService;
    this.chatMessageService = chatMessageService;
    this.chatExportService = chatExportService;
    this.timelineItemMapper = timelineItemMapper;
    this.roomMapper = roomMapper;
    this.subscriptionMapper = subscriptionMapper;
    this.attachmentMapper = attachmentMapper;
    this.meetingMapper = meetingMapper;
  }

  private static void rejectGuest(UserPrincipal user) {
    if (UserType.GUEST.equals(user.getUserType())) {
      throw new ForbiddenException();
    }
  }

  private static UserPrincipal getCurrentUser(SecurityContext securityContext) {
    return Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new);
  }

  @Override
  @TimedCall
  public Response listRooms(List<RoomExtraFieldDto> extraFields, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    boolean includeMembers = extraFields != null && extraFields.contains(RoomExtraFieldDto.MEMBERS);
    boolean includeSettings =
        extraFields != null && extraFields.contains(RoomExtraFieldDto.SETTINGS);
    List<Room> rooms = roomService.getRooms(extraFields, currentUser);
    Map<String, RoomUserSettings> settingsByRoomId =
        includeSettings ? roomService.getRoomsUserSettings(currentUser) : null;
    return Response.status(Status.OK)
        .entity(roomMapper.ent2dto(rooms, settingsByRoomId, includeMembers, includeSettings))
        .build();
  }

  @Override
  @TimedCall
  public Response getRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    Room room = roomAccess.loadForMember(roomId, currentUser);
    RoomUserSettings settings = roomService.getRoomUserSettings(roomId, currentUser).orElse(null);
    return Response.status(Status.OK)
        .entity(roomMapper.ent2dto(room, settings, true, true))
        .build();
  }

  @Override
  @TimedCall
  public Response insertRoom(
      RoomCreationFieldsDto insertRoomRequestDto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    rejectGuest(currentUser);
    if (insertRoomRequestDto.getType() == null) {
      throw new BadRequestException("Room type is required");
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
    Room createdRoom = roomService.createRoom(insertRoomRequestDto, currentUser);
    RoomUserSettings createdRoomSettings =
        createdRoom.getUserSettings().stream()
            .filter(settings -> settings.getUserId().equals(currentUser.getId()))
            .findAny()
            .orElse(null);
    return Response.status(Status.CREATED)
        .entity(roomMapper.ent2dto(createdRoom, createdRoomSettings, true, true))
        .build();
  }

  @Override
  @TimedCall
  public Response deleteRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    rejectGuest(currentUser);
    Room room = roomAccess.loadForMember(roomId, currentUser);
    if (RoomType.ONE_TO_ONE.equals(room.getType())) {
      return Response.status(Status.FORBIDDEN).build();
    }
    roomLifecycleService.deleteRoom(roomId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response updateRoom(
      UUID roomId, RoomEditableFieldsDto updateRoomRequestDto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    rejectGuest(currentUser);
    Room room = roomAccess.loadForMember(roomId, currentUser);
    if (RoomType.ONE_TO_ONE.equals(room.getType())) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (RoomType.GROUP.equals(room.getType())
        && updateRoomRequestDto.getName() == null
        && updateRoomRequestDto.getDescription() == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    return Response.status(Status.OK)
        .entity(
            roomMapper.ent2dto(
                roomService.updateRoom(roomId, updateRoomRequestDto, currentUser),
                null,
                false,
                false))
        .build();
  }

  @Override
  public Response updateRoomOwners(
      UUID roomId, List<@Valid MemberDto> memberDto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    rejectGuest(currentUser);
    Room room = roomAccess.loadForMember(roomId, currentUser);
    if (RoomType.ONE_TO_ONE.equals(room.getType())) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    return Response.status(Status.OK)
        .entity(
            subscriptionMapper.ent2memberDto(
                membersService.updateRoomOwners(roomId, memberDto, currentUser)))
        .build();
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
    rejectGuest(currentUser);

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
    rejectGuest(currentUser);
    roomService.deleteRoomPicture(roomId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response muteRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    rejectGuest(currentUser);
    roomService.muteRoom(roomId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response unmuteRoom(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    rejectGuest(currentUser);
    roomService.unmuteRoom(roomId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response clearRoomHistory(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    rejectGuest(currentUser);
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
        .entity(
            subscriptionMapper.ent2memberDto(membersService.getRoomMembers(roomId, currentUser)))
        .build();
  }

  @Override
  @TimedCall
  public Response insertRoomMembers(
      UUID roomId,
      List<@Valid MemberToInsertDto> memberToInsertDto,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    rejectGuest(currentUser);
    if (memberToInsertDto.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    Room room = roomAccess.loadForMember(roomId, currentUser);
    if (RoomType.ONE_TO_ONE.equals(room.getType())) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (RoomType.GROUP.equals(room.getType())) {
      int maxMembers = currentUser.getCountLimit(CarbonioAttribute.WSC_MAX_GROUP_MEMBERS, 0);
      long newMembers =
          memberToInsertDto.stream().map(MemberToInsertDto::getUserId).distinct().count();
      if ((room.getSubscriptions().size() + newMembers) > maxMembers) {
        return Response.status(Status.BAD_REQUEST).build();
      }
    }
    return Response.status(Status.CREATED)
        .entity(
            membersService.insertRoomMembers(roomId, memberToInsertDto, currentUser).stream()
                .map(this::toMemberInsertedDto)
                .toList())
        .build();
  }

  @Override
  @TimedCall
  public Response deleteRoomMember(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    rejectGuest(currentUser);
    Room room = roomAccess.loadForMember(roomId, currentUser);
    if (RoomType.ONE_TO_ONE.equals(room.getType())) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    roomLifecycleService.removeMember(roomId, userId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response insertOwner(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    rejectGuest(currentUser);
    Room room = roomAccess.loadForMember(roomId, currentUser);
    if (RoomType.ONE_TO_ONE.equals(room.getType())) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    membersService.promoteMemberToOwner(roomId, userId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response deleteOwner(UUID roomId, UUID userId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    rejectGuest(currentUser);
    Room room = roomAccess.loadForMember(roomId, currentUser);
    if (RoomType.ONE_TO_ONE.equals(room.getType())) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    membersService.demoteOwnerToMember(roomId, userId, currentUser);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  @TimedCall
  public Response listRoomAttachmentsInfo(
      UUID roomId,
      Integer limit,
      String cursor,
      UUID userId,
      String mimeType,
      MimeTypeCategoryDto mimeTypeCategory,
      OffsetDateTime createdAfter,
      OffsetDateTime createdBefore,
      Long minSize,
      Long maxSize,
      AttachmentSortByDto sortBy,
      AttachmentOrderDto order,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    if (mimeType != null && mimeTypeCategory != null) {
      throw new BadRequestException("mimeType and mimeTypeCategory filters are mutually exclusive");
    }
    AttachmentFilter attachmentFilter =
        AttachmentFilter.create()
            .userId(userId != null ? userId.toString() : null)
            .mimeType(mimeType)
            .mimeTypeCategory(
                mimeTypeCategory != null ? MimeTypeCategory.valueOf(mimeTypeCategory.name()) : null)
            .createdAfter(createdAfter)
            .createdBefore(createdBefore)
            .minSize(minSize)
            .maxSize(maxSize)
            .sortBy(sortBy != null ? sortBy.toString() : null)
            .order(order != null ? order.toString() : null);
    AttachmentsPage page =
        attachmentService.getAttachmentInfoByRoomId(
            roomId, limit, cursor, attachmentFilter, currentUser);
    return Response.status(Status.OK)
        .entity(
            AttachmentsPaginationDto.create()
                .total(page.total())
                .attachments(attachmentMapper.ent2dto(page.attachments()))
                .cursor(page.cursor()))
        .build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response bulkDeleteRoomAttachments(
      UUID roomId,
      BulkDeleteAttachmentsRequestDto bulkDeleteAttachmentsRequestDto,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    BulkDeleteResult result =
        attachmentService.bulkDeleteRoomAttachments(
            roomId, bulkDeleteAttachmentsRequestDto.getAttachmentIds(), currentUser);
    return Response.ok()
        .entity(
            BulkDeleteAttachmentsResponseDto.create()
                .successIds(result.successIds())
                .failedIds(result.failedIds()))
        .build();
  }

  @Override
  @TimedCall
  public Response insertAttachmentMultipart(
      MultipartFormDataInput input, UUID roomId, SecurityContext securityContext) {

    UserPrincipal currentUser = getCurrentUser(securityContext);
    rejectGuest(currentUser);

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

    String decodedFileName;
    try {
      decodedFileName = StringFormatUtils.decodeFileName(fileName);
    } catch (UnsupportedEncodingException e) {
      throw new BadRequestException("Unable to decode the file name", e);
    }
    if (decodedFileName.codePointCount(0, decodedFileName.length()) > 256) {
      throw new BadRequestException("The file name must be at most 256 characters");
    }

    String mimeType = mediaType.toString();
    String description = getStringFromPart(formDataMap, "description");
    String replyToId = getStringFromPart(formDataMap, "replyToId");
    String area = getStringFromPart(formDataMap, "area");
    String tempId = getStringFromPart(formDataMap, "tempId");

    if (tempId != null && tempId.length() > ChatsConstant.MAX_TEMP_ID_LENGTH) {
      throw new BadRequestException(
          String.format("tempId must be at most %d characters", ChatsConstant.MAX_TEMP_ID_LENGTH));
    }
    if (description != null && description.length() > ChatsConstant.MAX_MESSAGE_TEXT_LENGTH) {
      throw new BadRequestException(
          String.format(
              "description must be at most %d characters", ChatsConstant.MAX_MESSAGE_TEXT_LENGTH));
    }

    if (area != null && !isValidArea(area)) {
      return Response.status(Status.BAD_REQUEST).entity("Invalid area format").build();
    }

    Message created;
    try {
      created =
          chatMessageService.uploadAttachment(
              roomId.toString(),
              filePart.getBody(InputStream.class, null),
              mimeType,
              Long.parseLong(contentLength),
              decodedFileName,
              normalizeStringValue(description),
              normalizeStringValue(replyToId),
              normalizeStringValue(area),
              normalizeStringValue(tempId),
              currentUser);
    } catch (ChatsHttpException e) {
      throw e;
    } catch (Exception e) {
      ChatsLogger.error("Error processing multipart attachment upload", e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error processing file").build();
    }
    return Response.status(Status.CREATED).entity(toMessageDto(created)).build();
  }

  private MemberInsertedDto toMemberInsertedDto(InsertedMember member) {
    return MemberInsertedDto.create()
        .userId(member.userId())
        .owner(member.owner())
        .clearedAt(member.clearedAt());
  }

  private InputPart getFormPart(Map<String, List<InputPart>> formParts, String fieldName) {
    List<InputPart> parts = formParts.get(fieldName);
    return (parts != null && !parts.isEmpty()) ? parts.get(0) : null;
  }

  private String getStringFromPart(Map<String, List<InputPart>> formParts, String fieldName) {
    try {
      InputPart part = getFormPart(formParts, fieldName);
      if (part == null) {
        return null;
      }
      if (!part.isContentTypeFromMessage()) {
        part.setMediaType(MediaType.TEXT_PLAIN_TYPE.withCharset("UTF-8"));
      }
      return part.getBody(String.class, null);
    } catch (Exception e) {
      return null;
    }
  }

  private boolean isValidArea(String area) {
    return !area.isEmpty()
        && area.length() <= 64
        && Pattern.compile("^(\\s)|^\\w|^\\d*+x+\\d*").matcher(area).matches();
  }

  private String normalizeStringValue(String value) {
    return (value == null || value.isEmpty()) ? null : value;
  }

  @Override
  public Response getMeetingByRoomId(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    return Response.ok()
        .entity(meetingMapper.ent2dto(meetingService.getMeetingByRoomId(roomId, currentUser)))
        .build();
  }

  @Override
  public Response pinMessage(UUID roomId, UUID messageId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    chatMessageService.pinMessage(roomId.toString(), messageId.toString(), currentUser.getId());
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response unpinMessage(UUID roomId, UUID messageId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    chatMessageService.unpinMessage(roomId.toString(), messageId.toString(), currentUser.getId());
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response setReaction(
      UUID roomId,
      UUID messageId,
      SetReactionRequestDto setReactionRequest,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    String reaction = setReactionRequest.getReaction();
    if (reaction.isBlank()) {
      throw new BadRequestException("Reaction must not be blank");
    }
    chatMessageService.setReaction(roomId.toString(), messageId.toString(), currentUser, reaction);
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response removeReaction(UUID roomId, UUID messageId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    chatMessageService.removeReaction(roomId.toString(), messageId.toString(), currentUser.getId());
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response sendMessage(
      UUID roomId, SendMessageRequestDto dto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    String replyToId = dto.getReplyToId() != null ? dto.getReplyToId().toString() : null;
    try {
      Message sent =
          chatMessageService.sendMessage(
              roomId.toString(), currentUser, dto.getText(), replyToId, dto.getTempId());
      return Response.status(Status.CREATED).entity(toMessageDto(sent)).build();
    } catch (ForbiddenException | NotFoundException e) {
      return rejection(e);
    }
  }

  @Override
  public Response forwardMessage(
      UUID roomId, UUID messageId, ForwardMessageRequestDto dto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    return Response.status(Status.CREATED)
        .entity(forwardSingle(roomId, messageId, dto.getToRoomId(), currentUser))
        .build();
  }

  @Override
  public Response forwardMessages(
      UUID targetRoomId, ForwardMessagesRequestDto dto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    List<MessageDto> results = new ArrayList<>();
    for (var item : dto.getMessages()) {
      results.add(
          forwardSingle(item.getSourceRoomId(), item.getMessageId(), targetRoomId, currentUser));
    }
    return Response.status(Status.CREATED).entity(results).build();
  }

  private MessageDto forwardSingle(
      UUID sourceRoomId, UUID messageId, UUID toRoomId, UserPrincipal currentUser) {
    return toMessageDto(
        chatMessageService.forwardMessage(
            sourceRoomId.toString(), messageId.toString(), toRoomId.toString(), currentUser));
  }

  @Override
  public Response deleteMessage(UUID roomId, UUID messageId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    try {
      chatMessageService.deleteMessage(roomId.toString(), messageId.toString(), currentUser);
    } catch (ForbiddenException e) {
      return rejection(e);
    }
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response editMessage(
      UUID roomId,
      UUID messageId,
      com.zextras.carbonio.chats.model.EditMessageRequestDto editMessageRequestDto,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    try {
      Message edited =
          chatMessageService.editMessage(
              roomId.toString(),
              messageId.toString(),
              currentUser,
              editMessageRequestDto.getText());
      return Response.status(Status.OK).entity(toMessageDto(edited)).build();
    } catch (ForbiddenException e) {
      return rejection(e);
    }
  }

  @Override
  public Response markRead(
      UUID roomId, MarkReadRequestDto markReadRequestDto, SecurityContext securityContext) {
    if (markReadRequestDto == null
        || markReadRequestDto.getItemId() == null
        || markReadRequestDto.getItemId().isBlank()) {
      return Response.status(Status.BAD_REQUEST).entity("itemId is required").build();
    }
    UserPrincipal currentUser = getCurrentUser(securityContext);
    chatMessageService.setReadMarker(
        roomId.toString(), currentUser.getId(), markReadRequestDto.getItemId());
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response searchMessages(
      UUID roomId,
      String q,
      Integer limit,
      String before,
      String beforeId,
      SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    OffsetDateTime beforeTs = before != null ? parseTimestamp(before) : null;
    MessageSearchPage page =
        chatMessageService.searchVisibleMessages(
            roomId.toString(),
            currentUser.getId(),
            q,
            beforeTs,
            nullIfBlank(beforeId),
            limit != null ? limit : 50);
    List<MessageDto> messages =
        page.messages().stream().map(m -> toMessageDto(m, page.visibleReplyIds())).toList();
    return Response.status(Status.OK)
        .entity(MessageSearchResultDto.create().messages(messages).hasMore(page.hasMore()))
        .build();
  }

  @Override
  public Response getTimeline(
      UUID roomId,
      String before,
      String beforeId,
      String after,
      String afterId,
      String around,
      String aroundId,
      Integer limit,
      SecurityContext securityContext) {
    UserPrincipal principal = getCurrentUser(securityContext);
    TimelineView timeline =
        chatMessageService.getTimeline(
            roomId.toString(),
            principal.getId(),
            parseTimestamp(before),
            nullIfBlank(beforeId),
            parseTimestamp(after),
            nullIfBlank(afterId),
            parseTimestamp(around),
            nullIfBlank(aroundId),
            limit != null ? limit : 50);
    MessageContext context = timeline.context();
    List<TimelineItemDto> items =
        timeline.page().items().stream()
            .map(
                item ->
                    timelineItemMapper.toTimelineItem(
                        item, context.attachmentsById(), context.repliedById()))
            .collect(Collectors.toList());
    return Response.ok(
            TimelineDto.create()
                .items(items)
                .hasMoreBefore(timeline.page().hasMoreBefore())
                .hasMoreAfter(timeline.page().hasMoreAfter())
                .markers(toReadMarkerDtos(timeline.markers())))
        .build();
  }

  @Override
  public Response getRoomReadMarkers(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    return Response.ok(
            toReadMarkerDtos(
                chatMessageService.getRoomReadMarkers(roomId.toString(), currentUser.getId())))
        .build();
  }

  @Override
  public Response getPinnedMessages(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    PinnedMessagesView view =
        chatMessageService.getPinnedMessagesView(roomId.toString(), currentUser.getId());
    MessageContext context = view.context();
    List<PinnedMessageDto> result = new ArrayList<>();
    for (MessagePin pin : view.pins()) {
      PinnedMessageDto dto =
          PinnedMessageDto.create()
              .messageId(pin.getMessageId())
              .roomId(pin.getRoomId())
              .pinnedBy(pin.getPinnedBy())
              .message(
                  timelineItemMapper
                      .toMessageTimelineItem(
                          pin.getMessage(), context.attachmentsById(), context.repliedById())
                      .getMessage());
      if (pin.getPinnedAt() != null) dto.pinnedAt(pin.getPinnedAt());
      result.add(dto);
    }
    return Response.ok(result).build();
  }

  private static List<ReadMarkerDto> toReadMarkerDtos(ReadMarkersView view) {
    return view.markers().stream()
        .map(
            marker ->
                ReadMarkerDto.create()
                    .userId(UUID.fromString(marker.getUserId()))
                    .itemCreatedAt(view.floor().clamp(marker.getItemCreatedAt())))
        .collect(Collectors.toList());
  }

  private static OffsetDateTime parseTimestamp(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value);
    } catch (DateTimeParseException e) {
      throw new BadRequestException("Invalid date-time value", e);
    }
  }

  private static String nullIfBlank(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static Response rejection(ChatsHttpException e) {
    return Response.status(e.getHttpStatusCode()).entity(e.getDebugInfo()).build();
  }

  private static MessageDto toMessageDto(Message msg) {
    return toMessageDto(
        msg, msg.getReplyToId() != null ? Set.of(msg.getReplyToId()) : Set.<String>of());
  }

  private static MessageDto toMessageDto(Message msg, Set<String> visibleReplyIds) {
    MessageDto dto =
        MessageDto.create()
            .id(UUID.fromString(msg.getId()))
            .roomId(UUID.fromString(msg.getRoomId()))
            .senderId(UUID.fromString(msg.getSenderId()))
            .text(msg.getText() != null ? msg.getText() : "")
            .createdAt(msg.getCreatedAt());
    if (msg.getReplyToId() != null && visibleReplyIds.contains(msg.getReplyToId())) {
      dto.replyToId(UUID.fromString(msg.getReplyToId()));
    }
    if (Boolean.TRUE.equals(msg.isEdited()) && msg.getUpdatedAt() != null) {
      dto.editedInfo(EditedInfoDto.create().editedAt(msg.getUpdatedAt()));
    }
    if (Boolean.TRUE.equals(msg.isDeleted())) {
      dto.deletedInfo(
          DeletedInfoDto.create()
              .deletedBy(msg.getSenderId())
              .deletedAt(msg.getDeletedAt() != null ? msg.getDeletedAt() : msg.getUpdatedAt()));
    }
    if (Boolean.TRUE.equals(msg.isForwarded()) && msg.getForwardedSenderId() != null) {
      dto.forwardedInfo(
          ForwardedInfoDto.create()
              .originalSenderId(msg.getForwardedSenderId())
              .originalSentAt(
                  msg.getForwardedAt() != null ? msg.getForwardedAt() : msg.getCreatedAt()));
    } else if (Boolean.TRUE.equals(msg.isForwarded())) {
      dto.forwardedInfo(
          ForwardedInfoDto.create()
              .originalSenderId(msg.getSenderId())
              .originalSentAt(msg.getCreatedAt()));
    }
    if (msg.getAttachmentId() != null) {
      dto.attachmentId(UUID.fromString(msg.getAttachmentId()));
    }
    return dto;
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
  public Response exportRoomMessages(
      UUID roomId, String tz, String deletedPlaceholder, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    ZoneId zone = parseZone(tz);
    String placeholder =
        (deletedPlaceholder != null && !deletedPlaceholder.isBlank())
            ? deletedPlaceholder
            : "Deleted message";
    StreamingOutput stream = chatExportService.export(roomId, currentUser, zone, placeholder);
    return Response.status(Status.OK)
        .entity(stream)
        .header("Content-Type", "text/plain; charset=utf-8")
        .header("Content-Disposition", "attachment; filename=\"chat-export.txt\"")
        .build();
  }

  private static ZoneId parseZone(String tz) {
    if (tz == null || tz.isBlank()) {
      return ZoneOffset.UTC;
    }
    try {
      return ZoneId.of(tz);
    } catch (DateTimeException e) {
      return ZoneOffset.UTC;
    }
  }
}
