// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTRoom;
import com.zextras.carbonio.chats.core.data.model.message.ChatAction;
import com.zextras.carbonio.chats.core.data.model.message.ChatEvent;
import com.zextras.carbonio.chats.core.data.model.message.ChatRequest;
import com.zextras.carbonio.chats.core.data.model.message.ChatResponse;
import com.zextras.carbonio.chats.core.data.model.message.InboxItemDto;
import com.zextras.carbonio.chats.core.data.model.message.MessageDto;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.service.mongoosent.MNTChatService;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for MongoosENT chat messaging. Handles sending/receiving messages, reactions,
 * typing indicators, read markers, and room management.
 */
@Singleton
@ServerEndpoint(value = "/messages-json")
public class ChatWebSocketManager {

  private static final String CHAT_ROUTING_KEY = "chat-events";

  private final Map<String, String> consumerTagMap;
  private final Channel channel;
  private final ObjectMapper objectMapper;
  private final MNTChatService chatService;

  @Inject
  public ChatWebSocketManager(Channel channel, ObjectMapper objectMapper, MNTChatService chatService) {
    this.channel = channel;
    this.objectMapper = objectMapper;
    this.chatService = chatService;
    this.consumerTagMap = new ConcurrentHashMap<>();
    Runtime.getRuntime()
        .addShutdownHook(new Thread(this::stop, "Chat websocket manager shutdown hook"));
  }

  @OnOpen
  public void onOpen(Session session) {
    SessionPingManager.add(session);

    String userId = getUserIdFromSession(session);
    String queueId = session.getId();
    String userQueue = userId + "/" + queueId;

    // Send connected event
    ChatResponse connected =
        ChatResponse.create(ChatEvent.CONNECTED)
            .queueId(queueId)
            .userId(userId)
            .sentDate(OffsetDateTime.now());

    sendToSession(session, connected);

    if (channel == null || !channel.isOpen()) {
      ChatsLogger.error(
          String.format(
              "Unable to open chat websocket session %s: channel is not up!", queueId));
      return;
    }

    try {
      // Create exchange and queue for this user
      channel.exchangeDeclare(userId, BuiltinExchangeType.DIRECT, false, false, null);
      channel.queueDeclare(queueId, false, false, true, null);
      channel.queueBind(queueId, userId, CHAT_ROUTING_KEY);

      // Consumer to receive messages from RabbitMQ
      DeliverCallback deliverCallback =
          (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try {
              if (session.isOpen()) {
                session.getAsyncRemote().sendText(message);
              }
            } catch (Exception e) {
              ChatsLogger.warn(
                  String.format(
                      "Error sending chat message to websocket for user/queue '%s'%nMessage: '%s'",
                      userQueue, message));
            }
          };

      String tag = channel.basicConsume(queueId, true, deliverCallback, consumerTag -> {});
      consumerTagMap.put(queueId, tag);

    } catch (Exception e) {
      ChatsLogger.warn(
          String.format(
              "Error interacting with message broker for chat user/queue '%s'", userQueue));
    }
  }

  @OnMessage
  public void onMessage(Session session, String message) {
    if (message == null || message.isBlank()) return;

    String userId = getUserIdFromSession(session);

    try {
      ChatRequest request = objectMapper.readValue(message, ChatRequest.class);
      ChatResponse response = handleRequest(request, userId, session.getId());

      if (response != null) {
        // Set requestId for correlation
        response.requestId(request.getRequestId());
        sendToSession(session, response);
      }
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Error processing chat message from user '%s': %s", userId, e.getMessage()));
      sendToSession(session, ChatResponse.createError("Invalid request: " + e.getMessage()));
    }
  }

  private ChatResponse handleRequest(ChatRequest request, String userId, String sessionId) {
    if (request.getAction() == null) {
      return ChatResponse.createError("Missing action");
    }

    try {
      switch (request.getAction()) {
        case CREATE_ROOM:
          return handleCreateRoom(request, userId);
        case SEND_MESSAGE:
          return handleSendMessage(request, userId);
        case EDIT_MESSAGE:
          return handleEditMessage(request, userId);
        case DELETE_MESSAGE:
          return handleDeleteMessage(request, userId);
        case FORWARD_MESSAGE:
          return handleForwardMessage(request, userId);
        case ADD_REACTION:
          return handleAddReaction(request, userId);
        case REMOVE_REACTION:
          return handleRemoveReaction(request, userId);
        case MARK_AS_READ:
          return handleMarkAsRead(request, userId);
        case GET_READ_STATUS:
          return handleGetReadStatus(request, userId);
        case TYPING:
          return handleTyping(request, userId);
        case PAUSED:
          return handlePaused(request, userId);
        case GET_HISTORY:
          return handleGetHistory(request, userId);
        case GET_MESSAGES_AROUND:
          return handleGetMessagesAround(request, userId);
        case SEARCH_MESSAGES:
          return handleSearchMessages(request, userId);
        case GET_INBOX:
          return handleGetInbox(userId);
        case PING:
          return ChatResponse.create(ChatEvent.PONG);
        default:
          return ChatResponse.createError("Unknown action: " + request.getAction());
      }
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Error handling action %s: %s", request.getAction(), e.getMessage()));
      return ChatResponse.createError(e.getMessage());
    }
  }

  private ChatResponse handleCreateRoom(ChatRequest request, String userId) {
    MNTRoom.RoomType type =
        "GROUP".equalsIgnoreCase(request.getRoomType())
            ? MNTRoom.RoomType.GROUP
            : MNTRoom.RoomType.ONE_TO_ONE;

    MNTRoom room =
        chatService.createRoom(
            userId, type, request.getRoomName(), request.getRoomDescription(), request.getMemberIds());

    // Notify all members about the new room
    List<String> memberIds = chatService.getRoomMemberIds(room.getId());
    ChatResponse broadcast =
        ChatResponse.create(ChatEvent.ROOM_CREATED)
            .roomId(room.getId())
            .roomName(room.getName())
            .roomType(room.getType().toString())
            .memberIds(memberIds);

    for (String memberId : memberIds) {
      if (!memberId.equals(userId)) {
        broadcastToUser(memberId, broadcast);
      }
    }

    return ChatResponse.create(ChatEvent.ROOM_CREATED)
        .roomId(room.getId())
        .roomName(room.getName())
        .roomType(room.getType().toString())
        .memberIds(memberIds);
  }

  private ChatResponse handleSendMessage(ChatRequest request, String userId) {
    MessageDto message =
        chatService.sendMessage(
            request.getRoomId(),
            userId,
            request.getText(),
            request.getReplyToId(),
            request.getForwardedFromId(),
            null);

    // Broadcast to room members (excluding sender - they get direct response)
    ChatResponse broadcast =
        ChatResponse.create(ChatEvent.MESSAGE_RECEIVED)
            .roomId(request.getRoomId())
            .message(message);

    broadcastToRoom(request.getRoomId(), broadcast, userId);

    return ChatResponse.create(ChatEvent.MESSAGE_RECEIVED).message(message);
  }

  private ChatResponse handleEditMessage(ChatRequest request, String userId) {
    MessageDto message =
        chatService.editMessage(request.getMessageId(), userId, request.getText());

    // Broadcast to room members (excluding sender)
    ChatResponse broadcast =
        ChatResponse.create(ChatEvent.MESSAGE_EDITED)
            .roomId(message.getRoomId())
            .message(message);

    broadcastToRoom(message.getRoomId(), broadcast, userId);

    return ChatResponse.create(ChatEvent.MESSAGE_EDITED).message(message);
  }

  private ChatResponse handleDeleteMessage(ChatRequest request, String userId) {
    MessageDto message = chatService.getMessageById(request.getMessageId());
    chatService.deleteMessage(request.getMessageId(), userId);

    // Broadcast to room members (excluding sender)
    ChatResponse broadcast =
        ChatResponse.create(ChatEvent.MESSAGE_DELETED)
            .roomId(message.getRoomId())
            .messageId(request.getMessageId());

    broadcastToRoom(message.getRoomId(), broadcast, userId);

    return ChatResponse.create(ChatEvent.MESSAGE_DELETED)
        .roomId(message.getRoomId())
        .messageId(request.getMessageId());
  }

  private ChatResponse handleAddReaction(ChatRequest request, String userId) {
    MessageDto message = chatService.getMessageById(request.getMessageId());
    chatService.addReaction(request.getMessageId(), userId, request.getReaction());

    // Broadcast to room members
    ChatResponse broadcast =
        ChatResponse.create(ChatEvent.REACTION_ADDED)
            .roomId(message.getRoomId())
            .messageId(request.getMessageId())
            .userId(userId)
            .reaction(request.getReaction());

    broadcastToRoom(message.getRoomId(), broadcast);

    return ChatResponse.create(ChatEvent.REACTION_ADDED)
        .messageId(request.getMessageId())
        .reaction(request.getReaction());
  }

  private ChatResponse handleRemoveReaction(ChatRequest request, String userId) {
    MessageDto message = chatService.getMessageById(request.getMessageId());
    chatService.removeReaction(request.getMessageId(), userId, request.getReaction());

    // Broadcast to room members
    ChatResponse broadcast =
        ChatResponse.create(ChatEvent.REACTION_REMOVED)
            .roomId(message.getRoomId())
            .messageId(request.getMessageId())
            .userId(userId)
            .reaction(request.getReaction());

    broadcastToRoom(message.getRoomId(), broadcast);

    return ChatResponse.create(ChatEvent.REACTION_REMOVED)
        .messageId(request.getMessageId())
        .reaction(request.getReaction());
  }

  private ChatResponse handleMarkAsRead(ChatRequest request, String userId) {
    chatService.markAsRead(userId, request.getRoomId(), request.getMessageId());

    // Broadcast to room members
    ChatResponse broadcast =
        ChatResponse.create(ChatEvent.MESSAGE_READ)
            .roomId(request.getRoomId())
            .userId(userId)
            .messageId(request.getMessageId());

    broadcastToRoom(request.getRoomId(), broadcast);

    return ChatResponse.create(ChatEvent.MESSAGE_READ).messageId(request.getMessageId());
  }

  private ChatResponse handleTyping(ChatRequest request, String userId) {
    ChatResponse broadcast =
        ChatResponse.create(ChatEvent.USER_TYPING).roomId(request.getRoomId()).userId(userId);

    broadcastToRoom(request.getRoomId(), broadcast, userId);

    return null;
  }

  private ChatResponse handlePaused(ChatRequest request, String userId) {
    ChatResponse broadcast =
        ChatResponse.create(ChatEvent.USER_PAUSED).roomId(request.getRoomId()).userId(userId);

    broadcastToRoom(request.getRoomId(), broadcast, userId);

    return null;
  }

  private ChatResponse handleGetHistory(ChatRequest request, String userId) {
    int limit = request.getLimit() != null ? request.getLimit() : 100;
    List<MessageDto> messages;

    if (request.getAfterMessageId() != null) {
      // Loading newer messages (forward direction)
      messages = chatService.getHistoryAfter(
          request.getRoomId(), userId, limit, request.getAfterMessageId());
    } else {
      // Loading older messages (backward direction) or initial load
      messages = chatService.getHistory(
          request.getRoomId(), userId, limit, request.getBeforeMessageId());
    }

    return ChatResponse.create(ChatEvent.HISTORY_RESPONSE)
        .roomId(request.getRoomId())
        .messages(messages);
  }

  private ChatResponse handleGetMessagesAround(ChatRequest request, String userId) {
    int limit = request.getLimit() != null ? request.getLimit() : 50;
    List<MessageDto> messages =
        chatService.getMessagesAround(
            request.getRoomId(), userId, request.getMessageId(), limit);

    return ChatResponse.create(ChatEvent.MESSAGES_AROUND_RESPONSE)
        .roomId(request.getRoomId())
        .messages(messages);
  }

  private ChatResponse handleSearchMessages(ChatRequest request, String userId) {
    int limit = request.getLimit() != null ? request.getLimit() : 100;
    List<MessageDto> messages =
        chatService.searchMessages(
            request.getRoomId(), userId, request.getSearchText(), limit);

    return ChatResponse.create(ChatEvent.SEARCH_RESPONSE)
        .roomId(request.getRoomId())
        .messages(messages);
  }

  private ChatResponse handleForwardMessage(ChatRequest request, String userId) {
    MessageDto message =
        chatService.forwardMessage(
            request.getMessageId(), userId, request.getTargetRoomId());

    // Broadcast to target room members (excluding sender)
    ChatResponse broadcast =
        ChatResponse.create(ChatEvent.MESSAGE_RECEIVED)
            .roomId(request.getTargetRoomId())
            .message(message);

    broadcastToRoom(request.getTargetRoomId(), broadcast, userId);

    return ChatResponse.create(ChatEvent.MESSAGE_RECEIVED).message(message);
  }

  private ChatResponse handleGetReadStatus(ChatRequest request, String userId) {
    Map<String, String> readStatus =
        chatService.getReadStatus(request.getRoomId(), userId);

    return ChatResponse.create(ChatEvent.READ_STATUS_RESPONSE)
        .roomId(request.getRoomId())
        .readStatus(readStatus);
  }

  private ChatResponse handleGetInbox(String userId) {
    List<InboxItemDto> inbox = chatService.getInbox(userId);
    return ChatResponse.create(ChatEvent.INBOX_RESPONSE).inbox(inbox);
  }

  @OnClose
  public void onClose(Session session) {
    SessionPingManager.remove(session);
    closeSession(session);
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    SessionPingManager.remove(session);
    String userId = getUserIdFromSession(session);
    String queueId = session.getId();
    String userQueue = userId + "/" + queueId;
    ChatsLogger.warn(String.format("Chat websocket error for user/queue '%s': %s", userQueue, throwable.getMessage()));
    try {
      session.close();
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format("Error closing chat websocket session for user/queue '%s'", userQueue));
    }
  }

  private void closeSession(Session session) {
    String userId = getUserIdFromSession(session);
    String queueId = session.getId();
    String userQueue = userId + "/" + queueId;

    if (channel == null || !channel.isOpen()) {
      ChatsLogger.error(
          String.format(
              "Unable to close chat websocket session %s: channel is not up!", queueId));
      return;
    }

    queueConsumerCleanup(userQueue, queueId, userId);
  }

  private void queueConsumerCleanup(String userQueue, String queueId, String userId) {
    if (channel != null && channel.isOpen()) {
      basicCancel(queueId, userQueue);
      queueUnBind(userQueue, queueId, userId);
      queueDeleteNoWait(userQueue, queueId);
    }
  }

  private void basicCancel(String queueId, String userQueue) {
    String tag = consumerTagMap.get(queueId);
    if (tag != null) {
      try {
        channel.basicCancel(tag);
        consumerTagMap.remove(queueId);
      } catch (Exception e) {
        ChatsLogger.warn(
            String.format("Error cancelling chat consumer for user/queue '%s'", userQueue));
      }
    }
  }

  private void queueUnBind(String userQueue, String queueId, String userId) {
    try {
      channel.queueUnbind(queueId, userId, CHAT_ROUTING_KEY);
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format(
              "Error unbinding chat queue from exchange for user/queue '%s'", userQueue));
    }
  }

  private void queueDeleteNoWait(String userQueue, String queueId) {
    try {
      channel.queueDeleteNoWait(queueId, false, false);
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Error deleting chat queue for user/queue '%s'", userQueue));
    }
  }

  private String getUserIdFromSession(Session session) {
    return (String)
        ((HttpSession) session.getUserProperties().get(HttpSession.class.getName()))
            .getAttribute("userId");
  }

  private void sendToSession(Session session, ChatResponse response) {
    try {
      if (session.isOpen()) {
        String json = objectMapper.writeValueAsString(response);
        session.getAsyncRemote().sendText(json);
      }
    } catch (Exception e) {
      ChatsLogger.warn("Error sending response to chat websocket: " + e.getMessage());
    }
  }

  private void broadcastToRoom(String roomId, ChatResponse response) {
    broadcastToRoom(roomId, response, null);
  }

  private void broadcastToRoom(String roomId, ChatResponse response, String excludeUserId) {
    try {
      List<String> memberIds = chatService.getRoomMemberIds(roomId);
      String json = objectMapper.writeValueAsString(response);
      byte[] body = json.getBytes(StandardCharsets.UTF_8);

      for (String memberId : memberIds) {
        if (excludeUserId != null && memberId.equals(excludeUserId)) {
          continue;
        }
        broadcastToUserInternal(memberId, body);
      }
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting chat event to room: " + e.getMessage());
    }
  }

  private void broadcastToUser(String userId, ChatResponse response) {
    try {
      String json = objectMapper.writeValueAsString(response);
      byte[] body = json.getBytes(StandardCharsets.UTF_8);
      broadcastToUserInternal(userId, body);
    } catch (Exception e) {
      ChatsLogger.warn("Error broadcasting chat event to user: " + e.getMessage());
    }
  }

  private void broadcastToUserInternal(String userId, byte[] body) {
    try {
      channel.exchangeDeclare(userId, BuiltinExchangeType.DIRECT, false, false, null);
      channel.basicPublish(userId, CHAT_ROUTING_KEY, null, body);
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format("Error broadcasting chat event to user '%s': %s", userId, e.getMessage()));
    }
  }

  public void stop() {
    try {
      if (channel != null && channel.isOpen()) {
        channel.close();
        ChatsLogger.info("Chat websocket manager channel closed successfully.");
      }
    } catch (Exception e) {
      ChatsLogger.error("Error during stopping chat websocket manager", e);
    }
  }
}
