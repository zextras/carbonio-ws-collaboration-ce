// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.data.entity.MessageReaction;
import com.zextras.carbonio.chats.core.data.entity.ReadMarker;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.entity.UserContact;
import com.zextras.carbonio.chats.core.data.model.InboxLastItem;
import com.zextras.carbonio.chats.core.data.model.InboxLastItemRefs;
import com.zextras.carbonio.chats.core.data.model.PinResult;
import com.zextras.carbonio.chats.core.data.model.PresenceSummary;
import com.zextras.carbonio.chats.core.data.model.TimelineItem;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.repository.MessagePinRepository;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.ReadMarkerRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.repository.UserContactRepository;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.utils.SearchTextUtils;
import io.ebean.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class ChatMessageServiceImpl implements ChatMessageService {

  private final MessageRepository messageRepository;
  private final MessageReactionRepository reactionRepository;
  private final ReadMarkerRepository readRepository;
  private final MessagePinRepository pinRepository;
  private final SystemEventRepository systemEventRepository;
  private final WsPresenceRepository wsPresenceRepository;
  private final UserContactRepository contactRepository;
  private final RoomRepository roomRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final ObjectMapper objectMapper;

  @Inject
  public ChatMessageServiceImpl(
      MessageRepository messageRepository,
      MessageReactionRepository reactionRepository,
      ReadMarkerRepository readRepository,
      MessagePinRepository pinRepository,
      SystemEventRepository systemEventRepository,
      WsPresenceRepository wsPresenceRepository,
      UserContactRepository contactRepository,
      RoomRepository roomRepository,
      SubscriptionRepository subscriptionRepository,
      ObjectMapper objectMapper) {
    this.messageRepository = messageRepository;
    this.reactionRepository = reactionRepository;
    this.readRepository = readRepository;
    this.pinRepository = pinRepository;
    this.systemEventRepository = systemEventRepository;
    this.wsPresenceRepository = wsPresenceRepository;
    this.contactRepository = contactRepository;
    this.roomRepository = roomRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.objectMapper = objectMapper;
  }

  // ==================== MESSAGE OPERATIONS ====================

  @Override
  public SystemEvent insertSystemEvent(SystemEvent event) {
    return systemEventRepository.insert(event);
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
  public List<Message> getMessageHistory(
      String roomId, OffsetDateTime start, OffsetDateTime end, int limit) {
    // For MAM, we query with before=end and after=start
    return messageRepository.getByRoomId(roomId, end, start, limit);
  }

  @Override
  public List<Message> getMessageHistory(
      String roomId, OffsetDateTime start, OffsetDateTime end, String endId, int limit) {
    // Use composite cursor when both end timestamp and anchor ID are provided
    return messageRepository.getByRoomId(roomId, end, endId, start, limit);
  }

  @Override
  public List<Message> getMessageHistory(
      String roomId, OffsetDateTime start, OffsetDateTime end, int limit, boolean ascending) {
    return messageRepository.getByRoomId(roomId, end, start, limit, ascending);
  }

  @Override
  public List<Message> getMessagesByIds(String roomId, List<String> stanzaIds) {
    return messageRepository.getByRoomIdAndIds(roomId, stanzaIds);
  }

  @Override
  public List<Message> searchMessages(
      String roomId, String searchText, OffsetDateTime before, OffsetDateTime after, int limit) {
    List<String> words = SearchTextUtils.toSearchWords(searchText);
    if (words.isEmpty()) {
      return List.of();
    }
    return messageRepository.searchByText(roomId, words, before, after, limit);
  }

  @Override
  public List<Message> getMessagesUpToId(
      String roomId, String toId, OffsetDateTime start, int limit) {
    return messageRepository.getByRoomIdUpToMessageId(roomId, toId, start, limit);
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
  public Optional<String> addReaction(
      String roomId, String messageId, String userId, String reaction) {
    VisibilityFloor floor = getVisibilityFloor(roomId, userId);
    Message message =
        messageRepository
            .getById(messageId)
            .filter(m -> roomId.equals(m.getRoomId()))
            .filter(m -> floor.canAccess(m.getCreatedAt()))
            .orElseThrow(
                () -> new NotFoundException(String.format("Message '%s' not found", messageId)));

    // A user has a single reaction per message. Replace an existing different emoji in place.
    Optional<MessageReaction> existing = reactionRepository.getByUserAndMessage(messageId, userId);
    if (existing.isPresent()) {
      MessageReaction current = existing.get();
      if (reaction.equals(current.getReaction())) {
        return Optional.empty();
      }
      String previous = current.getReaction();
      reactionRepository.update(current.reaction(reaction));
      return Optional.of(previous);
    }

    // Two concurrent first-time requests can both pass the check above (TOCTOU). Catch the
    // resulting duplicate-key violation and fall back to an update.
    try {
      reactionRepository.insert(MessageReaction.create(message, userId, reaction));
    } catch (io.ebean.DuplicateKeyException e) {
      reactionRepository
          .getByUserAndMessage(messageId, userId)
          .ifPresent(r -> reactionRepository.update(r.reaction(reaction)));
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> removeReaction(String messageId, String userId) {
    Optional<MessageReaction> existing = reactionRepository.getByUserAndMessage(messageId, userId);
    if (existing.isEmpty()) {
      return Optional.empty();
    }
    String removed = existing.get().getReaction();
    reactionRepository.delete(messageId, userId);
    return Optional.of(removed);
  }

  @Override
  public List<MessageReaction> getReactions(String messageId) {
    return reactionRepository.getByMessageId(messageId);
  }

  // ==================== PIN OPERATIONS ====================

  @Transactional
  @Override
  public PinResult pinMessage(String roomId, String messageId, String userId) {
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
            .content(content)
            .createdAt(pin.getPinnedAt());
    systemEventRepository.insert(event);
    return new PinResult(message, event);
  }

  @Transactional
  @Override
  public Optional<SystemEvent> unpinMessage(String roomId, String messageId, String userId) {
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
  public List<MessagePin> getPinnedMessages(String roomId) {
    return pinRepository.getPinnedMessages(roomId);
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

    return readRepository.advance(userId, roomId, itemCreatedAt);
  }

  @Override
  public Optional<ReadMarker> getReadMarker(String roomId, String userId) {
    return readRepository.get(userId, roomId);
  }

  @Override
  public List<ReadMarker> getRoomReadMarkers(String roomId) {
    return readRepository.getByRoomId(roomId);
  }

  // ==================== PRESENCE OPERATIONS ====================

  @Override
  public List<PresenceSummary> getBatchPresence(Collection<String> userIds) {
    return wsPresenceRepository.getBatchPresence(userIds);
  }

  // ==================== CONTACT/ROSTER OPERATIONS ====================

  @Override
  public List<String> getRoster(String userId) {
    return contactRepository.getByUserId(userId).stream().map(UserContact::getContactId).toList();
  }

  @Override
  public void addMutualContact(String user1Id, String user2Id) {
    contactRepository.addMutualContact(user1Id, user2Id);
  }

  @Override
  public List<String> getPresenceSubscribers(String userId) {
    // Offline users would drop the event anyway and load presence when they connect
    return roomRepository.getOnlineUserIdsSharingRoomWith(userId);
  }

  // ==================== MEMBERSHIP VALIDATION ====================

  @Override
  public void validateReplyTarget(String roomId, String userId, String replyToId) {
    VisibilityFloor floor = getVisibilityFloor(roomId, userId);
    messageRepository
        .getById(replyToId)
        .filter(m -> roomId.equals(m.getRoomId()))
        .filter(m -> floor.canAccess(m.getCreatedAt()))
        .orElseThrow(
            () -> new NotFoundException(String.format("Message '%s' not found", replyToId)));
  }

  @Override
  public VisibilityFloor getVisibilityFloor(String roomId, String userId) {
    return subscriptionRepository
        .getVisibilityFloor(roomId, userId)
        .orElseThrow(
            () ->
                roomRepository.getById(roomId).isEmpty()
                    ? new NotFoundException(String.format("Room '%s' not found", roomId))
                    : new ForbiddenException(
                        String.format("User '%s' is not a member of room '%s'", userId, roomId)));
  }

  @Override
  public void validateRoomMembership(String roomId, String userId) {
    Room room =
        roomRepository
            .getById(roomId)
            .orElseThrow(() -> new NotFoundException(String.format("Room '%s' not found", roomId)));
    boolean isMember = room.getSubscriptions().stream().anyMatch(s -> userId.equals(s.getUserId()));
    if (!isMember) {
      throw new ForbiddenException(
          String.format("User '%s' is not a member of room '%s'", userId, roomId));
    }
  }
}
