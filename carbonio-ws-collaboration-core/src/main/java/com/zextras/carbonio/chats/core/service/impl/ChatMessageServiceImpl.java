// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

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
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.data.entity.MessageReaction;
import com.zextras.carbonio.chats.core.data.entity.ReadMarker;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.InboxLastItem;
import com.zextras.carbonio.chats.core.data.model.InboxLastItemRefs;
import com.zextras.carbonio.chats.core.data.model.MessageContext;
import com.zextras.carbonio.chats.core.data.model.MessageSearchPage;
import com.zextras.carbonio.chats.core.data.model.PinResult;
import com.zextras.carbonio.chats.core.data.model.PinnedMessagesView;
import com.zextras.carbonio.chats.core.data.model.ReadMarkersView;
import com.zextras.carbonio.chats.core.data.model.TimelineItem;
import com.zextras.carbonio.chats.core.data.model.TimelinePage;
import com.zextras.carbonio.chats.core.data.model.TimelineView;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.event.mapper.TimelineItemMapper;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageReceivedBroadcaster;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MessagePinRepository;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository.Removed;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository.SetKind;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository.SetResult;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.MessageSendDedupRepository;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository;
import com.zextras.carbonio.chats.core.repository.ReadMarkerRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.repository.TimelineRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.RoomAccess;
import com.zextras.carbonio.chats.core.spi.MessageSentListener;
import com.zextras.carbonio.chats.core.utils.SearchTextUtils;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.TimelineMessageDto;
import io.ebean.annotation.Transactional;
import jakarta.annotation.Nullable;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class ChatMessageServiceImpl implements ChatMessageService {

  private static final String NOT_A_MEMBER_TO_SEND = "Not a room member or message ID conflict";
  private static final String REPLY_TARGET_NOT_FOUND = "Reply target not found";

  private final MessageRepository messageRepository;
  private final MessageWriteRepository messageWriteRepository;
  private final MessageReactionRepository reactionRepository;
  private final ReadMarkerRepository readRepository;
  private final MessagePinRepository pinRepository;
  private final SystemEventRepository systemEventRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final TimelineRepository timelineRepository;
  private final MessageSendDedupRepository messageSendDedupRepository;
  private final RoomAccess roomAccess;
  private final AttachmentService attachmentService;
  private final StoragesService storagesService;
  private final EventDispatcher eventDispatcher;
  private final MessageReceivedBroadcaster messageReceivedBroadcaster;
  private final TimelineItemMapper timelineItemMapper;
  private final ObjectMapper objectMapper;
  private final Set<MessageSentListener> messageSentListeners;

  @Inject
  public ChatMessageServiceImpl(
      MessageRepository messageRepository,
      MessageWriteRepository messageWriteRepository,
      MessageReactionRepository reactionRepository,
      ReadMarkerRepository readRepository,
      MessagePinRepository pinRepository,
      SystemEventRepository systemEventRepository,
      FileMetadataRepository fileMetadataRepository,
      SubscriptionRepository subscriptionRepository,
      TimelineRepository timelineRepository,
      MessageSendDedupRepository messageSendDedupRepository,
      RoomAccess roomAccess,
      AttachmentService attachmentService,
      StoragesService storagesService,
      EventDispatcher eventDispatcher,
      MessageReceivedBroadcaster messageReceivedBroadcaster,
      TimelineItemMapper timelineItemMapper,
      ObjectMapper objectMapper,
      Set<MessageSentListener> messageSentListeners) {
    this.messageRepository = messageRepository;
    this.messageWriteRepository = messageWriteRepository;
    this.reactionRepository = reactionRepository;
    this.readRepository = readRepository;
    this.pinRepository = pinRepository;
    this.systemEventRepository = systemEventRepository;
    this.fileMetadataRepository = fileMetadataRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.timelineRepository = timelineRepository;
    this.messageSendDedupRepository = messageSendDedupRepository;
    this.roomAccess = roomAccess;
    this.attachmentService = attachmentService;
    this.storagesService = storagesService;
    this.eventDispatcher = eventDispatcher;
    this.messageReceivedBroadcaster = messageReceivedBroadcaster;
    this.timelineItemMapper = timelineItemMapper;
    this.objectMapper = objectMapper;
    this.messageSentListeners = messageSentListeners;
  }

  // ==================== MESSAGE OPERATIONS ====================

  @Override
  public SystemEvent insertSystemEvent(SystemEvent event) {
    return systemEventRepository.insert(event);
  }

  @Override
  public SystemEvent updateSystemEvent(SystemEvent event) {
    return systemEventRepository.update(event);
  }

  @Override
  public List<SystemEvent> getSystemEvents(String roomId) {
    return systemEventRepository.findByRoomId(roomId);
  }

  @Override
  public Optional<Message> getMessage(String messageId) {
    return messageRepository.getById(messageId);
  }

  @Override
  public List<Message> getMessagesByIds(String roomId, List<String> messageIds) {
    return messageRepository.getByRoomIdAndIds(roomId, messageIds);
  }

  @Override
  public List<Message> searchMessages(
      String roomId,
      String searchText,
      OffsetDateTime before,
      String beforeId,
      OffsetDateTime after,
      int limit) {
    List<String> words = SearchTextUtils.toSearchWords(searchText);
    if (words.isEmpty()) {
      return List.of();
    }
    if (before != null && beforeId != null) {
      OffsetDateTime anchor =
          messageRepository
              .getById(beforeId)
              .filter(message -> roomId.equals(message.getRoomId()))
              .map(Message::getCreatedAt)
              .filter(createdAt -> after == null || createdAt.isAfter(after))
              .orElse(before);
      return messageRepository.searchByText(roomId, words, anchor, beforeId, after, limit);
    }
    return messageRepository.searchByText(roomId, words, before, after, limit);
  }

  @Override
  public Message sendMessage(
      String roomId,
      UserPrincipal sender,
      @Nullable String text,
      @Nullable String replyToId,
      @Nullable String tempId) {
    String body = text != null ? text : "";
    if (body.trim().isEmpty()) {
      throw new BadRequestException("Message text must not be blank");
    }
    String messageId = UUID.randomUUID().toString();
    String dedupTempId = sendTempId(tempId);
    MessageWriteRepository.SendResult sent;
    if (dedupTempId == null) {
      sent =
          messageWriteRepository.sendMessage(
              roomId, sender.getId(), body, messageId, replyToId, null);
      if (sent.status() != MessageWriteRepository.SendStatus.SENT) {
        throw sent.status() == MessageWriteRepository.SendStatus.REPLY_NOT_FOUND
            ? new NotFoundException(REPLY_TARGET_NOT_FOUND)
            : new ForbiddenException(NOT_A_MEMBER_TO_SEND);
      }
    } else {
      MessageWriteRepository.SendOnceResult once =
          messageWriteRepository.sendMessageOnce(
              roomId, sender.getId(), body, messageId, replyToId, dedupTempId);
      switch (once.status()) {
        case NOT_MEMBER -> throw new ForbiddenException(NOT_A_MEMBER_TO_SEND);
        case REPLY_NOT_FOUND -> throw new NotFoundException(REPLY_TARGET_NOT_FOUND);
        case DUPLICATE -> {
          return firstRequestMessage(roomId, once.messageId());
        }
        case SENT -> {}
      }
      sent =
          new MessageWriteRepository.SendResult(
              MessageWriteRepository.SendStatus.SENT, once.messageId(), once.createdAt());
    }

    Message sentMessage =
        Message.create()
            .id(sent.messageId())
            .roomId(roomId)
            .senderId(sender.getId())
            .text(body)
            .createdAt(sent.createdAt())
            .replyToId(replyToId);
    try {
      messageReceivedBroadcaster.broadcast(
          roomId, buildResolvedTimelineMessage(sentMessage, Map.of()), tempId);
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting message-received event: " + e.getMessage());
    }
    notifyMessageSent(sender, roomId, sent.messageId(), body, sent.createdAt());
    return sentMessage;
  }

  @Override
  public Message uploadAttachment(
      String roomId,
      InputStream file,
      String mimeType,
      long contentLength,
      String fileName,
      @Nullable String description,
      @Nullable String replyToId,
      @Nullable String area,
      @Nullable String tempId,
      UserPrincipal sender) {
    String dedupTempId = sendTempId(tempId);
    if (dedupTempId == null && replyToId == null) {
      roomAccess.requireMember(UUID.fromString(roomId), sender);
    } else {
      VisibilityFloor floor = getVisibilityFloor(roomId, sender.getId());
      if (replyToId != null) {
        validateReplyTarget(roomId, floor, replyToId);
      }
    }
    String messageId = UUID.randomUUID().toString();
    if (dedupTempId != null) {
      MessageSendDedupRepository.Claim claim =
          messageSendDedupRepository.claim(roomId, sender.getId(), dedupTempId, messageId);
      if (!claim.claimed()) {
        return firstRequestMessage(roomId, claim.existingMessageId());
      }
    }
    Message created = null;
    try {
      created =
          attachmentService.addAttachment(
              UUID.fromString(roomId),
              file,
              mimeType,
              contentLength,
              fileName,
              description,
              replyToId,
              area,
              tempId,
              messageId,
              sender);
    } finally {
      if (created == null && dedupTempId != null) {
        releaseSendClaim(roomId, sender.getId(), dedupTempId, messageId);
      }
    }
    notifyMessageSent(sender, roomId, created.getId(), created.getText(), created.getCreatedAt());
    return created;
  }

  @Override
  public Message editMessage(
      String roomId, String messageId, UserPrincipal editor, String newText) {
    MessageWriteRepository.EditResult edited =
        messageWriteRepository.editMessage(messageId, roomId, editor.getId(), newText);
    switch (edited.status()) {
      case NOT_ALLOWED ->
          throw new ForbiddenException("Not a room member, not the sender, or message is deleted");
      case NOT_VISIBLE ->
          throw new NotFoundException(String.format("Message '%s' not found", messageId));
      case EDITED -> {}
    }
    try {
      WsEvent event =
          MessageEdited.create()
              .messageId(edited.messageId())
              .roomId(roomId)
              .text(edited.text())
              .editedAt(edited.updatedAt());
      if (edited.pinned()) {
        broadcastToRoomMembers(roomId, event);
      } else {
        messageReceivedBroadcaster.broadcastToMembersWhoCanAccess(
            roomId, edited.createdAt(), event);
      }
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting message-edited event: " + e.getMessage());
    }
    notifyMessageSent(editor, roomId, edited.messageId(), edited.text(), edited.createdAt());
    return Message.create()
        .id(edited.messageId())
        .roomId(roomId)
        .senderId(editor.getId())
        .text(edited.text())
        .createdAt(edited.createdAt())
        .edited(true)
        .updatedAt(edited.updatedAt());
  }

  @Override
  public void deleteMessage(String roomId, String messageId, UserPrincipal deleter) {
    MessageWriteRepository.DeleteResult deleted =
        messageWriteRepository.deleteMessage(messageId, roomId, deleter.getId());
    switch (deleted.status()) {
      case NOT_ALLOWED ->
          throw new ForbiddenException(
              "Not a room member, not the sender, or message already deleted");
      case NOT_VISIBLE ->
          throw new NotFoundException(String.format("Message '%s' not found", messageId));
      case DELETED -> {}
    }
    try {
      WsEvent event =
          MessageDeleted.create()
              .messageId(deleted.messageId())
              .roomId(roomId)
              .deletedBy(deleter.getId())
              .deletedAt(deleted.deletedAt());
      messageReceivedBroadcaster.broadcastToMembersWhoCanAccess(roomId, deleted.createdAt(), event);
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting message-deleted event: " + e.getMessage());
    }
    if (deleted.unpinned() != null) {
      broadcastMessageUnpinned(
          roomId,
          deleted.messageId(),
          deleter.getId(),
          deleted.unpinned().systemEventId(),
          deleted.unpinned().systemEventCreatedAt());
    }
  }

  @Override
  public Message forwardMessage(
      String sourceRoomId, String messageId, String targetRoomId, UserPrincipal user) {
    String newMessageId = UUID.randomUUID().toString();

    VisibilityFloor sourceFloor = getVisibilityFloor(sourceRoomId, user.getId());
    getVisibilityFloor(targetRoomId, user.getId());

    String newAttachmentId = null;
    FileMetadata newMeta = null;
    FileMetadata sourceMeta = null;
    Message originalMessage = messageRepository.getById(messageId).orElse(null);
    boolean originalVisible =
        originalMessage == null
            || (sourceRoomId.equals(originalMessage.getRoomId())
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
            origAttachmentId, originalMessage.getSenderId(), newAttachmentId, user.getId());
        newMeta =
            FileMetadata.create()
                .id(newAttachmentId)
                .name(origMeta.get().getName())
                .originalSize(origMeta.get().getOriginalSize())
                .mimeType(origMeta.get().getMimeType())
                .type(FileMetadataType.ATTACHMENT)
                .userId(user.getId())
                .roomId(targetRoomId)
                .messageId(newMessageId)
                .area(origMeta.get().getArea());
        fileMetadataRepository.save(newMeta);
      }
    }

    MessageWriteRepository.ForwardResult forwarded;
    try {
      forwarded =
          messageWriteRepository.forwardMessage(
              sourceRoomId, targetRoomId, user.getId(), messageId, newMessageId, newAttachmentId);
    } catch (RuntimeException e) {
      try {
        discardCopiedAttachment(newAttachmentId, newMeta, user.getId());
      } catch (RuntimeException cleanup) {
        e.addSuppressed(cleanup);
      }
      throw e;
    }

    if (forwarded.status() != MessageWriteRepository.ForwardStatus.FORWARDED) {
      discardCopiedAttachment(newAttachmentId, newMeta, user.getId());
      if (forwarded.status() == MessageWriteRepository.ForwardStatus.MESSAGE_NOT_FOUND) {
        throw new NotFoundException(String.format("Message '%s' not found", messageId));
      }
      throw new ForbiddenException("Not a member of source/target room or message not found");
    }

    if (sourceMeta != null) {
      attachmentService.copyVideoPreview(sourceMeta, newAttachmentId, user.getId());
    }

    Message forwardedMessage =
        Message.create()
            .id(forwarded.messageId())
            .roomId(targetRoomId)
            .senderId(user.getId())
            .text(forwarded.originalText() != null ? forwarded.originalText() : "")
            .createdAt(forwarded.createdAt())
            .forwarded(true)
            .forwardedSenderId(forwarded.forwardedSenderId())
            .forwardedAt(forwarded.forwardedAt())
            .attachmentId(forwarded.attachmentId());
    try {
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
              .message(buildResolvedTimelineMessage(forwardedMessage, knownAttachments));
      broadcastToRoomMembers(targetRoomId, event);
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting message-forwarded event: " + e.getMessage());
    }
    return forwardedMessage;
  }

  private void discardCopiedAttachment(
      @Nullable String attachmentId, @Nullable FileMetadata metadata, String ownerId) {
    if (attachmentId == null) {
      return;
    }
    try {
      storagesService.deleteFile(attachmentId, ownerId);
    } catch (Exception ignored) {
      // best-effort
    }
    fileMetadataRepository.delete(metadata);
  }

  @Override
  public TimelineView getTimeline(
      String roomId,
      String userId,
      @Nullable OffsetDateTime before,
      @Nullable String beforeId,
      @Nullable OffsetDateTime after,
      @Nullable String afterId,
      @Nullable OffsetDateTime around,
      @Nullable String aroundId,
      int limit) {
    VisibilityFloor floor = getVisibilityFloor(roomId, userId);
    OffsetDateTime effectiveAfter = floor.clamp(after);

    TimelinePage page;
    if (around != null) {
      page =
          aroundId != null
              ? timelineRepository.findAround(roomId, around, aroundId, effectiveAfter, limit)
              : timelineRepository.findAround(roomId, around, effectiveAfter, limit);
    } else if (after != null) {
      page =
          afterId != null
              ? timelineRepository.findAfter(roomId, after, afterId, floor.clearedAt(), limit)
              : timelineRepository.findAfter(roomId, effectiveAfter, limit);
    } else if (before != null) {
      page = timelineRepository.findBefore(roomId, before, beforeId, effectiveAfter, limit);
    } else {
      page = timelineRepository.findLatest(roomId, effectiveAfter, limit);
    }

    MessageContext context = resolveContext(page.items(), roomId, floor);
    return new TimelineView(
        page, context, new ReadMarkersView(readRepository.getByRoomId(roomId), floor));
  }

  @Override
  public MessageSearchPage searchVisibleMessages(
      String roomId,
      String userId,
      String searchText,
      @Nullable OffsetDateTime before,
      @Nullable String beforeId,
      int limit) {
    VisibilityFloor floor = getVisibilityFloor(roomId, userId);
    List<Message> page =
        searchMessages(roomId, searchText, before, beforeId, floor.clearedAt(), limit + 1);
    boolean hasMore = page.size() > limit;
    List<Message> trimmed = hasMore ? page.subList(0, limit) : page;
    Set<String> visibleReplyIds =
        resolveRepliedMessages(messageItems(trimmed), roomId, floor).keySet();
    return new MessageSearchPage(trimmed, hasMore, visibleReplyIds);
  }

  private static List<TimelineItem> messageItems(List<Message> messages) {
    return messages.stream().map(m -> (TimelineItem) new TimelineItem.MessageItem(m)).toList();
  }

  private MessageContext resolveContext(
      List<TimelineItem> items, String roomId, VisibilityFloor floor) {
    Map<String, Message> repliedById = resolveRepliedMessages(items, roomId, floor);
    return new MessageContext(repliedById, resolveAttachments(items, repliedById.values()));
  }

  private Map<String, Message> resolveRepliedMessages(
      List<TimelineItem> items, String roomId, VisibilityFloor floor) {
    List<String> replyIds =
        items.stream()
            .map(i -> i instanceof TimelineItem.MessageItem m ? m.message().getReplyToId() : null)
            .filter(Objects::nonNull)
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
        .filter(Objects::nonNull)
        .forEach(attachmentIds::add);
    repliedMessages.stream()
        .map(Message::getAttachmentId)
        .filter(Objects::nonNull)
        .forEach(attachmentIds::add);
    List<String> distinct = attachmentIds.stream().distinct().collect(Collectors.toList());
    if (distinct.isEmpty()) {
      return Map.of();
    }
    return fileMetadataRepository.getByIds(distinct).stream()
        .collect(Collectors.toMap(FileMetadata::getId, f -> f, (a, b) -> a));
  }

  private TimelineMessageDto buildResolvedTimelineMessage(
      Message message, Map<String, FileMetadata> knownAttachments) {
    Map<String, FileMetadata> attachmentsById = new HashMap<>(knownAttachments);
    if (message.getAttachmentId() != null
        && !attachmentsById.containsKey(message.getAttachmentId())) {
      fileMetadataRepository
          .getById(message.getAttachmentId())
          .ifPresent(m -> attachmentsById.put(m.getId(), m));
    }
    TimelineMessageDto dto = timelineItemMapper.baseMessageFields(message, attachmentsById);
    if (message.getReplyToId() != null) {
      messageRepository
          .getById(message.getReplyToId())
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

  private static String sendTempId(@Nullable String tempId) {
    return tempId == null || tempId.isBlank() ? null : tempId;
  }

  private Message firstRequestMessage(String roomId, @Nullable String messageId) {
    return Optional.ofNullable(messageId)
        .flatMap(messageRepository::getById)
        .filter(m -> roomId.equals(m.getRoomId()))
        .orElseThrow(
            () ->
                new ConflictException("The first request with this tempId has not completed yet"));
  }

  private void releaseSendClaim(String roomId, String userId, String tempId, String messageId) {
    try {
      messageSendDedupRepository.release(roomId, userId, tempId, messageId);
    } catch (Exception e) {
      ChatsLogger.warn(
          "Could not release the send claim of tempId '" + tempId + "': " + e.getMessage());
    }
  }

  private void notifyMessageSent(
      UserPrincipal sender,
      String roomId,
      String messageId,
      String text,
      OffsetDateTime createdAt) {
    UUID room = UUID.fromString(roomId);
    for (MessageSentListener listener : messageSentListeners) {
      try {
        listener.onMessageSent(sender, room, messageId, text, createdAt);
      } catch (Exception e) {
        ChatsLogger.warn("Error in message-sent listener: " + e.getMessage());
      }
    }
  }

  /**
   * Broadcasts an event to all members of the given room.
   *
   * <p>Sender-side filtering of "self events" (e.g. avoiding double-processing on the actor's own
   * client) is handled by the frontend WS event handler, which has the full session context to
   * decide what to skip and what to keep (e.g. read-updated echoes are still needed by the sender
   * to clear their unread counter).
   */
  private void broadcastToRoomMembers(String roomId, WsEvent event) {
    List<String> memberIds = subscriptionRepository.getUserIdsByRoomId(roomId);
    eventDispatcher.sendWsEvent(memberIds, event);
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
              .timestamp(systemEventCreatedAt);
      broadcastToRoomMembers(roomId, event);
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting unpin event: " + e.getMessage());
    }
  }

  private void broadcastReactionChanged(
      String roomId, OffsetDateTime messageCreatedAt, ReactionChanged event) throws Exception {
    messageReceivedBroadcaster.broadcastToMembersWhoCanAccess(roomId, messageCreatedAt, event);
  }

  private static ReactionChanged reactionEvent(
      String roomId, String messageId, String userId, String reaction) {
    return ReactionChanged.create()
        .messageId(messageId)
        .roomId(roomId)
        .userId(userId)
        .reaction(reaction);
  }

  // ==================== INBOX OPERATIONS ====================

  @Override
  @Transactional(readOnly = true)
  public Map<String, InboxLastItem> getInboxLastItems(String userId) {
    Map<String, InboxLastItemRefs> refs = messageRepository.getLastItemRefsByUserId(userId);
    if (refs.isEmpty()) {
      return Map.of();
    }
    Map<String, Message> messagesById =
        messageRepository
            .getByIdsWithReactions(
                refs.values().stream()
                    .filter(r -> !r.eventIsNewer())
                    .map(InboxLastItemRefs::messageId)
                    .filter(Objects::nonNull)
                    .toList())
            .stream()
            .collect(Collectors.toMap(Message::getId, m -> m, (a, b) -> a));
    List<String> eventIds =
        refs.values().stream()
            .filter(InboxLastItemRefs::eventIsNewer)
            .map(InboxLastItemRefs::eventId)
            .toList();
    Map<String, SystemEvent> eventsById =
        systemEventRepository.getByIds(eventIds).stream()
            .collect(Collectors.toMap(SystemEvent::getId, e -> e, (a, b) -> a));

    List<String> replyingIds =
        refs.values().stream()
            .filter(r -> !r.eventIsNewer() && r.messageId() != null)
            .map(r -> messagesById.get(r.messageId()))
            .filter(m -> m != null && m.getReplyToId() != null)
            .map(Message::getId)
            .toList();
    Map<String, Message> originalsById =
        messageRepository.getVisibleRepliedOriginals(userId, replyingIds).stream()
            .collect(Collectors.toMap(Message::getId, m -> m, (a, b) -> a));

    Map<String, InboxLastItem> result = new HashMap<>();
    refs.forEach(
        (roomId, ref) -> {
          Message message = ref.messageId() == null ? null : messagesById.get(ref.messageId());
          SystemEvent event = ref.eventIsNewer() ? eventsById.get(ref.eventId()) : null;
          if (event != null) {
            result.put(roomId, new InboxLastItem(new TimelineItem.SystemEventItem(event), null));
          } else if (message != null) {
            result.put(
                roomId,
                new InboxLastItem(
                    new TimelineItem.MessageItem(message),
                    message.getReplyToId() == null
                        ? null
                        : originalsById.get(message.getReplyToId())));
          }
        });
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Long> getUnreadCounts(String userId) {
    return messageRepository.getUnreadCountsByUserId(userId, UNREAD_CAP);
  }

  @Override
  public Map<String, ReadMarker> getReadMarkersByUserId(String userId) {
    List<ReadMarker> markers = readRepository.getByUserId(userId);
    Map<String, ReadMarker> result = new java.util.HashMap<>();
    for (ReadMarker marker : markers) {
      result.put(marker.getId().getRoomId(), marker);
    }
    return result;
  }

  // ==================== REACTION OPERATIONS ====================

  @Override
  public SetResult setReaction(
      String roomId, String messageId, UserPrincipal user, String reaction) {
    SetResult result = setReaction(roomId, messageId, user.getId(), reaction);
    notifyMessageSent(user, roomId, messageId, reaction, result.messageCreatedAt());
    return result;
  }

  @Override
  public SetResult setReaction(String roomId, String messageId, String userId, String reaction) {
    VisibilityFloor floor = getVisibilityFloor(roomId, userId);
    Message target =
        messageRepository
            .getById(messageId)
            .filter(m -> roomId.equals(m.getRoomId()))
            .filter(m -> !Boolean.TRUE.equals(m.isDeleted()))
            .filter(m -> floor.canAccess(m.getCreatedAt()))
            .orElseThrow(
                () -> new NotFoundException(String.format("Message '%s' not found", messageId)));
    SetResult result =
        reactionRepository
            .set(messageId, userId, reaction)
            .withMessageCreatedAt(target.getCreatedAt());
    try {
      if (result.kind() != SetKind.UNCHANGED) {
        ReactionChanged event =
            reactionEvent(roomId, messageId, userId, reaction)
                .operation(
                    result.kind() == SetKind.ADDED
                        ? ReactionChanged.OperationEnum.ADDED
                        : ReactionChanged.OperationEnum.UPDATED)
                .previousReaction(result.previousReaction())
                .updatedAt(result.updatedAt());
        broadcastReactionChanged(roomId, result.messageCreatedAt(), event);
      }
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting reaction event: " + e.getMessage());
    }
    return result;
  }

  @Override
  public Optional<Removed> removeReaction(String roomId, String messageId, String userId) {
    VisibilityFloor floor = getVisibilityFloor(roomId, userId);
    Message target =
        messageRepository
            .getById(messageId)
            .filter(m -> roomId.equals(m.getRoomId()))
            .filter(m -> floor.canAccess(m.getCreatedAt()))
            .orElseThrow(
                () -> new NotFoundException(String.format("Message '%s' not found", messageId)));
    Optional<Removed> removed =
        reactionRepository
            .remove(messageId, userId)
            .map(r -> r.withMessageCreatedAt(target.getCreatedAt()));
    try {
      if (removed.isPresent()) {
        ReactionChanged event =
            reactionEvent(roomId, messageId, userId, removed.get().reaction())
                .operation(ReactionChanged.OperationEnum.REMOVED)
                .deletedAt(removed.get().deletedAt());
        broadcastReactionChanged(roomId, removed.get().messageCreatedAt(), event);
      }
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting reaction event: " + e.getMessage());
    }
    return removed;
  }

  @Override
  public List<MessageReaction> getReactions(String messageId) {
    return reactionRepository.getByMessageId(messageId);
  }

  // ==================== PIN OPERATIONS ====================

  @Override
  public PinResult pinMessage(String roomId, String messageId, String userId) {
    PinResult pinned = storePin(roomId, messageId, userId);
    SystemEvent systemEvent = pinned.systemEvent();
    try {
      TimelineMessageDto message = renderPinned(pinned.message(), roomId);
      messageReceivedBroadcaster.broadcastWithReplyVisibility(
          roomId,
          message,
          visible ->
              MessagePinned.create()
                  .messageId(messageId)
                  .roomId(roomId)
                  .pinnedBy(systemEvent.getActorId())
                  .systemEventId(systemEvent.getId())
                  .systemEventCreatedAt(systemEvent.getCreatedAt())
                  .timestamp(systemEvent.getCreatedAt())
                  .message(visible));
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting pin event: " + e.getMessage());
    }
    return pinned;
  }

  private TimelineMessageDto renderPinned(Message message, String roomId) {
    MessageContext context =
        resolveContext(messageItems(List.of(message)), roomId, new VisibilityFloor(null));
    return timelineItemMapper
        .toMessageTimelineItem(message, context.attachmentsById(), context.repliedById())
        .getMessage();
  }

  @Transactional
  PinResult storePin(String roomId, String messageId, String userId) {
    VisibilityFloor floor = getVisibilityFloor(roomId, userId);
    // Validate message exists and belongs to this room
    Message message =
        messageRepository
            .getById(messageId)
            .orElseThrow(
                () -> new NotFoundException(String.format("Message '%s' not found", messageId)));
    if (!roomId.equals(message.getRoomId())) {
      throw new NotFoundException(
          String.format("Message '%s' does not belong to room '%s'", messageId, roomId));
    }
    if (Boolean.TRUE.equals(message.isDeleted()) || !floor.canAccess(message.getCreatedAt())) {
      throw new NotFoundException(String.format("Message '%s' not found", messageId));
    }

    // Both DB writes are atomic: pin record + system event in the same operation.
    // Any exception from either step rolls back both via the surrounding @Transactional.
    MessagePin pin =
        pinRepository
            .pin(roomId, messageId, userId)
            .orElseThrow(
                () -> new NotFoundException(String.format("Message '%s' not found", messageId)));
    String content;
    try {
      content = objectMapper.writeValueAsString(Map.of("pinnedBy", userId, "messageId", messageId));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize MESSAGE_PINNED system event content", e);
    }
    SystemEvent event =
        SystemEvent.create()
            .id(UUID.randomUUID().toString())
            .roomId(roomId)
            .type("MESSAGE_PINNED")
            .actorId(userId)
            .content(content);
    return new PinResult(message, systemEventRepository.insertAt(event, pin.getPinnedAt()));
  }

  @Override
  public Optional<SystemEvent> unpinMessage(String roomId, String messageId, String userId) {
    Optional<SystemEvent> unpinned = storeUnpin(roomId, messageId, userId);
    unpinned.ifPresent(
        systemEvent ->
            broadcastMessageUnpinned(
                roomId,
                messageId,
                systemEvent.getActorId(),
                systemEvent.getId(),
                systemEvent.getCreatedAt()));
    return unpinned;
  }

  @Transactional
  Optional<SystemEvent> storeUnpin(String roomId, String messageId, String userId) {
    getVisibilityFloor(roomId, userId);
    // Both DB writes are atomic: unpin record + system event in the same operation.
    // Any exception from either step rolls back both via the surrounding @Transactional.
    if (pinRepository.unpin(roomId, messageId) == 0) {
      return Optional.empty();
    }
    String content;
    try {
      content =
          objectMapper.writeValueAsString(Map.of("unpinnedBy", userId, "messageId", messageId));
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to serialize MESSAGE_UNPINNED system event content", e);
    }
    SystemEvent event =
        SystemEvent.create()
            .id(UUID.randomUUID().toString())
            .roomId(roomId)
            .type("MESSAGE_UNPINNED")
            .actorId(userId)
            .content(content);
    systemEventRepository.insert(event);
    return Optional.of(event);
  }

  @Override
  public List<MessagePin> getPinnedMessages(String roomId, String userId) {
    getVisibilityFloor(roomId, userId);
    return pinRepository.getPinnedMessages(roomId);
  }

  @Override
  public PinnedMessagesView getPinnedMessagesView(String roomId, String userId) {
    VisibilityFloor floor = getVisibilityFloor(roomId, userId);
    List<MessagePin> pins = pinRepository.getPinnedMessages(roomId);
    List<Message> messages = pins.stream().map(MessagePin::getMessage).toList();
    return new PinnedMessagesView(pins, resolveContext(messageItems(messages), roomId, floor));
  }

  // ==================== READ MARKER OPERATIONS (Smart Markers) ====================

  @Override
  public Optional<OffsetDateTime> setReadMarker(String roomId, String userId, String itemId) {
    VisibilityFloor floor = getVisibilityFloor(roomId, userId);
    OffsetDateTime itemCreatedAt =
        messageRepository
            .getById(itemId)
            .filter(m -> roomId.equals(m.getRoomId()))
            .map(Message::getCreatedAt)
            .or(
                () ->
                    systemEventRepository
                        .getById(itemId)
                        .filter(e -> roomId.equals(e.getRoomId()))
                        .map(SystemEvent::getCreatedAt))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Message or system event '%s' not found", itemId)));

    if (!floor.canAccess(itemCreatedAt)) {
      throw new NotFoundException(String.format("Message or system event '%s' not found", itemId));
    }

    Optional<OffsetDateTime> advanced = readRepository.advance(userId, roomId, itemCreatedAt);
    advanced.ifPresent(
        movedTo -> {
          try {
            WsEvent event =
                ReadUpdated.create().roomId(roomId).userId(userId).itemCreatedAt(movedTo);
            messageReceivedBroadcaster.broadcastToMembersWhoCanAccess(roomId, movedTo, event);
          } catch (Exception e) {
            ChatsLogger.warn(
                "Error broadcasting read-updated event after markRead: " + e.getMessage());
          }
        });
    return advanced;
  }

  @Override
  public Optional<ReadMarker> getReadMarker(String roomId, String userId) {
    return readRepository.get(userId, roomId);
  }

  @Override
  public ReadMarkersView getRoomReadMarkers(String roomId, String userId) {
    VisibilityFloor floor = getVisibilityFloor(roomId, userId);
    return new ReadMarkersView(readRepository.getByRoomId(roomId), floor);
  }

  // ==================== MEMBERSHIP VALIDATION ====================

  @Override
  public void validateReplyTarget(String roomId, String userId, String replyToId) {
    validateReplyTarget(roomId, getVisibilityFloor(roomId, userId), replyToId);
  }

  @Override
  public void validateReplyTarget(String roomId, VisibilityFloor floor, String replyToId) {
    messageRepository
        .getById(replyToId)
        .filter(m -> roomId.equals(m.getRoomId()))
        .filter(m -> floor.canAccess(m.getCreatedAt()))
        .orElseThrow(
            () -> new NotFoundException(String.format("Message '%s' not found", replyToId)));
  }

  @Override
  public VisibilityFloor getVisibilityFloor(String roomId, String userId) {
    return roomAccess.getVisibilityFloor(roomId, userId);
  }
}
