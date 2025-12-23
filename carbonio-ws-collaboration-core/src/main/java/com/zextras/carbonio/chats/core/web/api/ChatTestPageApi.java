// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Serves the chat test page for WebSocket testing. */
@Path("/new-chat")
public class ChatTestPageApi {

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Response getTestPage() {
    return Response.ok(HTML_PAGE).build();
  }

  private static final String HTML_PAGE =
      """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Chats</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: #f0f2f5;
      height: 100vh;
      display: flex;
      flex-direction: column;
    }
    .app { display: flex; flex: 1; overflow: hidden; }
    .sidebar {
      width: 350px;
      background: white;
      border-right: 1px solid #e0e0e0;
      display: flex;
      flex-direction: column;
    }
    .sidebar-header {
      padding: 16px;
      background: #1976d2;
      color: white;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .sidebar-header h1 { font-size: 18px; font-weight: 500; }
    .status-indicator { display: flex; align-items: center; gap: 6px; font-size: 12px; }
    .status-dot { width: 8px; height: 8px; border-radius: 50%; background: #ff5252; }
    .status-dot.connected { background: #69f0ae; }
    .search-box { padding: 12px; border-bottom: 1px solid #e0e0e0; }
    .search-box input {
      width: 100%;
      padding: 10px 14px;
      border: 1px solid #e0e0e0;
      border-radius: 20px;
      outline: none;
      font-size: 14px;
    }
    .search-box input:focus { border-color: #1976d2; }
    .room-list { flex: 1; overflow-y: auto; }
    .room-item {
      padding: 14px 16px;
      border-bottom: 1px solid #f0f0f0;
      cursor: pointer;
      display: flex;
      gap: 12px;
      align-items: center;
    }
    .room-item:hover { background: #f5f5f5; }
    .room-item.active { background: #e3f2fd; }
    .room-avatar {
      width: 48px; height: 48px;
      border-radius: 50%;
      background: #1976d2;
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 500;
      font-size: 18px;
      flex-shrink: 0;
    }
    .room-info { flex: 1; min-width: 0; }
    .room-name { font-weight: 500; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .room-last-message { font-size: 13px; color: #666; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .room-meta { text-align: right; flex-shrink: 0; }
    .room-time { font-size: 11px; color: #999; margin-bottom: 4px; }
    .unread-badge { background: #1976d2; color: white; font-size: 11px; padding: 2px 6px; border-radius: 10px; }
    .chat-area { flex: 1; display: flex; flex-direction: column; background: #e5ddd5; overflow: hidden; min-height: 0; }
    .chat-header {
      padding: 12px 16px;
      background: white;
      border-bottom: 1px solid #e0e0e0;
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .chat-header-info { flex: 1; }
    .chat-header-name { font-weight: 500; }
    .chat-header-status { font-size: 12px; color: #666; }
    .chat-header-actions { display: flex; gap: 8px; }
    .chat-header-actions button {
      padding: 8px 12px;
      background: #f5f5f5;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 13px;
    }
    .chat-header-actions button:hover { background: #e0e0e0; }
    .messages-container { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; min-height: 0; overflow-anchor: none; }
    .message {
      max-width: 65%;
      margin-bottom: 8px;
      padding: 8px 12px;
      border-radius: 8px;
      background: white;
      box-shadow: 0 1px 1px rgba(0,0,0,0.1);
      position: relative;
    }
    .message.sent { background: #dcf8c6; align-self: flex-end; }
    .message.received { align-self: flex-start; }
    .message-sender { font-size: 12px; font-weight: 500; color: #1976d2; margin-bottom: 2px; }
    .message-text { word-break: break-word; line-height: 1.4; }
    .message-meta { display: flex; justify-content: flex-end; gap: 4px; margin-top: 4px; font-size: 11px; color: #999; align-items: center; }
    .message.deleted .message-text { font-style: italic; color: #999; }
    .message-reactions { display: flex; gap: 4px; margin-top: 4px; flex-wrap: wrap; }
    .reaction-badge { background: rgba(0,0,0,0.05); padding: 2px 6px; border-radius: 10px; font-size: 12px; cursor: pointer; }
    .reaction-badge:hover { background: rgba(0,0,0,0.1); }
    .typing-indicator { padding: 8px 16px; font-size: 13px; color: #666; font-style: italic; }
    .reply-preview {
      background: rgba(0,0,0,0.05);
      border-left: 3px solid #1976d2;
      padding: 6px 10px;
      margin-bottom: 6px;
      border-radius: 4px;
      font-size: 12px;
    }
    .reply-preview-sender { font-weight: 500; color: #1976d2; }
    .reply-preview-text { color: #666; margin-top: 2px; }
    .forwarded-label { font-size: 11px; color: #666; font-style: italic; margin-bottom: 4px; }
    .input-area { padding: 12px 16px; background: white; display: flex; flex-direction: column; gap: 8px; flex-shrink: 0; }
    .reply-bar {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      background: #f5f5f5;
      border-radius: 8px;
      font-size: 13px;
    }
    .reply-bar-content { flex: 1; }
    .reply-bar-close { cursor: pointer; font-size: 18px; color: #666; }
    .input-row { display: flex; gap: 12px; align-items: center; }
    .input-row input {
      flex: 1;
      padding: 12px 16px;
      border: 1px solid #e0e0e0;
      border-radius: 24px;
      outline: none;
      font-size: 14px;
    }
    .input-row input:focus { border-color: #1976d2; }
    .input-row button {
      width: 48px; height: 48px;
      border-radius: 50%;
      background: #1976d2;
      color: white;
      border: none;
      cursor: pointer;
      font-size: 18px;
    }
    .input-row button:hover { background: #1565c0; }
    .input-row button:disabled { background: #ccc; cursor: not-allowed; }
    .empty-state {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #666;
      background: #f0f2f5;
    }
    .empty-state svg { width: 120px; height: 120px; margin-bottom: 16px; opacity: 0.5; }
    .empty-state p { font-size: 16px; }
    .search-results { background: white; padding: 16px; border-bottom: 1px solid #e0e0e0; max-height: 200px; overflow-y: auto; }
    .search-results h3 { font-size: 14px; margin-bottom: 8px; color: #666; }
    .search-result-item { padding: 8px; border-radius: 4px; cursor: pointer; margin-bottom: 4px; }
    .search-result-item:hover { background: #f5f5f5; }
    .search-result-text { font-size: 13px; }
    .search-result-date { font-size: 11px; color: #999; }
    .date-separator { text-align: center; margin: 16px 0; }
    .date-separator span { background: rgba(0,0,0,0.1); padding: 4px 12px; border-radius: 8px; font-size: 12px; color: #666; }
    .loading { text-align: center; padding: 20px; color: #666; }
    .go-to-latest {
      position: absolute;
      bottom: 80px;
      right: 24px;
      background: #1976d2;
      color: white;
      border: none;
      border-radius: 20px;
      padding: 8px 16px;
      cursor: pointer;
      font-size: 13px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.2);
      z-index: 10;
    }
    .go-to-latest:hover { background: #1565c0; }
    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    .modal { background: white; border-radius: 8px; padding: 24px; min-width: 300px; max-width: 500px; }
    .modal h2 { margin-bottom: 16px; font-size: 18px; }
    .modal input { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px; margin-bottom: 16px; }
    .modal-buttons { display: flex; gap: 8px; justify-content: flex-end; }
    .modal-buttons button { padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; }
    .modal-buttons .primary { background: #1976d2; color: white; }
    .modal-buttons .secondary { background: #e0e0e0; }
    .room-select-list { max-height: 300px; overflow-y: auto; margin-bottom: 16px; }
    .room-select-item { padding: 12px; border: 1px solid #e0e0e0; border-radius: 4px; margin-bottom: 8px; cursor: pointer; }
    .room-select-item:hover { background: #f5f5f5; }
    .room-select-item.selected { background: #e3f2fd; border-color: #1976d2; }
    .message-menu {
      position: absolute;
      top: 4px;
      background: white;
      border-radius: 4px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.2);
      z-index: 100;
      min-width: 120px;
    }
    .message.sent .message-menu { right: 100%; margin-right: 4px; }
    .message.received .message-menu { left: 100%; margin-left: 4px; }
    .message-menu-item { padding: 8px 12px; cursor: pointer; font-size: 13px; }
    .message-menu-item:hover { background: #f5f5f5; }
    .message-menu-item.danger { color: #e53935; }
    .menu-trigger {
      position: absolute;
      top: 4px;
      opacity: 0;
      cursor: pointer;
      padding: 4px 8px;
      font-size: 16px;
      color: #666;
      transition: opacity 0.2s;
    }
    .message.sent .menu-trigger { left: -28px; }
    .message.received .menu-trigger { right: -28px; }
    .message:hover .menu-trigger { opacity: 1; }
    .checkmarks { color: #53bdeb; font-size: 14px; margin-left: 2px; }
    .checkmarks.read { color: #53bdeb; }
    .checkmarks.delivered { color: #999; }
    .hidden { display: none !important; }
  </style>
</head>
<body>
  <div class="app">
    <div class="sidebar">
      <div class="sidebar-header">
        <h1>Chats</h1>
        <div style="display:flex;align-items:center;gap:12px;">
          <button onclick="openNewRoomModal()" style="padding:6px 12px;background:white;color:#1976d2;border:none;border-radius:4px;cursor:pointer;font-size:13px;">+ New</button>
          <div class="status-indicator">
            <span id="status-text">Connecting...</span>
            <div id="status-dot" class="status-dot"></div>
          </div>
        </div>
      </div>
      <div class="search-box">
        <input type="text" id="room-search" placeholder="Search conversations..." oninput="filterRooms()">
      </div>
      <div class="room-list" id="room-list">
        <div class="loading">Loading conversations...</div>
      </div>
    </div>

    <div class="chat-area" id="chat-area">
      <div class="empty-state" id="empty-state">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
        </svg>
        <p>Select a conversation to start chatting</p>
      </div>

      <div id="chat-content" class="hidden" style="display:flex;flex-direction:column;flex:1;min-height:0;overflow:hidden;">
        <div class="chat-header">
          <div class="room-avatar" id="chat-avatar">?</div>
          <div class="chat-header-info">
            <div class="chat-header-name" id="chat-name">Chat</div>
            <div class="chat-header-status" id="chat-status"></div>
          </div>
          <div class="chat-header-actions">
            <button onclick="openSearchModal()">Search</button>
            <button onclick="openTestModal()">Test</button>
          </div>
        </div>

        <div id="search-results" class="search-results hidden"></div>

        <div style="position:relative;flex:1;display:flex;flex-direction:column;min-height:0;">
          <div class="messages-container" id="messages-container" onscroll="handleScroll()">
            <div class="loading">Loading messages...</div>
          </div>
          <button id="go-to-latest-btn" class="go-to-latest hidden" onclick="goToLatest()">&#8595; Go to latest</button>
        </div>

        <div id="typing-indicator" class="typing-indicator hidden"></div>

        <div class="input-area">
          <div id="reply-bar" class="reply-bar hidden">
            <div class="reply-bar-content">
              <div id="reply-to-sender" style="font-weight:500;color:#1976d2;"></div>
              <div id="reply-to-text" style="color:#666;"></div>
            </div>
            <span class="reply-bar-close" onclick="cancelReply()">&times;</span>
          </div>
          <div class="input-row">
            <input type="text" id="message-input" placeholder="Type a message..." onkeypress="handleKeyPress(event)" oninput="handleTyping()">
            <button onclick="sendMessage()" id="send-btn">&#10148;</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div id="search-modal" class="modal-overlay hidden">
    <div class="modal">
      <h2>Search Messages</h2>
      <input type="text" id="search-input" placeholder="Enter search term..." onkeypress="if(event.key==='Enter')performSearch()">
      <div class="modal-buttons">
        <button class="secondary" onclick="closeSearchModal()">Cancel</button>
        <button class="primary" onclick="performSearch()">Search</button>
      </div>
    </div>
  </div>

  <div id="forward-modal" class="modal-overlay hidden">
    <div class="modal">
      <h2>Forward to...</h2>
      <div class="room-select-list" id="forward-room-list"></div>
      <div class="modal-buttons">
        <button class="secondary" onclick="closeForwardModal()">Cancel</button>
        <button class="primary" onclick="confirmForward()">Forward</button>
      </div>
    </div>
  </div>

  <div id="edit-modal" class="modal-overlay hidden">
    <div class="modal">
      <h2>Edit Message</h2>
      <input type="text" id="edit-input" onkeypress="if(event.key==='Enter')confirmEdit()">
      <div class="modal-buttons">
        <button class="secondary" onclick="closeEditModal()">Cancel</button>
        <button class="primary" onclick="confirmEdit()">Save</button>
      </div>
    </div>
  </div>

  <div id="test-modal" class="modal-overlay hidden">
    <div class="modal">
      <h2>Generate Test Messages</h2>
      <p style="margin-bottom:12px;color:#666;font-size:13px;">Enter the number of random messages to send to this chat:</p>
      <input type="number" id="test-count-input" min="1" value="10" placeholder="Number of messages..." onkeypress="if(event.key==='Enter')confirmTestMessages()">
      <div class="modal-buttons">
        <button class="secondary" onclick="closeTestModal()">Cancel</button>
        <button class="primary" onclick="confirmTestMessages()">Send Messages</button>
      </div>
    </div>
  </div>

  <div id="new-room-modal" class="modal-overlay hidden">
    <div class="modal" style="min-width:400px;">
      <h2>New Conversation</h2>
      <div style="margin-bottom:12px;">
        <label style="display:block;margin-bottom:4px;font-size:13px;color:#666;">Search users:</label>
        <input type="text" id="user-search-input" placeholder="Type to search users..." oninput="searchUsers()">
      </div>
      <div id="user-search-results" style="max-height:200px;overflow-y:auto;margin-bottom:12px;"></div>
      <div style="margin-bottom:12px;">
        <label style="display:block;margin-bottom:4px;font-size:13px;color:#666;">Selected users:</label>
        <div id="selected-users" style="display:flex;flex-wrap:wrap;gap:4px;min-height:32px;padding:8px;background:#f5f5f5;border-radius:4px;"></div>
      </div>
      <div style="margin-bottom:12px;" id="group-options" class="hidden">
        <label style="display:block;margin-bottom:4px;font-size:13px;color:#666;">Group name (optional):</label>
        <input type="text" id="new-room-name" placeholder="Enter group name...">
      </div>
      <div class="modal-buttons">
        <button class="secondary" onclick="closeNewRoomModal()">Cancel</button>
        <button class="primary" onclick="confirmNewRoom()">Start Chat</button>
      </div>
    </div>
  </div>

<script>
let ws = null;
let currentUserId = null;
let currentRoomId = null;
let rooms = {};
let roomMessages = {};
let roomReadStatus = {};
let typingUsers = {};
let typingTimeout = null;
let isTyping = false;
let requestId = 0;
let replyToMessage = null;
let forwardMessageId = null;
let forwardTargetRoomId = null;
let editMessageId = null;
let openMenuId = null;
let loadingOlder = false;
let loadingNewer = false;
let hasMoreOlder = true;
let hasMoreNewer = false; // false for latest view, true after search jump
let scrollUpdateInProgress = false; // Block scroll handler during DOM updates
let userEmailsCache = {}; // Global cache for user emails (userId -> email)
let pendingHistoryRoomId = null; // Track which room we're loading history for
let selectedUsersForNewRoom = []; // Users selected for new room creation
let searchTimeout = null; // Debounce timer for user search
let allSearchableUsers = []; // All users from SearchUsersByFeatureRequest
let currentUserEmail = null; // Current user's email from GetInfoRequest
let jumpToMessageId = null; // Message to highlight after loading around

const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
const wsUrl = wsProtocol + '//' + window.location.host + '/services/chats/messages-json';
const usersApiBase = window.location.origin + '/services/chats/users';
const soapUrl = window.location.origin + '/service/soap/';

// Fetch user info from REST API
async function fetchUserEmails(userIds) {
  if (!userIds || userIds.length === 0) return;
  // Filter out already cached users
  const uncachedIds = userIds.filter(id => !userEmailsCache[id]);
  if (uncachedIds.length === 0) return;

  try {
    const params = uncachedIds.map(id => 'userIds=' + encodeURIComponent(id)).join('&');
    const response = await fetch(usersApiBase + '?' + params, { credentials: 'include' });
    if (response.ok) {
      const users = await response.json();
      users.forEach(user => {
        if (user.email) {
          userEmailsCache[user.id] = user.email;
        }
      });
      // Re-render if needed
      renderRoomList();
      if (currentRoomId) renderMessages();
    }
  } catch (e) {
    console.error('Error fetching user emails:', e);
  }
}

function getUserDisplayName(userId) {
  if (userEmailsCache[userId]) {
    return userEmailsCache[userId];
  }
  return userId ? userId.substring(0, 12) + '...' : 'unknown';
}

function init() {
  document.addEventListener('click', function(e) {
    if (!e.target.closest('.message-menu') && !e.target.classList.contains('menu-trigger')) {
      closeAllMenus();
    }
  });
  connect();
}

function connect() {
  updateStatus('Connecting...', false);
  try {
    ws = new WebSocket(wsUrl);
    ws.onopen = function() { updateStatus('Connected', true); };
    ws.onclose = function(e) {
      updateStatus('Disconnected', false);
      ws = null;
      setTimeout(connect, 3000);
    };
    ws.onerror = function(e) { console.error('WebSocket error:', e); };
    ws.onmessage = function(e) { handleMessage(JSON.parse(e.data)); };
  } catch (e) {
    console.error('Connection failed:', e);
    updateStatus('Error', false);
  }
}

function send(action, data) {
  if (!ws || ws.readyState !== WebSocket.OPEN) return;
  const msg = Object.assign({ action: action, requestId: 'r' + (++requestId) }, data || {});
  ws.send(JSON.stringify(msg));
}

function updateStatus(text, connected) {
  document.getElementById('status-text').textContent = text;
  document.getElementById('status-dot').className = connected ? 'status-dot connected' : 'status-dot';
}

function handleMessage(data) {
  switch (data.event) {
    case 'CONNECTED':
      currentUserId = data.userId;
      send('GET_INBOX');
      break;
    case 'INBOX_RESPONSE':
      handleInbox(data.inbox || []);
      break;
    case 'ROOM_CREATED':
      handleRoomCreated(data);
      break;
    case 'HISTORY_RESPONSE':
      handleHistory(data.roomId, data.messages || [], false);
      break;
    case 'MESSAGES_AROUND_RESPONSE':
      handleMessagesAround(data.roomId, data.messages || []);
      break;
    case 'SEARCH_RESPONSE':
      handleSearchResults(data.messages || []);
      break;
    case 'READ_STATUS_RESPONSE':
      roomReadStatus[data.roomId] = data.readStatus || {};
      if (data.roomId === currentRoomId) renderMessages();
      break;
    case 'MESSAGE_RECEIVED':
      handleNewMessage(data.message);
      break;
    case 'MESSAGE_EDITED':
      handleEditedMessage(data.message);
      break;
    case 'MESSAGE_DELETED':
      handleDeletedMessage(data.roomId, data.messageId);
      break;
    case 'REACTION_ADDED':
    case 'REACTION_REMOVED':
      if (data.roomId === currentRoomId) send('GET_HISTORY', { roomId: currentRoomId, limit: 100 });
      break;
    case 'USER_TYPING':
      handleUserTyping(data.roomId, data.userId);
      break;
    case 'USER_PAUSED':
      handleUserPaused(data.roomId, data.userId);
      break;
    case 'MESSAGE_READ':
      if (data.roomId === currentRoomId) {
        if (!roomReadStatus[data.roomId]) roomReadStatus[data.roomId] = {};
        roomReadStatus[data.roomId][data.userId] = data.messageId;
        renderMessages();
      }
      break;
    case 'ERROR':
      console.error('Server error:', data.error);
      break;
  }
}

function handleInbox(inbox) {
  rooms = {};
  const allMemberIds = new Set();
  inbox.forEach(function(item) {
    rooms[item.roomId] = item;
    // Collect all member IDs
    if (item.members) {
      item.members.forEach(id => allMemberIds.add(id));
    }
  });
  renderRoomList();
  // Fetch user emails asynchronously
  fetchUserEmails(Array.from(allMemberIds));
}

function handleHistory(roomId, messages, isContextLoad) {
  // Only process if this is still the room we're interested in
  if (roomId !== currentRoomId) {
    if (!roomMessages[roomId]) {
      roomMessages[roomId] = messages.reverse();
    }
    return;
  }

  // Block scroll handler during entire update
  scrollUpdateInProgress = true;

  const container = document.getElementById('messages-container');
  const wasLoadingOlder = loadingOlder;
  const wasLoadingNewer = loadingNewer;
  const scrollHeightBefore = container.scrollHeight;
  const scrollTopBefore = container.scrollTop;
  const clientHeight = container.clientHeight;
  // For NEWER: remember distance from bottom to maintain it
  const distanceFromBottomBefore = scrollHeightBefore - scrollTopBefore - clientHeight;

  console.log('[handleHistory] START - wasLoadingOlder:', wasLoadingOlder, 'wasLoadingNewer:', wasLoadingNewer);
  console.log('[handleHistory] BEFORE - scrollHeight:', scrollHeightBefore, 'scrollTop:', scrollTopBefore, 'distFromBottom:', distanceFromBottomBefore);
  console.log('[handleHistory] received messages:', messages.length);

  if (wasLoadingOlder && roomMessages[roomId] && roomMessages[roomId].length > 0) {
    // Loading older messages - prepend to existing
    const existing = roomMessages[roomId];
    const existingIds = new Set(existing.map(m => m.id));
    const newMsgs = messages.reverse().filter(m => !existingIds.has(m.id));
    console.log('[handleHistory] OLDER - adding', newMsgs.length, 'messages at beginning');
    roomMessages[roomId] = newMsgs.concat(existing);
    hasMoreOlder = messages.length > 0;
  } else if (wasLoadingNewer && roomMessages[roomId] && roomMessages[roomId].length > 0) {
    // Loading newer messages - append to existing
    const existing = roomMessages[roomId];
    const existingIds = new Set(existing.map(m => m.id));
    const newMsgs = messages.filter(m => !existingIds.has(m.id));
    console.log('[handleHistory] NEWER - adding', newMsgs.length, 'messages at end');
    roomMessages[roomId] = existing.concat(newMsgs);
    hasMoreNewer = messages.length > 0;
  } else {
    // Initial load - messages are DESC, reverse to ASC
    console.log('[handleHistory] INITIAL load');
    roomMessages[roomId] = messages.reverse();
    hasMoreOlder = messages.length >= 100;
    // hasMoreNewer stays as it was set (false for normal, true for search)
  }

  renderMessagesWithoutScroll();

  const scrollHeightAfter = container.scrollHeight;
  console.log('[handleHistory] AFTER RENDER - scrollHeight:', scrollHeightAfter);

  // Fix scroll position IMMEDIATELY (before any scroll events can fire)
  let targetScrollTop;
  if (wasLoadingOlder) {
    // Keep same view position: add the height difference to scrollTop
    targetScrollTop = scrollTopBefore + (scrollHeightAfter - scrollHeightBefore);
    console.log('[handleHistory] OLDER - target scrollTop:', targetScrollTop);
  } else if (wasLoadingNewer) {
    // Keep same distance from bottom (so we stay looking at the same messages)
    // distanceFromBottom should remain the same, so:
    // newScrollTop = newScrollHeight - clientHeight - distanceFromBottomBefore
    targetScrollTop = scrollHeightAfter - clientHeight - distanceFromBottomBefore;
    // But ensure we don't trigger another load immediately (distFromBottom should be > 100)
    const maxScrollTop = scrollHeightAfter - clientHeight - 150;
    if (targetScrollTop > maxScrollTop) {
      targetScrollTop = maxScrollTop;
    }
    console.log('[handleHistory] NEWER - target scrollTop:', targetScrollTop, '(max:', maxScrollTop, ')');
  } else {
    // Initial load - scroll to bottom
    targetScrollTop = scrollHeightAfter;
    console.log('[handleHistory] INITIAL - scrolling to bottom');
  }

  container.scrollTop = targetScrollTop;
  const intendedScrollTop = targetScrollTop;
  console.log('[handleHistory] FINAL scrollTop:', container.scrollTop, 'hasMoreOlder:', hasMoreOlder, 'hasMoreNewer:', hasMoreNewer);

  updateGoToLatestButton();

  // Reset flags
  loadingOlder = false;
  loadingNewer = false;

  // Re-enable scroll handler after a delay, verifying scroll position
  setTimeout(function() {
    // Check if browser changed our scrollTop (due to scroll anchoring)
    const actualScrollTop = container.scrollTop;
    if (Math.abs(actualScrollTop - intendedScrollTop) > 50) {
      console.log('[handleHistory] Browser changed scrollTop! Intended:', intendedScrollTop, 'Actual:', actualScrollTop, '- correcting');
      container.scrollTop = intendedScrollTop;
    }

    // Wait one more frame then re-enable
    requestAnimationFrame(function() {
      // Final check
      const finalScrollTop = container.scrollTop;
      const distFromTop = finalScrollTop;
      const distFromBottom = container.scrollHeight - finalScrollTop - container.clientHeight;
      console.log('[handleHistory] scroll handler re-enabled, scrollTop:', finalScrollTop, 'distFromTop:', distFromTop, 'distFromBottom:', distFromBottom);
      scrollUpdateInProgress = false;
    });
  }, 50);

  send('GET_READ_STATUS', { roomId: roomId });
}

function handleMessagesAround(roomId, messages) {
  if (roomId !== currentRoomId) return;

  console.log('[handleMessagesAround] START - messages:', messages.length);

  // Block ALL scroll handling during setup
  scrollUpdateInProgress = true;
  loadingOlder = true;
  loadingNewer = true;

  // Replace current messages
  roomMessages[roomId] = messages;
  hasMoreOlder = true;
  hasMoreNewer = true;

  renderMessagesWithoutScroll();
  updateGoToLatestButton();
  send('GET_READ_STATUS', { roomId: roomId });

  // Scroll to target message, then re-enable scroll handling
  const targetId = jumpToMessageId;
  jumpToMessageId = null;

  setTimeout(function() {
    const container = document.getElementById('messages-container');
    let intendedScrollTop = container.scrollTop;

    if (targetId) {
      const el = document.querySelector('[data-id="' + targetId + '"]');
      if (el) {
        el.scrollIntoView({ behavior: 'auto', block: 'center' });
        intendedScrollTop = container.scrollTop;
        el.style.background = '#fff9c4';
        setTimeout(function() { el.style.background = ''; }, 2000);
      }
      console.log('[handleMessagesAround] scrolled to message:', targetId, 'scrollTop:', intendedScrollTop);
    }

    // Re-enable scroll handling after verifying position
    loadingOlder = false;
    loadingNewer = false;

    setTimeout(function() {
      // Check if browser changed our scrollTop
      const actualScrollTop = container.scrollTop;
      if (Math.abs(actualScrollTop - intendedScrollTop) > 50) {
        console.log('[handleMessagesAround] Browser changed scrollTop! Intended:', intendedScrollTop, 'Actual:', actualScrollTop, '- correcting');
        container.scrollTop = intendedScrollTop;
      }

      requestAnimationFrame(function() {
        const finalScrollTop = container.scrollTop;
        const distFromTop = finalScrollTop;
        const distFromBottom = container.scrollHeight - finalScrollTop - container.clientHeight;
        console.log('[handleMessagesAround] scroll handler re-enabled, scrollTop:', finalScrollTop, 'distFromTop:', distFromTop, 'distFromBottom:', distFromBottom);
        scrollUpdateInProgress = false;
      });
    }, 50);
  }, 100);
}

function handleNewMessage(msg) {
  if (!msg) return;
  if (rooms[msg.roomId]) {
    rooms[msg.roomId].lastMessage = msg;
    if (msg.roomId !== currentRoomId) {
      rooms[msg.roomId].unreadCount = (rooms[msg.roomId].unreadCount || 0) + 1;
    }
    renderRoomList();
  }
  if (roomMessages[msg.roomId]) {
    const exists = roomMessages[msg.roomId].some(m => m.id === msg.id);
    if (!exists) roomMessages[msg.roomId].push(msg);
  }
  if (msg.roomId === currentRoomId) {
    renderMessages();
    scrollToBottom();
    // Only mark as read if we are NOT the sender
    if (msg.senderId !== currentUserId) {
      send('MARK_AS_READ', { roomId: msg.roomId, messageId: msg.id });
    }
  }
}

function handleEditedMessage(msg) {
  if (!msg || !roomMessages[msg.roomId]) return;
  const idx = roomMessages[msg.roomId].findIndex(m => m.id === msg.id);
  if (idx >= 0) {
    roomMessages[msg.roomId][idx] = msg;

    // Update replyTo in any messages that reference this one
    roomMessages[msg.roomId].forEach(m => {
      if (m.replyTo && m.replyTo.id === msg.id) {
        m.replyTo = {
          id: msg.id,
          senderId: msg.senderId,
          text: msg.text,
          createdAt: msg.createdAt
        };
      }
    });

    if (msg.roomId === currentRoomId) renderMessages();
  }
}

function handleDeletedMessage(roomId, messageId) {
  if (!roomMessages[roomId]) return;
  const idx = roomMessages[roomId].findIndex(m => m.id === messageId);
  if (idx >= 0) {
    roomMessages[roomId][idx].deleted = true;
    roomMessages[roomId][idx].text = '';

    // Update replyTo in any messages that reference this one
    roomMessages[roomId].forEach(m => {
      if (m.replyTo && m.replyTo.id === messageId) {
        m.replyTo.deleted = true;
        m.replyTo.text = '';
      }
    });

    if (roomId === currentRoomId) renderMessages();
  }
}

function handleUserTyping(roomId, userId) {
  if (userId === currentUserId) return;
  if (!typingUsers[roomId]) typingUsers[roomId] = {};
  typingUsers[roomId][userId] = Date.now();
  updateTypingIndicator();
  setTimeout(function() {
    if (typingUsers[roomId] && typingUsers[roomId][userId] && Date.now() - typingUsers[roomId][userId] > 4500) {
      delete typingUsers[roomId][userId];
      updateTypingIndicator();
    }
  }, 5000);
}

function handleUserPaused(roomId, userId) {
  if (typingUsers[roomId]) {
    delete typingUsers[roomId][userId];
    updateTypingIndicator();
  }
}

function updateTypingIndicator() {
  const indicator = document.getElementById('typing-indicator');
  if (!currentRoomId || !typingUsers[currentRoomId]) {
    indicator.classList.add('hidden');
    return;
  }
  const users = Object.keys(typingUsers[currentRoomId]);
  if (users.length === 0) {
    indicator.classList.add('hidden');
  } else {
    indicator.classList.remove('hidden');
    if (users.length === 1) {
      const userName = getUserDisplayName(users[0]);
      indicator.textContent = userName + ' is typing...';
    } else {
      indicator.textContent = users.length + ' people are typing...';
    }
  }
}

function getRoomDisplayName(room) {
  if (room.roomName) return room.roomName;
  if (room.roomType === 'ONE_TO_ONE' && room.members && room.members.length === 2) {
    const otherId = room.members.find(id => id !== currentUserId);
    if (otherId) {
      return getUserDisplayName(otherId);
    }
  }
  return room.roomId.substring(0, 8);
}

function renderRoomList() {
  const container = document.getElementById('room-list');
  const filter = document.getElementById('room-search').value.toLowerCase();
  const sorted = Object.values(rooms).sort((a, b) => {
    const aTime = a.lastMessage ? new Date(a.lastMessage.createdAt).getTime() : 0;
    const bTime = b.lastMessage ? new Date(b.lastMessage.createdAt).getTime() : 0;
    return bTime - aTime;
  });

  let html = '';
  sorted.forEach(function(room) {
    const name = getRoomDisplayName(room);
    if (filter && !name.toLowerCase().includes(filter)) return;

    const initial = name.charAt(0).toUpperCase();
    const lastMsg = room.lastMessage ? (room.lastMessage.text || '[deleted]') : 'No messages';
    const time = room.lastMessage ? formatTime(room.lastMessage.createdAt) : '';
    const unread = room.unreadCount > 0 ? '<span class="unread-badge">' + room.unreadCount + '</span>' : '';
    const active = room.roomId === currentRoomId ? ' active' : '';

    html += '<div class="room-item' + active + '" onclick="selectRoom(\\'' + room.roomId + '\\')">' +
      '<div class="room-avatar">' + initial + '</div>' +
      '<div class="room-info">' +
        '<div class="room-name">' + escapeHtml(name) + '</div>' +
        '<div class="room-last-message">' + escapeHtml(lastMsg.substring(0, 50)) + '</div>' +
      '</div>' +
      '<div class="room-meta">' +
        '<div class="room-time">' + time + '</div>' +
        unread +
      '</div>' +
    '</div>';
  });

  container.innerHTML = html || '<div class="loading">No conversations</div>';
}

function selectRoom(roomId) {
  currentRoomId = roomId;
  const room = rooms[roomId];
  hasMoreOlder = true;
  hasMoreNewer = false; // Normal view - we're at latest
  loadingOlder = false;
  loadingNewer = false;
  cancelReply();
  updateGoToLatestButton();

  document.getElementById('empty-state').classList.add('hidden');
  document.getElementById('chat-content').classList.remove('hidden');

  const name = getRoomDisplayName(room);
  document.getElementById('chat-name').textContent = name;
  document.getElementById('chat-avatar').textContent = name.charAt(0).toUpperCase();
  document.getElementById('chat-status').textContent = room.roomType || '';

  room.unreadCount = 0;
  renderRoomList();

  document.getElementById('messages-container').innerHTML = '<div class="loading">Loading messages...</div>';
  send('GET_HISTORY', { roomId: roomId, limit: 100 });

  document.getElementById('search-results').classList.add('hidden');
  document.getElementById('message-input').focus();
}

function renderMessages() {
  const container = document.getElementById('messages-container');
  const messages = roomMessages[currentRoomId] || [];
  const readStatus = roomReadStatus[currentRoomId] || {};

  if (messages.length === 0) {
    container.innerHTML = '<div class="loading">No messages yet</div>';
    return;
  }

  const readByOthers = {};
  for (const userId in readStatus) {
    if (userId !== currentUserId) {
      const msgId = readStatus[userId];
      if (!readByOthers[msgId]) readByOthers[msgId] = [];
      readByOthers[msgId].push(userId);
    }
  }

  let html = '';
  let lastDate = '';

  messages.forEach(function(msg, idx) {
    const date = formatDate(msg.createdAt);
    if (date !== lastDate) {
      html += '<div class="date-separator"><span>' + date + '</span></div>';
      lastDate = date;
    }

    const isSent = msg.senderId === currentUserId;
    const cls = isSent ? 'sent' : 'received';
    const deleted = msg.deleted ? ' deleted' : '';

    html += '<div class="message ' + cls + deleted + '" data-id="' + msg.id + '">';

    if (!msg.deleted) {
      html += '<span class="menu-trigger" onclick="toggleMenu(\\'' + msg.id + '\\', event)">&#8942;</span>';
    }

    if (msg.forwardedFrom) {
      const originalSender = getUserDisplayName(msg.forwardedFrom.senderId);
      html += '<div class="forwarded-label">Forwarded from ' + escapeHtml(originalSender) + '</div>';
    }

    if (msg.replyTo) {
      const replyText = msg.replyTo.deleted ? '<em>Message deleted</em>' : escapeHtml((msg.replyTo.text || '').substring(0, 50));
      html += '<div class="reply-preview">' +
        '<div class="reply-preview-sender">' + escapeHtml(getUserDisplayName(msg.replyTo.senderId)) + '</div>' +
        '<div class="reply-preview-text">' + replyText + '</div>' +
      '</div>';
    }

    if (!isSent) {
      html += '<div class="message-sender">' + escapeHtml(getUserDisplayName(msg.senderId)) + '</div>';
    }

    html += '<div class="message-text">' + (msg.deleted ? '<em>Message deleted</em>' : escapeHtml(msg.text)) + '</div>';

    if (msg.reactions && Object.keys(msg.reactions).length > 0) {
      html += '<div class="message-reactions">';
      for (const emoji in msg.reactions) {
        const userHasReaction = msg.reactions[emoji].includes(currentUserId);
        const badgeStyle = userHasReaction ? 'background:#bbdefb;border:1px solid #1976d2;' : '';
        html += '<span class="reaction-badge" style="' + badgeStyle + '" onclick="toggleReaction(\\'' + msg.id + '\\', \\'' + emoji + '\\')">' +
          emoji + ' ' + msg.reactions[emoji].length + '</span>';
      }
      html += '</div>';
    }

    html += '<div class="message-meta">';
    html += '<span>' + formatTime(msg.createdAt) + '</span>';
    if (msg.edited) html += '<span>(edited)</span>';
    if (isSent && !msg.deleted) {
      const isRead = Object.values(readStatus).some(lastRead => {
        const lastReadIdx = messages.findIndex(m => m.id === lastRead);
        return lastReadIdx >= idx;
      });
      html += '<span class="checkmarks ' + (isRead ? 'read' : 'delivered') + '">' + (isRead ? '&#10003;&#10003;' : '&#10003;') + '</span>';
    }
    html += '</div></div>';
  });

  container.innerHTML = html;
  scrollToBottom();
}

function renderMessagesWithoutScroll() {
  const container = document.getElementById('messages-container');
  const messages = roomMessages[currentRoomId] || [];
  const readStatus = roomReadStatus[currentRoomId] || {};

  if (messages.length === 0) {
    container.innerHTML = '<div class="loading">No messages yet</div>';
    return;
  }

  const readByOthers = {};
  for (const userId in readStatus) {
    if (userId !== currentUserId) {
      const msgId = readStatus[userId];
      if (!readByOthers[msgId]) readByOthers[msgId] = [];
      readByOthers[msgId].push(userId);
    }
  }

  let html = '';
  let lastDate = '';

  messages.forEach(function(msg, idx) {
    const date = formatDate(msg.createdAt);
    if (date !== lastDate) {
      html += '<div class="date-separator"><span>' + date + '</span></div>';
      lastDate = date;
    }

    const isSent = msg.senderId === currentUserId;
    const cls = isSent ? 'sent' : 'received';
    const deleted = msg.deleted ? ' deleted' : '';

    html += '<div class="message ' + cls + deleted + '" data-id="' + msg.id + '">';

    if (!msg.deleted) {
      html += '<span class="menu-trigger" onclick="toggleMenu(\\'' + msg.id + '\\', event)">&#8942;</span>';
    }

    if (msg.forwardedFrom) {
      const originalSender = getUserDisplayName(msg.forwardedFrom.senderId);
      html += '<div class="forwarded-label">Forwarded from ' + escapeHtml(originalSender) + '</div>';
    }

    if (msg.replyTo) {
      const replyText = msg.replyTo.deleted ? '<em>Message deleted</em>' : escapeHtml((msg.replyTo.text || '').substring(0, 50));
      html += '<div class="reply-preview">' +
        '<div class="reply-preview-sender">' + escapeHtml(getUserDisplayName(msg.replyTo.senderId)) + '</div>' +
        '<div class="reply-preview-text">' + replyText + '</div>' +
      '</div>';
    }

    if (!isSent) {
      html += '<div class="message-sender">' + escapeHtml(getUserDisplayName(msg.senderId)) + '</div>';
    }

    html += '<div class="message-text">' + (msg.deleted ? '<em>Message deleted</em>' : escapeHtml(msg.text)) + '</div>';

    if (msg.reactions && Object.keys(msg.reactions).length > 0) {
      html += '<div class="message-reactions">';
      for (const emoji in msg.reactions) {
        const userHasReaction = msg.reactions[emoji].includes(currentUserId);
        const badgeStyle = userHasReaction ? 'background:#bbdefb;border:1px solid #1976d2;' : '';
        html += '<span class="reaction-badge" style="' + badgeStyle + '" onclick="toggleReaction(\\'' + msg.id + '\\', \\'' + emoji + '\\')">' +
          emoji + ' ' + msg.reactions[emoji].length + '</span>';
      }
      html += '</div>';
    }

    html += '<div class="message-meta">';
    html += '<span>' + formatTime(msg.createdAt) + '</span>';
    if (msg.edited) html += '<span>(edited)</span>';
    if (isSent && !msg.deleted) {
      const isRead = Object.values(readStatus).some(lastRead => {
        const lastReadIdx = messages.findIndex(m => m.id === lastRead);
        return lastReadIdx >= idx;
      });
      html += '<span class="checkmarks ' + (isRead ? 'read' : 'delivered') + '">' + (isRead ? '&#10003;&#10003;' : '&#10003;') + '</span>';
    }
    html += '</div></div>';
  });

  container.innerHTML = html;
}

function toggleMenu(messageId, event) {
  event.stopPropagation();
  closeAllMenus();

  const msg = (roomMessages[currentRoomId] || []).find(m => m.id === messageId);
  if (!msg || msg.deleted) return;

  const isSent = msg.senderId === currentUserId;
  const isForwarded = !!msg.forwardedFrom || !!msg.forwardedFromId;
  const canEdit = isSent && !isForwarded;
  const msgEl = document.querySelector('[data-id="' + messageId + '"]');
  if (!msgEl) return;

  const menu = document.createElement('div');
  menu.className = 'message-menu';
  menu.innerHTML =
    '<div class="message-menu-item" onclick="replyTo(\\'' + messageId + '\\')">Reply</div>' +
    '<div class="message-menu-item" onclick="openForwardModal(\\'' + messageId + '\\')">Forward</div>' +
    (canEdit ? '<div class="message-menu-item" onclick="openEditModal(\\'' + messageId + '\\')">Edit</div>' : '') +
    (isSent ? '<div class="message-menu-item danger" onclick="deleteMessage(\\'' + messageId + '\\')">Delete</div>' : '') +
    '<div class="message-menu-item" onclick="addReactionPrompt(\\'' + messageId + '\\')">Add Reaction</div>';

  msgEl.appendChild(menu);
  openMenuId = messageId;
}

function closeAllMenus() {
  document.querySelectorAll('.message-menu').forEach(m => m.remove());
  openMenuId = null;
}

function replyTo(messageId) {
  closeAllMenus();
  const msg = (roomMessages[currentRoomId] || []).find(m => m.id === messageId);
  if (!msg) return;

  replyToMessage = msg;
  document.getElementById('reply-to-sender').textContent = getUserDisplayName(msg.senderId);
  document.getElementById('reply-to-text').textContent = (msg.text || '').substring(0, 50);
  document.getElementById('reply-bar').classList.remove('hidden');
  document.getElementById('message-input').focus();
}

function cancelReply() {
  replyToMessage = null;
  document.getElementById('reply-bar').classList.add('hidden');
}

function openForwardModal(messageId) {
  closeAllMenus();
  forwardMessageId = messageId;
  forwardTargetRoomId = null;

  const list = document.getElementById('forward-room-list');
  let html = '';
  Object.values(rooms).forEach(room => {
    if (room.roomId !== currentRoomId) {
      const name = getRoomDisplayName(room);
      html += '<div class="room-select-item" data-room="' + room.roomId + '" onclick="selectForwardRoom(\\'' + room.roomId + '\\')">' +
        escapeHtml(name) + '</div>';
    }
  });
  list.innerHTML = html || '<p>No other conversations available</p>';
  document.getElementById('forward-modal').classList.remove('hidden');
}

function selectForwardRoom(roomId) {
  forwardTargetRoomId = roomId;
  document.querySelectorAll('.room-select-item').forEach(el => el.classList.remove('selected'));
  document.querySelector('[data-room="' + roomId + '"]').classList.add('selected');
}

function confirmForward() {
  if (!forwardMessageId || !forwardTargetRoomId) return;
  send('FORWARD_MESSAGE', { messageId: forwardMessageId, targetRoomId: forwardTargetRoomId });
  closeForwardModal();
}

function closeForwardModal() {
  document.getElementById('forward-modal').classList.add('hidden');
  forwardMessageId = null;
  forwardTargetRoomId = null;
}

function openEditModal(messageId) {
  closeAllMenus();
  const msg = (roomMessages[currentRoomId] || []).find(m => m.id === messageId);
  if (!msg) return;

  editMessageId = messageId;
  document.getElementById('edit-input').value = msg.text || '';
  document.getElementById('edit-modal').classList.remove('hidden');
  document.getElementById('edit-input').focus();
}

function confirmEdit() {
  const text = document.getElementById('edit-input').value.trim();
  if (!text || !editMessageId) return;
  send('EDIT_MESSAGE', { messageId: editMessageId, text: text });
  closeEditModal();
}

function closeEditModal() {
  document.getElementById('edit-modal').classList.add('hidden');
  editMessageId = null;
}

function openTestModal() {
  if (!currentRoomId) return;
  document.getElementById('test-count-input').value = '10';
  document.getElementById('test-modal').classList.remove('hidden');
  document.getElementById('test-count-input').focus();
}

function closeTestModal() {
  document.getElementById('test-modal').classList.add('hidden');
}

// New Room Modal Functions
async function openNewRoomModal() {
  selectedUsersForNewRoom = [];
  document.getElementById('user-search-input').value = '';
  document.getElementById('user-search-results').innerHTML = '<div style="padding:8px;color:#666;">Loading users...</div>';
  document.getElementById('new-room-name').value = '';
  document.getElementById('group-options').classList.add('hidden');
  renderSelectedUsers();
  document.getElementById('new-room-modal').classList.remove('hidden');

  // Load current user email if not already loaded
  if (!currentUserEmail) {
    await loadCurrentUserEmail();
  }

  // Load all users initially
  await loadAllUsers('');
  document.getElementById('user-search-input').focus();
}

function closeNewRoomModal() {
  document.getElementById('new-room-modal').classList.add('hidden');
  selectedUsersForNewRoom = [];
}

async function loadCurrentUserEmail() {
  try {
    const payload = {
      Body: {
        GetInfoRequest: {
          _jsns: 'urn:zimbraAccount',
          rights: 'sendAs,sendAsDistList,viewFreeBusy,sendOnBehalfOf,sendOnBehalfOfDistList'
        }
      },
      Header: {
        context: {
          _jsns: 'urn:zimbra',
          session: {},
          userAgent: { name: 'CarbonioWebClient' }
        }
      }
    };

    const response = await fetch(soapUrl + 'GetInfoRequest', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (response.ok) {
      const data = await response.json();
      if (data.Body && data.Body.GetInfoResponse) {
        currentUserEmail = data.Body.GetInfoResponse.name;
      }
    }
  } catch (e) {
    console.error('Error loading current user email:', e);
  }
}

async function loadAllUsers(filterName) {
  try {
    const payload = {
      Body: {
        SearchUsersByFeatureRequest: {
          _jsns: 'urn:zimbraAccount',
          name: filterName || '',
          feature: 'WSC',
          offset: 0
        }
      },
      Header: {
        context: {
          _jsns: 'urn:zimbra',
          session: {},
          account: currentUserEmail ? { by: 'name', _content: currentUserEmail } : undefined,
          userAgent: { name: 'CarbonioWebClient' }
        }
      }
    };

    const response = await fetch(soapUrl + 'SearchUsersByFeatureRequest', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      throw new Error('Search failed');
    }

    const data = await response.json();
    if (data.Body && data.Body.SearchUsersByFeatureResponse) {
      const accounts = data.Body.SearchUsersByFeatureResponse.account || [];
      allSearchableUsers = accounts.map(acc => {
        // Extract email from attributes
        let email = acc.name;
        if (acc.a) {
          const mailAttr = acc.a.find(attr => attr.n === 'mail');
          if (mailAttr) email = mailAttr._content;
        }
        return {
          id: acc.id,
          email: email,
          name: acc.name
        };
      });
      renderSearchResults(allSearchableUsers);
    } else {
      allSearchableUsers = [];
      renderSearchResults([]);
    }
  } catch (e) {
    console.error('User search error:', e);
    document.getElementById('user-search-results').innerHTML = '<div style="padding:8px;color:#e53935;">Failed to load users. Please try again.</div>';
  }
}

async function searchUsers() {
  const input = document.getElementById('user-search-input');
  const query = input.value.trim().toLowerCase();

  // Filter locally from allSearchableUsers
  if (allSearchableUsers.length > 0) {
    const filtered = allSearchableUsers.filter(user => {
      if (!query) return true;
      return (user.email && user.email.toLowerCase().includes(query)) ||
             (user.name && user.name.toLowerCase().includes(query));
    });
    renderSearchResults(filtered);
  } else {
    // Reload from server with filter
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => loadAllUsers(query), 300);
  }
}

function renderSearchResults(users) {
  const container = document.getElementById('user-search-results');
  const selectedIds = selectedUsersForNewRoom.map(u => u.id);

  if (!users || users.length === 0) {
    container.innerHTML = '<div style="padding:8px;color:#666;">No users found</div>';
    return;
  }

  let html = '';
  users.forEach(user => {
    if (user.id === currentUserId) return; // Skip self
    if (selectedIds.includes(user.id)) return; // Skip already selected

    const displayName = user.email || user.name || user.id.substring(0, 12) + '...';
    html += '<div style="padding:8px 12px;cursor:pointer;border-bottom:1px solid #f0f0f0;" ' +
      'onclick="addUserToSelection(\\'' + user.id + '\\', \\'' + escapeHtml(displayName) + '\\')" ' +
      'onmouseover="this.style.background=\\'#f5f5f5\\'" onmouseout="this.style.background=\\'\\'"">' +
      '<div style="font-weight:500;">' + escapeHtml(displayName) + '</div>' +
      (user.name && user.email ? '<div style="font-size:12px;color:#666;">' + escapeHtml(user.name) + '</div>' : '') +
      '</div>';
  });

  container.innerHTML = html || '<div style="padding:8px;color:#666;">No more users to add</div>';
}

function addUserToSelection(userId, displayName) {
  if (selectedUsersForNewRoom.some(u => u.id === userId)) return;

  selectedUsersForNewRoom.push({ id: userId, name: displayName });

  // Cache the email
  if (!userEmailsCache[userId]) {
    userEmailsCache[userId] = displayName;
  }

  renderSelectedUsers();

  // Show group options if more than 1 user selected
  if (selectedUsersForNewRoom.length > 1) {
    document.getElementById('group-options').classList.remove('hidden');
  }

  // Re-render search results to remove selected user
  const input = document.getElementById('user-search-input');
  if (input.value.trim().length >= 2) {
    searchUsers();
  }
}

function removeUserFromSelection(userId) {
  selectedUsersForNewRoom = selectedUsersForNewRoom.filter(u => u.id !== userId);
  renderSelectedUsers();

  // Hide group options if 1 or fewer users selected
  if (selectedUsersForNewRoom.length <= 1) {
    document.getElementById('group-options').classList.add('hidden');
  }

  // Re-render search results
  const input = document.getElementById('user-search-input');
  if (input.value.trim().length >= 2) {
    searchUsers();
  }
}

function renderSelectedUsers() {
  const container = document.getElementById('selected-users');

  if (selectedUsersForNewRoom.length === 0) {
    container.innerHTML = '<span style="color:#999;font-size:13px;">No users selected</span>';
    return;
  }

  let html = '';
  selectedUsersForNewRoom.forEach(user => {
    html += '<span style="display:inline-flex;align-items:center;gap:4px;background:#1976d2;color:white;padding:4px 8px;border-radius:16px;font-size:13px;">' +
      escapeHtml(user.name) +
      '<span style="cursor:pointer;margin-left:2px;" onclick="removeUserFromSelection(\\'' + user.id + '\\')">&times;</span>' +
      '</span>';
  });

  container.innerHTML = html;
}

function confirmNewRoom() {
  if (selectedUsersForNewRoom.length === 0) {
    alert('Please select at least one user');
    return;
  }

  const memberIds = selectedUsersForNewRoom.map(u => u.id);
  const roomName = document.getElementById('new-room-name').value.trim();
  const roomType = selectedUsersForNewRoom.length > 1 ? 'GROUP' : 'ONE_TO_ONE';

  send('CREATE_ROOM', {
    memberIds: memberIds,
    roomType: roomType,
    roomName: roomName || null,
    roomDescription: null
  });

  closeNewRoomModal();
}

function handleRoomCreated(data) {
  // Add the new room to the inbox
  const room = {
    roomId: data.roomId,
    roomName: data.roomName,
    roomType: data.roomType,
    members: data.memberIds,
    unreadCount: 0,
    lastMessage: null
  };

  rooms[data.roomId] = room;
  renderRoomList();

  // Fetch emails for new members
  if (data.memberIds) {
    fetchUserEmails(data.memberIds);
  }

  // Auto-select the new room
  selectRoom(data.roomId);
}

function generateRandomWord(minLen, maxLen) {
  const chars = 'abcdefghijklmnopqrstuvwxyz';
  const len = Math.floor(Math.random() * (maxLen - minLen + 1)) + minLen;
  let word = '';
  for (let i = 0; i < len; i++) {
    word += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return word;
}

function generateRandomMessage() {
  // Generate 3 random words, each 3-12 characters
  const words = [];
  for (let i = 0; i < 3; i++) {
    words.push(generateRandomWord(3, 12));
  }
  return words.join(' ');
}

async function confirmTestMessages() {
  const countInput = document.getElementById('test-count-input');
  const count = parseInt(countInput.value, 10);

  if (isNaN(count) || count < 1) {
    alert('Please enter a valid number');
    return;
  }

  if (!currentRoomId) {
    alert('No room selected');
    return;
  }

  closeTestModal();

  // Send all messages without delay
  for (let i = 0; i < count; i++) {
    const text = generateRandomMessage();
    send('SEND_MESSAGE', { roomId: currentRoomId, text: text });
  }
}

function deleteMessage(messageId) {
  closeAllMenus();
  if (confirm('Delete this message?')) {
    send('DELETE_MESSAGE', { messageId: messageId });
  }
}

function addReactionPrompt(messageId) {
  closeAllMenus();
  const emoji = prompt('Enter emoji reaction:', '\\uD83D\\uDC4D');
  if (emoji) {
    send('ADD_REACTION', { messageId: messageId, reaction: emoji });
  }
}

function handleScroll() {
  // Skip if we're in the middle of updating the DOM
  if (scrollUpdateInProgress) {
    console.log('[handleScroll] BLOCKED - scrollUpdateInProgress');
    return;
  }

  const container = document.getElementById('messages-container');
  const scrollTop = container.scrollTop;
  const scrollHeight = container.scrollHeight;
  const clientHeight = container.clientHeight;
  const distanceFromBottom = scrollHeight - scrollTop - clientHeight;

  // Load older messages when scrolling near top
  if (scrollTop < 100 && !loadingOlder && hasMoreOlder) {
    console.log('[handleScroll] TRIGGER loadOlderMessages - scrollTop:', scrollTop);
    loadOlderMessages();
  }

  // Load newer messages when scrolling near bottom
  if (hasMoreNewer && !loadingNewer && distanceFromBottom < 100) {
    console.log('[handleScroll] TRIGGER loadNewerMessages - distanceFromBottom:', distanceFromBottom);
    loadNewerMessages();
  }
}

function loadOlderMessages() {
  const messages = roomMessages[currentRoomId] || [];
  if (messages.length === 0) return;

  loadingOlder = true;
  scrollUpdateInProgress = true; // Block scroll handler
  const oldestMsgId = messages[0].id;
  console.log('[loadOlderMessages] requesting before:', oldestMsgId);
  send('GET_HISTORY', { roomId: currentRoomId, beforeMessageId: oldestMsgId, limit: 50 });
}

function loadNewerMessages() {
  const messages = roomMessages[currentRoomId] || [];
  if (messages.length === 0) return;

  loadingNewer = true;
  scrollUpdateInProgress = true; // Block scroll handler
  const newestMsgId = messages[messages.length - 1].id;
  console.log('[loadNewerMessages] requesting after:', newestMsgId);
  send('GET_HISTORY', { roomId: currentRoomId, afterMessageId: newestMsgId, limit: 50 });
}

function goToLatest() {
  console.log('[goToLatest] resetting to latest messages');
  // Reset to normal view and reload latest messages
  scrollUpdateInProgress = true;
  hasMoreOlder = true;
  hasMoreNewer = false;
  loadingOlder = false;
  loadingNewer = false;
  roomMessages[currentRoomId] = [];
  document.getElementById('messages-container').innerHTML = '<div class="loading">Loading messages...</div>';
  send('GET_HISTORY', { roomId: currentRoomId, limit: 100 });
  updateGoToLatestButton();
}

function updateGoToLatestButton() {
  const btn = document.getElementById('go-to-latest-btn');
  // Show button when there are newer messages (we're not at latest)
  if (hasMoreNewer) {
    btn.classList.remove('hidden');
  } else {
    btn.classList.add('hidden');
  }
}

function scrollToBottom() {
  const container = document.getElementById('messages-container');
  container.scrollTop = container.scrollHeight;
}

function sendMessage() {
  const input = document.getElementById('message-input');
  const text = input.value.trim();
  if (!text || !currentRoomId) return;

  const data = { roomId: currentRoomId, text: text };
  if (replyToMessage) {
    data.replyToId = replyToMessage.id;
  }

  send('SEND_MESSAGE', data);
  input.value = '';
  cancelReply();

  if (isTyping) {
    isTyping = false;
    send('PAUSED', { roomId: currentRoomId });
  }
}

function handleKeyPress(e) {
  if (e.key === 'Enter') sendMessage();
}

function handleTyping() {
  if (!currentRoomId) return;
  if (!isTyping) {
    isTyping = true;
    send('TYPING', { roomId: currentRoomId });
  }
  clearTimeout(typingTimeout);
  typingTimeout = setTimeout(function() {
    if (isTyping) {
      isTyping = false;
      send('PAUSED', { roomId: currentRoomId });
    }
  }, 3000);
}

function toggleReaction(messageId, emoji) {
  // Check if user already has this reaction
  const msg = (roomMessages[currentRoomId] || []).find(m => m.id === messageId);
  if (msg && msg.reactions && msg.reactions[emoji] && msg.reactions[emoji].includes(currentUserId)) {
    send('REMOVE_REACTION', { messageId: messageId, reaction: emoji });
  } else {
    send('ADD_REACTION', { messageId: messageId, reaction: emoji });
  }
}

function openSearchModal() {
  document.getElementById('search-modal').classList.remove('hidden');
  document.getElementById('search-input').value = '';
  document.getElementById('search-input').focus();
}

function closeSearchModal() {
  document.getElementById('search-modal').classList.add('hidden');
}

function performSearch() {
  const text = document.getElementById('search-input').value.trim();
  if (!text || !currentRoomId) return;
  closeSearchModal();
  send('SEARCH_MESSAGES', { roomId: currentRoomId, searchText: text, limit: 50 });
}

function handleSearchResults(messages) {
  const container = document.getElementById('search-results');
  if (messages.length === 0) {
    container.innerHTML = '<h3>Search Results</h3><p>No messages found</p>';
  } else {
    let html = '<h3>Search Results (' + messages.length + ')</h3>';
    messages.forEach(function(msg) {
      html += '<div class="search-result-item" onclick="jumpToMessage(\\'' + msg.id + '\\')">' +
        '<div class="search-result-text">' + escapeHtml(msg.text.substring(0, 100)) + '</div>' +
        '<div class="search-result-date">' + formatDate(msg.createdAt) + ' ' + formatTime(msg.createdAt) + '</div>' +
      '</div>';
    });
    container.innerHTML = html;
  }
  container.classList.remove('hidden');
}

function jumpToMessage(messageId) {
  const el = document.querySelector('[data-id="' + messageId + '"]');
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    el.style.background = '#fff9c4';
    setTimeout(function() { el.style.background = ''; }, 2000);
  } else {
    // Message not loaded, need to load context around it
    jumpToMessageId = messageId;
    // Block scroll handler during loading
    loadingOlder = true;
    loadingNewer = true;
    // Clear current messages
    roomMessages[currentRoomId] = [];
    document.getElementById('messages-container').innerHTML = '<div class="loading">Loading...</div>';
    send('GET_MESSAGES_AROUND', { roomId: currentRoomId, messageId: messageId, limit: 50 });
  }
}

function filterRooms() { renderRoomList(); }

function formatTime(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);
  if (d.toDateString() === today.toDateString()) return 'Today';
  if (d.toDateString() === yesterday.toDateString()) return 'Yesterday';
  return d.toLocaleDateString();
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

init();
</script>
</body>
</html>
      """;
}
