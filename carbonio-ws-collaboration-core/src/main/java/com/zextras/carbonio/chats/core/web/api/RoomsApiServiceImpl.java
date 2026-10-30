// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.async.model.MessageDeleted;
import com.zextras.carbonio.async.model.MessageEdited;
import com.zextras.carbonio.async.model.MessageForwarded;
import com.zextras.carbonio.async.model.MessagePinned;
import com.zextras.carbonio.async.model.MessageUnpinned;
import com.zextras.carbonio.async.model.ReactionChanged;
import com.zextras.carbonio.async.model.ReadUpdated;
import com.zextras.carbonio.async.model.WsEvent;
import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.data.entity.ReadMarker;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.AttachmentFilter;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.model.PinResult;
import com.zextras.carbonio.chats.core.data.model.TimelineItem;
import com.zextras.carbonio.chats.core.data.model.TimelinePage;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.data.type.CarbonioAttribute;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.MimeTypeCategory;
import com.zextras.carbonio.chats.core.data.type.UserType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageReceivedBroadcaster;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.mapper.TimelineItemMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.MessageSendDedupRepository;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.TimelineRepository;
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
import com.zextras.carbonio.chats.model.ClearedDateDto;
import com.zextras.carbonio.chats.model.DeletedInfoDto;
import com.zextras.carbonio.chats.model.EditedInfoDto;
import com.zextras.carbonio.chats.model.ForwardMessageRequestDto;
import com.zextras.carbonio.chats.model.ForwardMessagesRequestDto;
import com.zextras.carbonio.chats.model.ForwardedInfoDto;
import com.zextras.carbonio.chats.model.MarkReadRequestDto;
import com.zextras.carbonio.chats.model.MemberDto;
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
import com.zextras.carbonio.chats.model.TimelineItemDto;
import com.zextras.carbonio.chats.model.TimelineMessageDto;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
  private final MembersService membersService;
  private final AttachmentService attachmentService;
  private final MeetingService meetingService;
  private final ChatMessageService chatMessageService;
  private final EventDispatcher eventDispatcher;
  private final ObjectMapper objectMapper;
  private final FileMetadataRepository fileMetadataRepository;
  private final MessageRepository messageRepository;
  private final MessageWriteRepository messageWriteRepository;
  private final StoragesService storagesService;
  private final TimelineRepository timelineRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final MessageReceivedBroadcaster messageReceivedBroadcaster;
  private final Set<MessageSentListener> messageSentListeners;
  private final ChatExportService chatExportService;
  private final TimelineItemMapper timelineItemMapper;
  private final MessageSendDedupRepository messageSendDedupRepository;

  @Inject
  public RoomsApiServiceImpl(
      RoomService roomService,
      MembersService membersService,
      AttachmentService attachmentService,
      MeetingService meetingService,
      ChatMessageService chatMessageService,
      EventDispatcher eventDispatcher,
      ObjectMapper objectMapper,
      FileMetadataRepository fileMetadataRepository,
      MessageRepository messageRepository,
      MessageWriteRepository messageWriteRepository,
      StoragesService storagesService,
      TimelineRepository timelineRepository,
      SubscriptionRepository subscriptionRepository,
      MessageReceivedBroadcaster messageReceivedBroadcaster,
      Set<MessageSentListener> messageSentListeners,
      ChatExportService chatExportService,
      TimelineItemMapper timelineItemMapper,
      MessageSendDedupRepository messageSendDedupRepository) {
    this.roomService = roomService;
    this.membersService = membersService;
    this.attachmentService = attachmentService;
    this.meetingService = meetingService;
    this.chatMessageService = chatMessageService;
    this.eventDispatcher = eventDispatcher;
    this.objectMapper = objectMapper;
    this.fileMetadataRepository = fileMetadataRepository;
    this.messageRepository = messageRepository;
    this.messageWriteRepository = messageWriteRepository;
    this.storagesService = storagesService;
    this.timelineRepository = timelineRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.messageReceivedBroadcaster = messageReceivedBroadcaster;
    this.messageSentListeners = messageSentListeners;
    this.chatExportService = chatExportService;
    this.timelineItemMapper = timelineItemMapper;
    this.messageSendDedupRepository = messageSendDedupRepository;
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
    if (memberToInsertDto.isEmpty()) {
      return Response.status(Status.BAD_REQUEST).build();
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
                long newMembers =
                    memberToInsertDto.stream().map(MemberToInsertDto::getUserId).distinct().count();
                if ((r.getMembers().size() + newMembers) > maxMembers) {
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
    return Response.status(Status.OK)
        .entity(
            attachmentService.getAttachmentInfoByRoomId(
                roomId, limit, cursor, attachmentFilter, currentUser))
        .build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.INFO)
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

    if (normalizeStringValue(replyToId) != null) {
      chatMessageService.validateReplyTarget(
          roomId.toString(), currentUser.getId(), normalizeStringValue(replyToId));
    }

    String dedupTempId = sendTempId(tempId);
    String messageId = UUID.randomUUID().toString();
    if (dedupTempId != null) {
      chatMessageService.validateRoomMembership(roomId.toString(), currentUser.getId());
      MessageSendDedupRepository.Claim claim =
          messageSendDedupRepository.claim(
              roomId.toString(), currentUser.getId(), dedupTempId, messageId);
      if (!claim.claimed()) {
        return duplicateSendResponse(roomId, claim.existingMessageId());
      }
    }

    MessageDto created;
    try {
      created =
          attachmentService.addAttachment(
              roomId,
              filePart.getBody(InputStream.class, null),
              mimeType,
              Long.parseLong(contentLength),
              decodedFileName,
              normalizeStringValue(description),
              normalizeStringValue(replyToId),
              normalizeStringValue(area),
              normalizeStringValue(tempId),
              messageId,
              currentUser);
    } catch (Exception e) {
      if (dedupTempId != null) {
        releaseSendClaim(roomId, currentUser.getId(), dedupTempId, messageId);
      }
      if (e instanceof ChatsHttpException chatsHttpException) {
        throw chatsHttpException;
      }
      ChatsLogger.error("Error processing multipart attachment upload", e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error processing file").build();
    }
    notifyMessageSent(currentUser, roomId, created.getId().toString(), created.getText());
    return Response.status(Status.CREATED).entity(created).build();
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
    return Response.ok().entity(meetingService.getMeetingByRoomId(roomId, currentUser)).build();
  }

  @Override
  public Response pinMessage(UUID roomId, UUID messageId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    // pinMessage now atomically inserts pin record + system event in ChatMessageServiceImpl
    PinResult pinned =
        chatMessageService.pinMessage(roomId.toString(), messageId.toString(), currentUser.getId());
    SystemEvent systemEvent = pinned.systemEvent();
    try {
      TimelineMessageDto message =
          toTimelineMessages(
                  List.of(pinned.message()), roomId.toString(), new VisibilityFloor(null))
              .get(0);
      messageReceivedBroadcaster.broadcastWithReplyVisibility(
          roomId.toString(),
          MessagePinned.create()
              .messageId(messageId.toString())
              .roomId(roomId.toString())
              .pinnedBy(systemEvent.getActorId())
              .systemEventId(systemEvent.getId())
              .systemEventCreatedAt(systemEvent.getCreatedAt())
              .timestamp(systemEvent.getCreatedAt())
              .message(message)
              .sentDate(OffsetDateTime.now()),
          message);
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting pin event: " + e.getMessage());
    }
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response unpinMessage(UUID roomId, UUID messageId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    chatMessageService.validateRoomMembership(roomId.toString(), currentUser.getId());
    // unpinMessage now atomically inserts unpin record + system event in ChatMessageServiceImpl
    chatMessageService
        .unpinMessage(roomId.toString(), messageId.toString(), currentUser.getId())
        .ifPresent(
            systemEvent ->
                broadcastMessageUnpinned(
                    roomId.toString(),
                    messageId.toString(),
                    systemEvent.getActorId(),
                    systemEvent.getId(),
                    systemEvent.getCreatedAt()));
    return Response.status(Status.NO_CONTENT).build();
  }

  private void broadcastMessageUnpinned(
      String roomId,
      String messageId,
      String unpinnedBy,
      String systemEventId,
      OffsetDateTime systemEventCreatedAt) {
    try {
      WsEvent event =
          MessageUnpinned.create()
              .messageId(messageId)
              .roomId(roomId)
              .unpinnedBy(unpinnedBy)
              .systemEventId(systemEventId)
              .systemEventCreatedAt(systemEventCreatedAt)
              .timestamp(OffsetDateTime.now())
              .sentDate(OffsetDateTime.now());
      broadcastToRoomMembers(roomId, objectMapper.writeValueAsString(event));
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting unpin event: " + e.getMessage());
    }
  }

  @Override
  public Response addReaction(
      UUID roomId, UUID messageId, String reaction, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    // A user has a single reaction per message: replacing a different emoji removes the old one.
    Optional<String> replaced =
        chatMessageService.addReaction(
            roomId.toString(), messageId.toString(), currentUser.getId(), reaction);
    try {
      if (replaced.isPresent()) {
        broadcastReactionChanged(
            roomId,
            messageId,
            currentUser.getId(),
            replaced.get(),
            ReactionChanged.OperationEnum.REMOVED);
      }
      broadcastReactionChanged(
          roomId, messageId, currentUser.getId(), reaction, ReactionChanged.OperationEnum.ADDED);
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting reaction event: " + e.getMessage());
    }
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response removeReaction(
      UUID roomId, UUID messageId, String reaction, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    chatMessageService.validateRoomMembership(roomId.toString(), currentUser.getId());
    // A user has a single reaction per message, so the path emoji is not needed to identify it.
    Optional<String> removed =
        chatMessageService.removeReaction(
            roomId.toString(), messageId.toString(), currentUser.getId());
    try {
      if (removed.isPresent()) {
        broadcastReactionChanged(
            roomId,
            messageId,
            currentUser.getId(),
            removed.get(),
            ReactionChanged.OperationEnum.REMOVED);
      }
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting reaction event: " + e.getMessage());
    }
    return Response.status(Status.NO_CONTENT).build();
  }

  private void broadcastReactionChanged(
      UUID roomId,
      UUID messageId,
      String userId,
      String reaction,
      ReactionChanged.OperationEnum operation)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    WsEvent event =
        ReactionChanged.create()
            .messageId(messageId.toString())
            .roomId(roomId.toString())
            .userId(userId)
            .reaction(reaction)
            .operation(operation)
            .sentDate(OffsetDateTime.now());
    broadcastToRoomMembers(roomId.toString(), objectMapper.writeValueAsString(event));
  }

  /**
   * Broadcasts a pre-serialized JSON event to all members of the given room.
   *
   * <p>Sender-side filtering of "self events" (e.g. avoiding double-processing on the actor's own
   * client) is handled by the frontend WS event handler, which has the full session context to
   * decide what to skip and what to keep (e.g. read-updated echoes are still needed by the sender
   * to clear their unread counter).
   */
  private void broadcastToRoomMembers(String roomId, String json) {
    List<String> memberIds = subscriptionRepository.getUserIdsByRoomId(roomId);
    eventDispatcher.sendRawToUserExchange(memberIds, json);
  }

  @Override
  public Response sendMessage(
      UUID roomId, SendMessageRequestDto dto, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    String messageId = UUID.randomUUID().toString();
    String text = dto.getText() != null ? dto.getText() : "";
    if (text.trim().isEmpty()) {
      throw new BadRequestException("Message text must not be blank");
    }
    String replyToId = dto.getReplyToId() != null ? dto.getReplyToId().toString() : null;
    String tempId = dto.getTempId();

    String dedupTempId = sendTempId(tempId);
    MessageWriteRepository.SendResult sent;
    if (dedupTempId == null) {
      Optional<MessageWriteRepository.SendResult> result =
          messageWriteRepository.sendMessage(
              roomId.toString(), currentUser.getId(), text, messageId, replyToId, null);
      if (result.isEmpty()) {
        if (replyToId != null
            && subscriptionRepository.getById(roomId.toString(), currentUser.getId()).isPresent()) {
          return Response.status(Status.NOT_FOUND).entity("Reply target not found").build();
        }
        return Response.status(Status.FORBIDDEN)
            .entity("Not a room member or message ID conflict")
            .build();
      }
      sent = result.get();
    } else {
      MessageWriteRepository.SendOnceResult once =
          messageWriteRepository.sendMessageOnce(
              roomId.toString(), currentUser.getId(), text, messageId, replyToId, dedupTempId);
      if (once.status() == MessageWriteRepository.SendOnceStatus.NOT_MEMBER) {
        return Response.status(Status.FORBIDDEN)
            .entity("Not a room member or message ID conflict")
            .build();
      }
      if (once.status() == MessageWriteRepository.SendOnceStatus.REPLY_NOT_FOUND) {
        return Response.status(Status.NOT_FOUND).entity("Reply target not found").build();
      }
      if (once.status() == MessageWriteRepository.SendOnceStatus.DUPLICATE) {
        return duplicateSendResponse(roomId, once.messageId());
      }
      sent = new MessageWriteRepository.SendResult(once.messageId(), once.createdAt());
    }

    try {
      Message sentMsg =
          Message.create()
              .id(sent.messageId())
              .roomId(roomId.toString())
              .senderId(currentUser.getId())
              .text(text)
              .createdAt(sent.createdAt())
              .replyToId(replyToId);
      messageReceivedBroadcaster.broadcast(
          roomId.toString(), buildResolvedTimelineMessage(sentMsg, Map.of()), tempId);
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting message-received event: " + e.getMessage());
    }

    notifyMessageSent(currentUser, roomId, sent.messageId(), text);

    MessageDto responseDto =
        MessageDto.create()
            .id(UUID.fromString(sent.messageId()))
            .roomId(roomId)
            .senderId(UUID.fromString(currentUser.getId()))
            .text(text)
            .createdAt(sent.createdAt());
    if (dto.getReplyToId() != null) {
      responseDto.replyToId(dto.getReplyToId());
    }
    return Response.status(Status.CREATED).entity(responseDto).build();
  }

  private void notifyMessageSent(UserPrincipal sender, UUID roomId, String messageId, String text) {
    for (MessageSentListener listener : messageSentListeners) {
      try {
        listener.onMessageSent(sender, roomId, messageId, text);
      } catch (Exception e) {
        ChatsLogger.warn("Error in message-sent listener: " + e.getMessage());
      }
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
    String toRoomIdStr = toRoomId.toString();
    String newMessageId = UUID.randomUUID().toString();

    VisibilityFloor sourceFloor =
        chatMessageService.getVisibilityFloor(sourceRoomId.toString(), currentUser.getId());
    chatMessageService.validateRoomMembership(toRoomIdStr, currentUser.getId());

    // Copy blob if the original message has an attachment (must happen before CTE)
    String newAttachmentId = null;
    FileMetadata newMeta = null;
    FileMetadata sourceMeta = null;
    Message originalMessage = messageRepository.getById(messageId.toString()).orElse(null);
    boolean originalVisible =
        originalMessage == null
            || (sourceRoomId.toString().equals(originalMessage.getRoomId())
                && sourceFloor.canAccess(originalMessage.getCreatedAt()));
    if (!originalVisible) {
      throw new NotFoundException(String.format("Message '%s' not found", messageId));
    }
    if (originalMessage != null && originalMessage.getAttachmentId() != null) {
      String origAttachmentId = originalMessage.getAttachmentId();
      Optional<FileMetadata> origMeta = fileMetadataRepository.getById(origAttachmentId);
      if (origMeta.isPresent()) {
        sourceMeta = origMeta.get();
        newAttachmentId = UUID.randomUUID().toString();
        storagesService.copyFile(
            origAttachmentId, originalMessage.getSenderId(), newAttachmentId, currentUser.getId());
        newMeta =
            FileMetadata.create()
                .id(newAttachmentId)
                .name(origMeta.get().getName())
                .originalSize(origMeta.get().getOriginalSize())
                .mimeType(origMeta.get().getMimeType())
                .type(FileMetadataType.ATTACHMENT)
                .userId(currentUser.getId())
                .roomId(toRoomIdStr)
                .messageId(newMessageId)
                .area(origMeta.get().getArea());
        fileMetadataRepository.save(newMeta);
      }
    }

    Optional<MessageWriteRepository.ForwardResult> result =
        messageWriteRepository.forwardMessage(
            sourceRoomId.toString(),
            toRoomIdStr,
            currentUser.getId(),
            messageId.toString(),
            newMessageId,
            newAttachmentId);

    if (result.isEmpty()) {
      // Best-effort cleanup of the copied blob and its metadata
      if (newAttachmentId != null) {
        try {
          storagesService.deleteFile(newAttachmentId, currentUser.getId());
        } catch (Exception ignored) {
          // best-effort
        }
        fileMetadataRepository.delete(newMeta);
      }
      boolean isMemberOfBoth =
          subscriptionRepository.getById(sourceRoomId.toString(), currentUser.getId()).isPresent()
              && subscriptionRepository.getById(toRoomIdStr, currentUser.getId()).isPresent();
      if (isMemberOfBoth) {
        throw new NotFoundException(String.format("Message '%s' not found", messageId));
      }
      throw new ForbiddenException("Not a member of source/target room or message not found");
    }

    MessageWriteRepository.ForwardResult forwarded = result.get();
    if (sourceMeta != null) {
      attachmentService.copyVideoPreview(sourceMeta, newAttachmentId, currentUser.getId());
    }

    try {
      Message forwardedMsg =
          Message.create()
              .id(forwarded.messageId())
              .roomId(toRoomIdStr)
              .senderId(currentUser.getId())
              .text(forwarded.originalText() != null ? forwarded.originalText() : "")
              .createdAt(forwarded.createdAt())
              .forwarded(true)
              .forwardedSenderId(forwarded.forwardedSenderId())
              .forwardedAt(forwarded.forwardedAt())
              .attachmentId(forwarded.attachmentId());
      Map<String, FileMetadata> knownAttachments = new HashMap<>();
      if (forwarded.attachmentId() != null) {
        knownAttachments.put(
            forwarded.attachmentId(),
            FileMetadata.create()
                .id(forwarded.attachmentId())
                .name(forwarded.attachmentName())
                .mimeType(forwarded.attachmentMime())
                .originalSize(forwarded.attachmentSize())
                .area(forwarded.attachmentArea()));
      }
      WsEvent event =
          MessageForwarded.create()
              .message(buildResolvedTimelineMessage(forwardedMsg, knownAttachments))
              .sentDate(OffsetDateTime.now());
      broadcastToRoomMembers(toRoomIdStr, objectMapper.writeValueAsString(event));
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting message-forwarded event: " + e.getMessage());
    }

    MessageDto responseDto =
        MessageDto.create()
            .id(UUID.fromString(forwarded.messageId()))
            .roomId(toRoomId)
            .senderId(UUID.fromString(currentUser.getId()))
            .text(forwarded.originalText() != null ? forwarded.originalText() : "")
            .createdAt(forwarded.createdAt());
    if (forwarded.forwardedSenderId() != null) {
      responseDto.forwardedInfo(
          ForwardedInfoDto.create()
              .originalSenderId(forwarded.forwardedSenderId())
              .originalSentAt(
                  forwarded.forwardedAt() != null
                      ? forwarded.forwardedAt()
                      : forwarded.createdAt()));
    }
    if (forwarded.attachmentId() != null) {
      responseDto.attachmentId(UUID.fromString(forwarded.attachmentId()));
    }
    return responseDto;
  }

  @Override
  public Response deleteMessage(UUID roomId, UUID messageId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);

    Optional<MessageWriteRepository.DeleteResult> result =
        messageWriteRepository.deleteMessage(
            messageId.toString(), roomId.toString(), currentUser.getId());

    if (result.isEmpty()) {
      return Response.status(Status.FORBIDDEN)
          .entity("Not a room member, not the sender, or message already deleted")
          .build();
    }

    MessageWriteRepository.DeleteResult deleted = result.get();

    try {
      WsEvent event =
          MessageDeleted.create()
              .messageId(deleted.messageId())
              .roomId(roomId.toString())
              .deletedBy(currentUser.getId())
              .deletedAt(OffsetDateTime.now())
              .sentDate(OffsetDateTime.now());
      broadcastToRoomMembers(roomId.toString(), objectMapper.writeValueAsString(event));
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting message-deleted event: " + e.getMessage());
    }
    if (deleted.unpinned() != null) {
      broadcastMessageUnpinned(
          roomId.toString(),
          deleted.messageId(),
          currentUser.getId(),
          deleted.unpinned().systemEventId(),
          deleted.unpinned().systemEventCreatedAt());
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

    Optional<MessageWriteRepository.EditResult> result =
        messageWriteRepository.editMessage(
            messageId.toString(),
            roomId.toString(),
            currentUser.getId(),
            editMessageRequestDto.getText());

    if (result.isEmpty()) {
      return Response.status(Status.FORBIDDEN)
          .entity("Not a room member, not the sender, or message is deleted")
          .build();
    }

    MessageWriteRepository.EditResult edited = result.get();

    try {
      WsEvent event =
          MessageEdited.create()
              .messageId(edited.messageId())
              .roomId(roomId.toString())
              .text(edited.text())
              .editedAt(edited.updatedAt())
              .sentDate(OffsetDateTime.now());
      messageReceivedBroadcaster.broadcastToMembersWhoCanAccess(
          roomId.toString(), edited.createdAt(), objectMapper.writeValueAsString(event));
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting message-edited event: " + e.getMessage());
    }
    return Response.status(Status.OK)
        .entity(
            MessageDto.create()
                .id(UUID.fromString(edited.messageId()))
                .roomId(roomId)
                .senderId(UUID.fromString(currentUser.getId()))
                .text(edited.text())
                .createdAt(edited.createdAt())
                .editedInfo(EditedInfoDto.create().editedAt(edited.updatedAt())))
        .build();
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
    chatMessageService
        .setReadMarker(roomId.toString(), currentUser.getId(), markReadRequestDto.getItemId())
        .ifPresent(
            itemCreatedAt -> {
              try {
                WsEvent event =
                    ReadUpdated.create()
                        .roomId(roomId.toString())
                        .userId(currentUser.getId())
                        .itemCreatedAt(itemCreatedAt)
                        .sentDate(OffsetDateTime.now());
                broadcastToRoomMembers(roomId.toString(), objectMapper.writeValueAsString(event));
              } catch (Exception e) {
                ChatsLogger.warn(
                    "Error broadcasting read-updated event after markRead: " + e.getMessage());
              }
            });
    return Response.status(Status.NO_CONTENT).build();
  }

  @Override
  public Response searchMessages(
      UUID roomId, String q, Integer limit, String before, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    VisibilityFloor floor =
        chatMessageService.getVisibilityFloor(roomId.toString(), currentUser.getId());
    OffsetDateTime floorAfter = floor.clearedAt();
    int pageLimit = limit != null ? limit : 50;
    OffsetDateTime beforeTs = before != null ? OffsetDateTime.parse(before) : null;
    List<Message> page =
        chatMessageService.searchMessages(
            roomId.toString(), q, beforeTs, floorAfter, pageLimit + 1);
    boolean hasMore = page.size() > pageLimit;
    List<Message> trimmed = hasMore ? page.subList(0, pageLimit) : page;
    Set<String> visibleReplyIds = visibleReplyTargetIds(trimmed, roomId.toString(), floor);
    List<MessageDto> messages =
        trimmed.stream().map(m -> toMessageDto(m, visibleReplyIds)).toList();
    return Response.status(Status.OK)
        .entity(MessageSearchResultDto.create().messages(messages).hasMore(hasMore))
        .build();
  }

  @Override
  public Response getTimeline(
      UUID roomId,
      String before,
      String beforeId,
      String after,
      String around,
      Integer limit,
      SecurityContext securityContext) {
    UserPrincipal principal = getCurrentUser(securityContext);
    VisibilityFloor floor =
        chatMessageService.getVisibilityFloor(roomId.toString(), principal.getId());
    int effectiveLimit = limit != null ? limit : 50;

    // Parse explicit after param
    OffsetDateTime afterTs =
        (after != null && !after.isBlank()) ? OffsetDateTime.parse(after) : null;
    OffsetDateTime beforeTs =
        (before != null && !before.isBlank()) ? OffsetDateTime.parse(before) : null;
    // beforeId anchors the composite cursor (created_at, id) < (beforeTs, beforeId)
    String effectiveBeforeId = (beforeId != null && !beforeId.isBlank()) ? beforeId : null;

    OffsetDateTime effectiveAfter = floor.clamp(afterTs);

    TimelinePage page;
    if (around != null && !around.isBlank()) {
      OffsetDateTime aroundTs = OffsetDateTime.parse(around);
      page =
          timelineRepository.findAround(
              roomId.toString(), aroundTs, effectiveAfter, effectiveLimit);
    } else if (afterTs != null) {
      page = timelineRepository.findAfter(roomId.toString(), effectiveAfter, effectiveLimit);
    } else if (beforeTs != null) {
      page =
          timelineRepository.findBefore(
              roomId.toString(), beforeTs, effectiveBeforeId, effectiveAfter, effectiveLimit);
    } else {
      page = timelineRepository.findLatest(roomId.toString(), effectiveAfter, effectiveLimit);
    }

    Map<String, Message> repliedById =
        resolveRepliedMessages(page.items(), roomId.toString(), floor);
    Map<String, FileMetadata> attachmentsById =
        resolveAttachments(page.items(), repliedById.values());
    List<TimelineItemDto> items =
        page.items().stream()
            .map(item -> timelineItemMapper.toTimelineItem(item, attachmentsById, repliedById))
            .collect(Collectors.toList());
    boolean hasMoreBefore = page.hasMoreBefore();
    boolean hasMoreAfter = page.hasMoreAfter();

    // Read markers for this room
    List<ReadMarker> readMarkers = chatMessageService.getRoomReadMarkers(roomId.toString());
    List<ReadMarkerDto> markers =
        readMarkers.stream()
            .map(
                m ->
                    ReadMarkerDto.create()
                        .userId(UUID.fromString(m.getUserId()))
                        .itemCreatedAt(m.getItemCreatedAt()))
            .collect(Collectors.toList());

    TimelineDto timeline =
        TimelineDto.create()
            .items(items)
            .hasMoreBefore(hasMoreBefore)
            .hasMoreAfter(hasMoreAfter)
            .markers(markers);

    return Response.ok(timeline).build();
  }

  @Override
  public Response getRoomReadMarkers(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    chatMessageService.validateRoomMembership(roomId.toString(), currentUser.getId());
    List<ReadMarker> markers = chatMessageService.getRoomReadMarkers(roomId.toString());
    List<ReadMarkerDto> result =
        markers.stream()
            .map(
                marker ->
                    ReadMarkerDto.create()
                        .userId(UUID.fromString(marker.getUserId()))
                        .itemCreatedAt(marker.getItemCreatedAt()))
            .collect(Collectors.toList());
    return Response.ok(result).build();
  }

  @Override
  public Response getPinnedMessages(UUID roomId, SecurityContext securityContext) {
    UserPrincipal currentUser = getCurrentUser(securityContext);
    VisibilityFloor floor =
        chatMessageService.getVisibilityFloor(roomId.toString(), currentUser.getId());
    List<MessagePin> pins = chatMessageService.getPinnedMessages(roomId.toString());
    List<TimelineMessageDto> messages =
        toTimelineMessages(
            pins.stream().map(MessagePin::getMessage).toList(), roomId.toString(), floor);
    List<PinnedMessageDto> result = new ArrayList<>();
    for (int i = 0; i < pins.size(); i++) {
      MessagePin pin = pins.get(i);
      PinnedMessageDto dto =
          PinnedMessageDto.create()
              .messageId(pin.getMessageId())
              .roomId(pin.getRoomId())
              .pinnedBy(pin.getPinnedBy())
              .message(messages.get(i));
      if (pin.getPinnedAt() != null) dto.pinnedAt(pin.getPinnedAt());
      result.add(dto);
    }
    return Response.ok(result).build();
  }

  private List<TimelineMessageDto> toTimelineMessages(
      List<Message> messages, String roomId, VisibilityFloor floor) {
    List<TimelineItem> items =
        messages.stream().map(m -> (TimelineItem) new TimelineItem.MessageItem(m)).toList();
    Map<String, Message> repliedById = resolveRepliedMessages(items, roomId, floor);
    Map<String, FileMetadata> attachmentsById = resolveAttachments(items, repliedById.values());
    return messages.stream()
        .map(
            m ->
                timelineItemMapper
                    .toMessageTimelineItem(m, attachmentsById, repliedById)
                    .getMessage())
        .toList();
  }

  // Batch-fetch the page's distinct reply targets in one query, dropping any original hidden by the
  // caller's cleared history. Absent originals won't be exploded.
  private Map<String, Message> resolveRepliedMessages(
      List<TimelineItem> items, String roomId, VisibilityFloor floor) {
    List<String> replyIds =
        items.stream()
            .map(i -> i instanceof TimelineItem.MessageItem m ? m.message().getReplyToId() : null)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());
    if (replyIds.isEmpty()) {
      return Map.of();
    }
    return messageRepository.getByRoomIdAndIds(roomId, replyIds).stream()
        .filter(o -> o.getCreatedAt() != null && floor.canAccess(o.getCreatedAt()))
        .collect(Collectors.toMap(Message::getId, m -> m, (a, b) -> a));
  }

  private Map<String, FileMetadata> resolveAttachments(
      List<TimelineItem> items, Collection<Message> repliedMessages) {
    List<String> attachmentIds = new ArrayList<>();
    items.stream()
        .map(i -> i instanceof TimelineItem.MessageItem m ? m.message().getAttachmentId() : null)
        .filter(id -> id != null)
        .forEach(attachmentIds::add);
    repliedMessages.stream()
        .map(Message::getAttachmentId)
        .filter(id -> id != null)
        .forEach(attachmentIds::add);
    List<String> distinct = attachmentIds.stream().distinct().collect(Collectors.toList());
    if (distinct.isEmpty()) {
      return Map.of();
    }
    return fileMetadataRepository.getByIds(distinct).stream()
        .collect(Collectors.toMap(FileMetadata::getId, f -> f, (a, b) -> a));
  }

  private TimelineMessageDto buildResolvedTimelineMessage(
      Message msg, Map<String, FileMetadata> knownAttachments) {
    Map<String, FileMetadata> attachmentsById = new HashMap<>(knownAttachments);
    if (msg.getAttachmentId() != null && !attachmentsById.containsKey(msg.getAttachmentId())) {
      fileMetadataRepository
          .getById(msg.getAttachmentId())
          .ifPresent(m -> attachmentsById.put(m.getId(), m));
    }
    TimelineMessageDto dto = timelineItemMapper.baseMessageFields(msg, attachmentsById);
    if (msg.getReplyToId() != null) {
      messageRepository
          .getById(msg.getReplyToId())
          .ifPresent(
              replied -> {
                if (replied.getAttachmentId() != null
                    && !attachmentsById.containsKey(replied.getAttachmentId())) {
                  fileMetadataRepository
                      .getById(replied.getAttachmentId())
                      .ifPresent(m -> attachmentsById.put(m.getId(), m));
                }
                dto.repliedMessage(timelineItemMapper.baseMessageFields(replied, attachmentsById));
              });
    }
    return dto;
  }

  private Set<String> visibleReplyTargetIds(
      List<Message> messages, String roomId, VisibilityFloor floor) {
    List<TimelineItem> items =
        messages.stream().map(m -> (TimelineItem) new TimelineItem.MessageItem(m)).toList();
    return resolveRepliedMessages(items, roomId, floor).keySet();
  }

  private static String sendTempId(String tempId) {
    return tempId == null || tempId.isBlank() ? null : tempId;
  }

  private Response duplicateSendResponse(UUID roomId, String messageId) {
    Message original =
        messageId == null
            ? null
            : messageRepository
                .getById(messageId)
                .filter(m -> roomId.toString().equals(m.getRoomId()))
                .orElse(null);
    if (original == null) {
      throw new ConflictException("The first request with this tempId has not completed yet");
    }
    Set<String> replyIds =
        original.getReplyToId() != null ? Set.of(original.getReplyToId()) : Set.of();
    return Response.status(Status.CREATED).entity(toMessageDto(original, replyIds)).build();
  }

  private void releaseSendClaim(UUID roomId, String userId, String tempId, String messageId) {
    try {
      messageSendDedupRepository.release(roomId.toString(), userId, tempId, messageId);
    } catch (Exception e) {
      ChatsLogger.warn(
          "Could not release the send claim of tempId '" + tempId + "': " + e.getMessage());
    }
  }

  private MessageDto toMessageDto(Message msg, Set<String> visibleReplyIds) {
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
