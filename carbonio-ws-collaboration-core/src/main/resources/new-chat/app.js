// Global state
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
let openMenuId = null;
let loadingOlder = false;
let loadingNewer = false;
let hasMoreOlder = true;
let hasMoreNewer = false;
let scrollUpdateInProgress = false;
let userEmailsCache = {};
let pendingHistoryRoomId = null;
let selectedUsersForNewRoom = [];
let searchTimeout = null;
let allSearchableUsers = [];
let currentUserEmail = null;
let jumpToMessageId = null;
let pendingFiles = [];
let deleteAttachmentsMessageId = null;
let uploadingMessageId = null; // Track message being uploaded
let inlineEditId = null; // Track message being edited inline

const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
const wsUrl = wsProtocol + '//' + window.location.host + '/services/chats/messages-json';
const usersApiBase = window.location.origin + '/services/chats/users';
const attachmentsApiBase = window.location.origin + '/services/chats/mnt-attachments';
const soapUrl = window.location.origin + '/service/soap/';

// === Initialization ===
function init() {
  document.addEventListener('click', function(e) {
    if (!e.target.closest('.message-menu') && !e.target.classList.contains('menu-trigger')) {
      closeAllMenus();
    }
  });
  connect();
}

// === WebSocket Connection ===
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

// === User Info ===
async function fetchUserEmails(userIds) {
  if (!userIds || userIds.length === 0) return;
  const uncachedIds = userIds.filter(id => !userEmailsCache[id]);
  if (uncachedIds.length === 0) return;
  try {
    const params = uncachedIds.map(id => 'userIds=' + encodeURIComponent(id)).join('&');
    const response = await fetch(usersApiBase + '?' + params, { credentials: 'include' });
    if (response.ok) {
      const users = await response.json();
      users.forEach(user => { if (user.email) userEmailsCache[user.id] = user.email; });
      renderRoomList();
      if (currentRoomId) renderMessages();
    }
  } catch (e) { console.error('Error fetching user emails:', e); }
}

function getUserDisplayName(userId) {
  return userEmailsCache[userId] || (userId ? userId.substring(0, 12) + '...' : 'unknown');
}

// === Message Handler ===
function handleMessage(data) {
  switch (data.event) {
    case 'CONNECTED':
      currentUserId = data.userId;
      send('GET_INBOX');
      break;
    case 'INBOX_RESPONSE': handleInbox(data.inbox || []); break;
    case 'ROOM_CREATED': handleRoomCreated(data); break;
    case 'HISTORY_RESPONSE': handleHistory(data.roomId, data.messages || [], false); break;
    case 'MESSAGES_AROUND_RESPONSE': handleMessagesAround(data.roomId, data.messages || []); break;
    case 'SEARCH_RESPONSE': handleSearchResults(data.messages || []); break;
    case 'READ_STATUS_RESPONSE':
      roomReadStatus[data.roomId] = data.readStatus || {};
      if (data.roomId === currentRoomId) renderMessages();
      break;
    case 'MESSAGE_RECEIVED': handleNewMessage(data.message); break;
    case 'MESSAGE_EDITED': handleEditedMessage(data.message); break;
    case 'MESSAGE_DELETED': handleDeletedMessage(data.roomId, data.messageId); break;
    case 'REACTION_ADDED':
    case 'REACTION_REMOVED':
      if (data.roomId === currentRoomId) send('GET_HISTORY', { roomId: currentRoomId, limit: 100 });
      break;
    case 'USER_TYPING': handleUserTyping(data.roomId, data.userId); break;
    case 'USER_PAUSED': handleUserPaused(data.roomId, data.userId); break;
    case 'MESSAGE_READ':
      if (data.roomId === currentRoomId) {
        if (!roomReadStatus[data.roomId]) roomReadStatus[data.roomId] = {};
        roomReadStatus[data.roomId][data.userId] = data.messageId;
        renderMessages();
      }
      break;
    case 'ERROR': console.error('Server error:', data.error); break;
  }
}

// === Inbox & Rooms ===
function handleInbox(inbox) {
  rooms = {};
  const allMemberIds = new Set();
  inbox.forEach(function(item) {
    rooms[item.roomId] = item;
    if (item.members) item.members.forEach(id => allMemberIds.add(id));
  });
  renderRoomList();
  fetchUserEmails(Array.from(allMemberIds));
}

function renderRoomList() {
  const container = document.getElementById('room-list');
  const filter = document.getElementById('room-search').value.toLowerCase();
  const sorted = Object.values(rooms).sort((a, b) => {
    const timeA = a.lastMessage ? new Date(a.lastMessage.createdAt).getTime() : 0;
    const timeB = b.lastMessage ? new Date(b.lastMessage.createdAt).getTime() : 0;
    return timeB - timeA;
  });

  if (sorted.length === 0) {
    container.innerHTML = '<div class="loading">No conversations yet</div>';
    return;
  }

  let html = '';
  sorted.forEach(function(room) {
    const name = getRoomDisplayName(room);
    if (filter && !name.toLowerCase().includes(filter)) return;
    const lastMsg = room.lastMessage;
    const time = lastMsg ? formatTime(lastMsg.createdAt) : '';
    const preview = lastMsg ? (lastMsg.deleted ? 'Message deleted' : (lastMsg.text || '(attachment)').substring(0, 30)) : 'No messages';
    const unread = room.unreadCount || 0;
    const active = room.roomId === currentRoomId ? 'active' : '';
    const initials = name.substring(0, 2).toUpperCase();
    html += '<div class="room-item ' + active + '" onclick="selectRoom(\'' + room.roomId + '\')">' +
      '<div class="room-avatar">' + initials + '</div>' +
      '<div class="room-info"><div class="room-name">' + escapeHtml(name) + '</div>' +
      '<div class="room-last-message">' + escapeHtml(preview) + '</div></div>' +
      '<div class="room-meta"><div class="room-time">' + time + '</div>' +
      (unread > 0 ? '<div class="unread-badge">' + unread + '</div>' : '') + '</div></div>';
  });
  container.innerHTML = html || '<div class="loading">No matches</div>';
}

function getRoomDisplayName(room) {
  if (room.roomName) return room.roomName;
  if (room.name) return room.name;
  if (room.roomType === 'ONE_TO_ONE' && room.members && room.members.length === 2) {
    const otherId = room.members.find(id => id !== currentUserId);
    if (otherId) return getUserDisplayName(otherId);
  }
  return room.roomId ? room.roomId.substring(0, 8) : 'Chat';
}

function selectRoom(roomId) {
  currentRoomId = roomId;
  const room = rooms[roomId];
  if (room) room.unreadCount = 0;
  renderRoomList();

  document.getElementById('empty-state').classList.add('hidden');
  document.getElementById('chat-content').classList.remove('hidden');

  const name = room ? getRoomDisplayName(room) : 'Chat';
  document.getElementById('chat-name').textContent = name;
  document.getElementById('chat-avatar').textContent = name.substring(0, 2).toUpperCase();
  document.getElementById('chat-status').textContent = room ? (room.roomType === 'ONE_TO_ONE' ? 'One to one' : 'Group') : '';

  document.getElementById('messages-container').innerHTML = '<div class="loading">Loading messages...</div>';
  document.getElementById('search-results').classList.add('hidden');

  loadingOlder = false;
  loadingNewer = false;
  hasMoreOlder = true;
  hasMoreNewer = false;
  jumpToMessageId = null;
  scrollUpdateInProgress = false;

  send('GET_HISTORY', { roomId: roomId, limit: 100 });
}

// === History Handling ===
function handleHistory(roomId, messages, isContextLoad) {
  if (roomId !== currentRoomId) {
    if (!roomMessages[roomId]) roomMessages[roomId] = [...messages].reverse();
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

  if (wasLoadingOlder && roomMessages[roomId] && roomMessages[roomId].length > 0) {
    const existing = roomMessages[roomId];
    const existingIds = new Set(existing.map(m => m.id));
    const newMsgs = [...messages].reverse().filter(m => !existingIds.has(m.id));
    roomMessages[roomId] = newMsgs.concat(existing);
    hasMoreOlder = messages.length > 0;
  } else if (wasLoadingNewer && roomMessages[roomId] && roomMessages[roomId].length > 0) {
    const existing = roomMessages[roomId];
    const existingIds = new Set(existing.map(m => m.id));
    const newMsgs = messages.filter(m => !existingIds.has(m.id));
    roomMessages[roomId] = existing.concat(newMsgs);
    hasMoreNewer = messages.length > 0;
  } else {
    roomMessages[roomId] = [...messages].reverse();
    hasMoreOlder = messages.length >= 100;
  }

  renderMessagesWithoutScroll();

  const scrollHeightAfter = container.scrollHeight;

  // Fix scroll position IMMEDIATELY (before any scroll events can fire)
  let targetScrollTop;
  if (wasLoadingOlder) {
    // Keep same view position: add the height difference to scrollTop
    targetScrollTop = scrollTopBefore + (scrollHeightAfter - scrollHeightBefore);
  } else if (wasLoadingNewer) {
    // Keep same distance from bottom (so we stay looking at the same messages)
    targetScrollTop = scrollHeightAfter - clientHeight - distanceFromBottomBefore;
    // But ensure we don't trigger another load immediately (distFromBottom should be > 100)
    const maxScrollTop = scrollHeightAfter - clientHeight - 150;
    if (targetScrollTop > maxScrollTop) {
      targetScrollTop = maxScrollTop;
    }
  } else {
    // Initial load - scroll to bottom
    targetScrollTop = scrollHeightAfter;
  }

  container.scrollTop = targetScrollTop;
  const intendedScrollTop = targetScrollTop;

  updateGoToLatestButton();

  // Reset flags
  loadingOlder = false;
  loadingNewer = false;

  // Re-enable scroll handler after a delay, verifying scroll position
  setTimeout(function() {
    // Check if browser changed our scrollTop (due to scroll anchoring)
    const actualScrollTop = container.scrollTop;
    if (Math.abs(actualScrollTop - intendedScrollTop) > 50) {
      container.scrollTop = intendedScrollTop;
    }

    // Wait one more frame then re-enable
    requestAnimationFrame(function() {
      scrollUpdateInProgress = false;
    });
  }, 50);

  send('GET_READ_STATUS', { roomId: roomId });
}

function handleMessagesAround(roomId, messages) {
  if (roomId !== currentRoomId) return;

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
    }

    // Re-enable scroll handling after verifying position
    loadingOlder = false;
    loadingNewer = false;

    setTimeout(function() {
      // Check if browser changed our scrollTop
      const actualScrollTop = container.scrollTop;
      if (Math.abs(actualScrollTop - intendedScrollTop) > 50) {
        container.scrollTop = intendedScrollTop;
      }

      requestAnimationFrame(function() {
        scrollUpdateInProgress = false;
      });
    }, 50);
  }, 100);
}

// === Message Handling ===
function handleNewMessage(msg) {
  if (!msg) return;
  if (rooms[msg.roomId]) {
    rooms[msg.roomId].lastMessage = msg;
    if (msg.roomId !== currentRoomId) rooms[msg.roomId].unreadCount = (rooms[msg.roomId].unreadCount || 0) + 1;
    renderRoomList();
  }
  if (roomMessages[msg.roomId]) {
    const exists = roomMessages[msg.roomId].some(m => m.id === msg.id);
    if (!exists) roomMessages[msg.roomId].push(msg);
  }
  if (msg.roomId === currentRoomId) {
    renderMessages();
    scrollToBottom();
    if (msg.senderId !== currentUserId) send('MARK_AS_READ', { roomId: msg.roomId, messageId: msg.id });
  }

  // Clear uploading state if this is our message
  if (msg.senderId === currentUserId && uploadingMessageId === 'pending') {
    uploadingMessageId = null;
  }
}

function handleEditedMessage(msg) {
  if (!msg || !roomMessages[msg.roomId]) return;
  const idx = roomMessages[msg.roomId].findIndex(m => m.id === msg.id);
  if (idx >= 0) {
    roomMessages[msg.roomId][idx] = msg;
    roomMessages[msg.roomId].forEach(m => {
      if (m.replyTo && m.replyTo.id === msg.id) {
        m.replyTo = { id: msg.id, senderId: msg.senderId, text: msg.text, createdAt: msg.createdAt };
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
    roomMessages[roomId][idx].text = 'This message was deleted';
    roomMessages[roomId].forEach(m => {
      if (m.replyTo && m.replyTo.id === messageId) {
        m.replyTo.text = 'This message was deleted';
      }
    });
    if (roomId === currentRoomId) renderMessages();
  }
}

// === Typing Indicators ===
function handleUserTyping(roomId, userId) {
  if (userId === currentUserId) return;
  if (!typingUsers[roomId]) typingUsers[roomId] = {};
  typingUsers[roomId][userId] = Date.now();
  if (roomId === currentRoomId) updateTypingIndicator();
}

function handleUserPaused(roomId, userId) {
  if (typingUsers[roomId]) delete typingUsers[roomId][userId];
  if (roomId === currentRoomId) updateTypingIndicator();
}

function updateTypingIndicator() {
  const indicator = document.getElementById('typing-indicator');
  const users = typingUsers[currentRoomId] || {};
  const typingUserIds = Object.keys(users).filter(id => Date.now() - users[id] < 5000);
  if (typingUserIds.length === 0) {
    indicator.classList.add('hidden');
  } else {
    const names = typingUserIds.map(id => getUserDisplayName(id).split('@')[0]);
    indicator.textContent = names.join(', ') + ' typing...';
    indicator.classList.remove('hidden');
  }
}

// === Message Rendering ===
function renderMessages() {
  renderMessagesWithoutScroll();
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

  let html = '';
  let lastDate = null;

  messages.forEach(function(msg) {
    const msgDate = formatDate(msg.createdAt);
    if (msgDate !== lastDate) {
      html += '<div class="date-separator"><span>' + msgDate + '</span></div>';
      lastDate = msgDate;
    }

    const isSent = msg.senderId === currentUserId;
    const isUploading = uploadingMessageId === msg.id;
    let msgClass = 'message ' + (isSent ? 'sent' : 'received');
    if (msg.deleted) msgClass += ' deleted';
    if (isUploading) msgClass += ' uploading';

    html += '<div class="' + msgClass + '" data-id="' + msg.id + '">';
    html += '<span class="menu-trigger" onclick="toggleMenu(\'' + msg.id + '\')">&#8942;</span>';
    html += '<div id="menu-' + msg.id + '" class="message-menu hidden">';
    if (!msg.deleted) {
      html += '<div class="message-menu-item" onclick="setReply(\'' + msg.id + '\')">Reply</div>';
      html += '<div class="message-menu-item" onclick="openForwardModal(\'' + msg.id + '\')">Forward</div>';
      html += '<div class="message-menu-item" onclick="toggleReaction(\'' + msg.id + '\', \'👍\')">👍</div>';
      html += '<div class="message-menu-item" onclick="toggleReaction(\'' + msg.id + '\', \'❤️\')">❤️</div>';
      html += '<div class="message-menu-item" onclick="toggleReaction(\'' + msg.id + '\', \'😂\')">😂</div>';
      if (isSent) {
        html += '<div class="message-menu-item" onclick="startInlineEdit(\'' + msg.id + '\')">Edit</div>';
        if (msg.attachments && msg.attachments.length > 0) {
          html += '<div class="message-menu-item" onclick="openDeleteAttachmentsModal(\'' + msg.id + '\')">Delete Attachments</div>';
        }
      }
    }
    if (isSent) html += '<div class="message-menu-item danger" onclick="deleteMessage(\'' + msg.id + '\')">Delete</div>';
    html += '</div>';

    if (msg.forwardedFrom) {
      const originalSender = getUserDisplayName(msg.forwardedFrom.senderId);
      html += '<div class="forwarded-label">Forwarded from ' + escapeHtml(originalSender) + '</div>';
    } else if (msg.forwardedFromId) {
      html += '<div class="forwarded-label">Forwarded</div>';
    }

    if (msg.replyTo) {
      html += '<div class="reply-preview">';
      html += '<div class="reply-preview-sender">' + escapeHtml(getUserDisplayName(msg.replyTo.senderId)) + '</div>';
      html += '<div class="reply-preview-text">' + escapeHtml((msg.replyTo.text || '').substring(0, 50)) + '</div>';
      html += '</div>';
    }

    if (!isSent && !msg.deleted) {
      html += '<div class="message-sender">' + escapeHtml(getUserDisplayName(msg.senderId)) + '</div>';
    }

    if (inlineEditId === msg.id) {
      html += '<div class="message-text"><input type="text" class="inline-edit-input" id="inline-edit-' + msg.id + '" value="' + escapeHtml(msg.text || '') + '" onkeydown="handleInlineEditKey(event, \'' + msg.id + '\')" onblur="cancelInlineEdit()"></div>';
    } else {
      html += '<div class="message-text">' + (msg.deleted ? 'This message was deleted' : escapeHtml(msg.text || '')) + '</div>';
    }

    // Render attachments
    if (msg.attachments && msg.attachments.length > 0 && !msg.deleted) {
      html += '<div class="message-attachments">';
      msg.attachments.forEach(function(att) {
        const icon = getFileIcon(att.mimeType);
        const size = formatFileSize(att.fileSize);
        html += '<div class="attachment-item">';
        html += '<span class="attachment-icon">' + icon + '</span>';
        html += '<div class="attachment-info">';
        html += '<div class="attachment-name">' + escapeHtml(att.fileName) + '</div>';
        html += '<div class="attachment-size">' + size + '</div>';
        html += '</div>';
        html += '<span class="attachment-download" onclick="downloadAttachment(\'' + att.id + '\', \'' + escapeHtml(att.fileName) + '\')">Download</span>';
        html += '</div>';
      });
      html += '</div>';
    }

    if (msg.reactions && Object.keys(msg.reactions).length > 0 && !msg.deleted) {
      html += '<div class="message-reactions">';
      for (const [emoji, users] of Object.entries(msg.reactions)) {
        const hasMyReaction = users.includes(currentUserId);
        html += '<span class="reaction-badge' + (hasMyReaction ? ' my-reaction' : '') + '" onclick="toggleReaction(\'' + msg.id + '\', \'' + emoji + '\')">' + emoji + ' ' + users.length + '</span>';
      }
      html += '</div>';
    }

    html += '<div class="message-meta">';
    if (msg.editedAt) html += '<span>(edited)</span>';
    html += '<span>' + formatTime(msg.createdAt) + '</span>';
    if (isSent && !msg.deleted && !isUploading) {
      const readBy = Object.entries(readStatus).filter(([uid, mid]) => uid !== currentUserId && mid === msg.id).length;
      html += '<span class="checkmarks ' + (readBy > 0 ? 'read' : 'delivered') + '">✓✓</span>';
    }
    if (isUploading) html += '<span class="upload-spinner"></span>';
    html += '</div>';
    html += '</div>';
  });

  container.innerHTML = html;
}

// === Send Message with REST Upload ===
async function sendMessage() {
  const input = document.getElementById('message-input');
  const text = input.value.trim();
  if (!text && pendingFiles.length === 0) return;
  if (!currentRoomId) return;

  const filesToUpload = [...pendingFiles];
  clearPendingFiles();
  input.value = '';

  // Show uploading state
  if (filesToUpload.length > 0) {
    uploadingMessageId = 'pending';
  }

  try {
    // Upload all files first via REST, collect attachment IDs
    const attachmentIds = [];
    let uploadFailed = false;
    for (const file of filesToUpload) {
      const params = new URLSearchParams({
        fileName: file.name,
        mimeType: file.type || 'application/octet-stream',
        fileSize: file.size
      });

      const response = await fetch(attachmentsApiBase + '/upload?' + params.toString(), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/octet-stream'
        },
        body: file,
        credentials: 'include'
      });

      if (response.ok) {
        const attachment = await response.json();
        attachmentIds.push(attachment.id);
      } else {
        console.error('Failed to upload file:', file.name, await response.text());
        uploadFailed = true;
        break; // Stop uploading on first failure
      }
    }

    // Only send message if all uploads succeeded (or no files to upload)
    if (uploadFailed) {
      uploadingMessageId = null;
      alert('Upload failed. Message not sent.');
      return;
    }

    // Now send message with attachment IDs
    const data = {
      roomId: currentRoomId,
      text: text || (attachmentIds.length > 0 ? '(attachment)' : '')
    };
    if (replyToMessage) data.replyToId = replyToMessage.id;
    if (attachmentIds.length > 0) data.attachmentIds = attachmentIds;

    send('SEND_MESSAGE', data);
    cancelReply();

    if (isTyping) {
      isTyping = false;
      send('PAUSED', { roomId: currentRoomId });
    }
  } catch (e) {
    console.error('Error sending message:', e);
    uploadingMessageId = null;
    alert('Error sending message: ' + e.message);
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

// === File Handling ===
function handleFileSelect(event) {
  const files = Array.from(event.target.files);
  if (files.length === 0) return;
  files.forEach(file => pendingFiles.push(file));
  renderPendingFiles();
  event.target.value = '';
}

function renderPendingFiles() {
  const container = document.getElementById('pending-files');
  if (pendingFiles.length === 0) {
    container.classList.add('hidden');
    return;
  }
  let html = '<div style="font-weight:500;margin-bottom:4px;">Files to attach:</div>';
  pendingFiles.forEach((file, index) => {
    const icon = getFileIcon(file.type);
    const size = formatFileSize(file.size);
    html += '<div class="pending-file">' +
      '<span>' + icon + '</span>' +
      '<span style="flex:1;">' + escapeHtml(file.name) + ' (' + size + ')</span>' +
      '<span class="pending-file-remove" onclick="removePendingFile(' + index + ')">&times;</span>' +
    '</div>';
  });
  container.innerHTML = html;
  container.classList.remove('hidden');
}

function removePendingFile(index) {
  pendingFiles.splice(index, 1);
  renderPendingFiles();
}

function clearPendingFiles() {
  pendingFiles = [];
  renderPendingFiles();
}

function getFileIcon(mimeType) {
  if (!mimeType) return '\uD83D\uDCC4';
  if (mimeType.startsWith('image/')) return '\uD83D\uDDBC\uFE0F';
  if (mimeType.startsWith('video/')) return '\uD83C\uDFAC';
  if (mimeType.startsWith('audio/')) return '\uD83C\uDFB5';
  if (mimeType.includes('pdf')) return '\uD83D\uDCC4';
  if (mimeType.includes('zip') || mimeType.includes('archive')) return '\uD83D\uDDC4\uFE0F';
  if (mimeType.includes('word') || mimeType.includes('document')) return '\uD83D\uDCC3';
  if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return '\uD83D\uDCCA';
  return '\uD83D\uDCC4';
}

function formatFileSize(bytes) {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

// === Attachment Download ===
function downloadAttachment(attachmentId, fileName) {
  const url = attachmentsApiBase + '/download/' + attachmentId;
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.style.display = 'none';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

// === Delete Attachments ===
function openDeleteAttachmentsModal(messageId) {
  closeAllMenus();
  const msg = (roomMessages[currentRoomId] || []).find(m => m.id === messageId);
  if (!msg || !msg.attachments || msg.attachments.length === 0) return;
  deleteAttachmentsMessageId = messageId;
  const list = document.getElementById('delete-attachments-list');
  let html = '';
  msg.attachments.forEach(att => {
    const icon = getFileIcon(att.mimeType);
    const size = formatFileSize(att.fileSize);
    html += '<div class="delete-attachment-item" onclick="toggleAttachmentCheckbox(\'' + att.id + '\')">' +
      '<input type="checkbox" class="attachment-checkbox" data-attachment-id="' + att.id + '">' +
      '<span class="attachment-icon">' + icon + '</span>' +
      '<div class="attachment-info">' +
      '<div class="attachment-name">' + escapeHtml(att.fileName) + '</div>' +
      '<div class="attachment-size">' + size + '</div>' +
      '</div></div>';
  });
  list.innerHTML = html;
  document.getElementById('delete-attachments-modal').classList.remove('hidden');
}

function closeDeleteAttachmentsModal() {
  document.getElementById('delete-attachments-modal').classList.add('hidden');
  deleteAttachmentsMessageId = null;
}

function toggleAttachmentCheckbox(attachmentId) {
  const checkbox = document.querySelector('[data-attachment-id="' + attachmentId + '"]');
  if (checkbox) checkbox.checked = !checkbox.checked;
}

function confirmDeleteAttachments() {
  if (!deleteAttachmentsMessageId) return;
  const selectedIds = [];
  document.querySelectorAll('.attachment-checkbox:checked').forEach(cb => {
    selectedIds.push(cb.getAttribute('data-attachment-id'));
  });
  if (selectedIds.length === 0) {
    alert('Please select at least one attachment to delete');
    return;
  }
  send('DELETE_ATTACHMENTS', { messageId: deleteAttachmentsMessageId, attachmentIds: selectedIds });
  closeDeleteAttachmentsModal();
}

// === Reply & Forward & Edit ===
function setReply(messageId) {
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
    if (room.roomId === currentRoomId) return;
    const name = getRoomDisplayName(room);
    html += '<div class="room-select-item" data-room-id="' + room.roomId + '" onclick="selectForwardRoom(\'' + room.roomId + '\')">' + escapeHtml(name) + '</div>';
  });
  list.innerHTML = html || '<p>No other rooms available</p>';
  document.getElementById('forward-modal').classList.remove('hidden');
}

function selectForwardRoom(roomId) {
  forwardTargetRoomId = roomId;
  document.querySelectorAll('.room-select-item').forEach(el => {
    el.classList.toggle('selected', el.getAttribute('data-room-id') === roomId);
  });
}

function closeForwardModal() {
  document.getElementById('forward-modal').classList.add('hidden');
  forwardMessageId = null;
  forwardTargetRoomId = null;
}

function confirmForward() {
  if (forwardMessageId && forwardTargetRoomId) {
    send('FORWARD_MESSAGE', { messageId: forwardMessageId, targetRoomId: forwardTargetRoomId });
    closeForwardModal();
  }
}

function startInlineEdit(messageId) {
  closeAllMenus();
  inlineEditId = messageId;
  renderMessagesWithoutScroll();
  setTimeout(() => {
    const input = document.getElementById('inline-edit-' + messageId);
    if (input) {
      input.focus();
      input.select();
    }
  }, 10);
}

function cancelInlineEdit() {
  if (inlineEditId) {
    inlineEditId = null;
    renderMessagesWithoutScroll();
  }
}

function handleInlineEditKey(event, messageId) {
  if (event.key === 'Enter') {
    event.preventDefault();
    const input = document.getElementById('inline-edit-' + messageId);
    const newText = input ? input.value.trim() : '';
    if (newText) {
      send('EDIT_MESSAGE', { messageId: messageId, text: newText });
    }
    inlineEditId = null;
    renderMessagesWithoutScroll();
  } else if (event.key === 'Escape') {
    event.preventDefault();
    cancelInlineEdit();
  }
}

function deleteMessage(messageId) {
  closeAllMenus();
  if (confirm('Delete this message?')) {
    send('DELETE_MESSAGE', { messageId: messageId });
  }
}

// === Menu & UI ===
function toggleMenu(messageId) {
  const menu = document.getElementById('menu-' + messageId);
  const wasOpen = !menu.classList.contains('hidden');
  closeAllMenus();
  if (!wasOpen) {
    menu.classList.remove('hidden');
    openMenuId = messageId;
  }
}

function closeAllMenus() {
  document.querySelectorAll('.message-menu').forEach(m => m.classList.add('hidden'));
  openMenuId = null;
}

function toggleReaction(messageId, emoji) {
  const msg = (roomMessages[currentRoomId] || []).find(m => m.id === messageId);
  if (msg && msg.reactions && msg.reactions[emoji] && msg.reactions[emoji].includes(currentUserId)) {
    send('REMOVE_REACTION', { messageId: messageId, reaction: emoji });
  } else {
    send('ADD_REACTION', { messageId: messageId, reaction: emoji });
  }
}

// === Scroll Handling ===
function handleScroll() {
  if (scrollUpdateInProgress || loadingOlder || loadingNewer) return;
  const container = document.getElementById('messages-container');
  const distFromTop = container.scrollTop;
  const distFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight;

  if (distFromTop < 100 && hasMoreOlder) {
    const messages = roomMessages[currentRoomId] || [];
    if (messages.length > 0) {
      loadingOlder = true;
      send('GET_HISTORY', { roomId: currentRoomId, beforeMessageId: messages[0].id, limit: 50 });
    }
  }

  if (distFromBottom < 100 && hasMoreNewer) {
    const messages = roomMessages[currentRoomId] || [];
    if (messages.length > 0) {
      loadingNewer = true;
      send('GET_HISTORY', { roomId: currentRoomId, afterMessageId: messages[messages.length - 1].id, limit: 50 });
    }
  }

  updateGoToLatestButton();
}

function updateGoToLatestButton() {
  const container = document.getElementById('messages-container');
  const distFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight;
  const btn = document.getElementById('go-to-latest-btn');
  if (distFromBottom > 200 || hasMoreNewer) {
    btn.classList.remove('hidden');
  } else {
    btn.classList.add('hidden');
  }
}

function goToLatest() {
  hasMoreNewer = false;
  send('GET_HISTORY', { roomId: currentRoomId, limit: 100 });
}

function scrollToBottom() {
  const container = document.getElementById('messages-container');
  container.scrollTop = container.scrollHeight;
}

// === Search ===
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
      html += '<div class="search-result-item" onclick="jumpToMessage(\'' + msg.id + '\')">' +
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
    jumpToMessageId = messageId;
    loadingOlder = true;
    loadingNewer = true;
    roomMessages[currentRoomId] = [];
    document.getElementById('messages-container').innerHTML = '<div class="loading">Loading...</div>';
    send('GET_MESSAGES_AROUND', { roomId: currentRoomId, messageId: messageId, limit: 50 });
  }
}

// === Test Modal ===
function openTestModal() {
  document.getElementById('test-modal').classList.remove('hidden');
  document.getElementById('test-count-input').value = '10';
  document.getElementById('test-count-input').focus();
}

function closeTestModal() {
  document.getElementById('test-modal').classList.add('hidden');
}

function confirmTestMessages() {
  const count = parseInt(document.getElementById('test-count-input').value) || 10;
  closeTestModal();
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  function randomWord() {
    const len = 3 + Math.floor(Math.random() * 10); // 3-12 chars
    let word = '';
    for (let i = 0; i < len; i++) word += chars[Math.floor(Math.random() * chars.length)];
    return word;
  }
  for (let i = 0; i < count; i++) {
    const text = randomWord() + ' ' + randomWord() + ' ' + randomWord();
    setTimeout(() => send('SEND_MESSAGE', { roomId: currentRoomId, text: text }), i * 100);
  }
}

// === New Room ===
function handleRoomCreated(data) {
  if (data.room) {
    rooms[data.room.roomId] = data.room;
    renderRoomList();
    selectRoom(data.room.roomId);
  }
  closeNewRoomModal();
}

async function openNewRoomModal() {
  selectedUsersForNewRoom = [];
  document.getElementById('user-search-input').value = '';
  document.getElementById('user-search-results').innerHTML = '<p style="color:#666;padding:8px;">Loading users...</p>';
  document.getElementById('selected-users').innerHTML = '';
  document.getElementById('new-room-name').value = '';
  document.getElementById('group-options').classList.add('hidden');
  document.getElementById('new-room-modal').classList.remove('hidden');
  document.getElementById('user-search-input').focus();
  // Fetch all users immediately
  if (allSearchableUsers.length === 0) {
    await fetchAllSearchableUsers();
  }
  renderUserSearchResults(allSearchableUsers.slice(0, 20));
}

function closeNewRoomModal() {
  document.getElementById('new-room-modal').classList.add('hidden');
  selectedUsersForNewRoom = [];
}

function searchUsers() {
  const query = document.getElementById('user-search-input').value.trim().toLowerCase();
  // Filter cached users by query (already loaded on modal open)
  const filtered = allSearchableUsers.filter(user =>
    user.email && user.email.toLowerCase().includes(query)
  );
  renderUserSearchResults(filtered.slice(0, 20));
}

async function fetchAllSearchableUsers() {
  try {
    const payload = {
      Body: {
        SearchUsersByFeatureRequest: {
          _jsns: "urn:zimbraAccount",
          name: "",
          feature: "WSC",
          offset: 0
        }
      },
      Header: {
        context: {
          _jsns: "urn:zimbra"
        }
      }
    };

    const response = await fetch(soapUrl + 'SearchUsersByFeatureRequest', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      credentials: 'include'
    });

    if (response.ok) {
      const data = await response.json();
      const accounts = data.Body?.SearchUsersByFeatureResponse?.account || [];
      allSearchableUsers = accounts.map(acc => {
        // Extract email from attributes
        const mailAttr = (acc.a || []).find(a => a.n === 'mail');
        return {
          id: acc.id,
          email: mailAttr ? mailAttr._content : acc.name
        };
      });
    }
  } catch (e) {
    console.error('Error fetching searchable users:', e);
  }
}

function renderUserSearchResults(users) {
  const container = document.getElementById('user-search-results');
  if (users.length === 0) {
    container.innerHTML = '<p style="color:#666;padding:8px;">No users found</p>';
    return;
  }
  let html = '';
  users.forEach(user => {
    if (user.id === currentUserId) return;
    if (selectedUsersForNewRoom.some(u => u.id === user.id)) return;
    html += '<div class="room-select-item" onclick="addUserToNewRoom(\'' + user.id + '\', \'' + escapeHtml(user.email || user.id) + '\')">' +
      escapeHtml(user.email || user.id) + '</div>';
  });
  container.innerHTML = html || '<p style="color:#666;padding:8px;">No more users</p>';
}

function addUserToNewRoom(userId, email) {
  if (selectedUsersForNewRoom.some(u => u.id === userId)) return;
  selectedUsersForNewRoom.push({ id: userId, email: email });
  renderSelectedUsers();
  document.getElementById('user-search-input').value = '';
  document.getElementById('user-search-results').innerHTML = '';
  document.getElementById('group-options').classList.toggle('hidden', selectedUsersForNewRoom.length < 2);
}

function removeUserFromNewRoom(userId) {
  selectedUsersForNewRoom = selectedUsersForNewRoom.filter(u => u.id !== userId);
  renderSelectedUsers();
  document.getElementById('group-options').classList.toggle('hidden', selectedUsersForNewRoom.length < 2);
}

function renderSelectedUsers() {
  const container = document.getElementById('selected-users');
  if (selectedUsersForNewRoom.length === 0) {
    container.innerHTML = '<span style="color:#999;">No users selected</span>';
    return;
  }
  let html = '';
  selectedUsersForNewRoom.forEach(user => {
    html += '<span style="background:#1976d2;color:white;padding:4px 8px;border-radius:12px;font-size:12px;display:inline-flex;align-items:center;gap:4px;">' +
      escapeHtml(user.email.split('@')[0]) +
      '<span onclick="removeUserFromNewRoom(\'' + user.id + '\')" style="cursor:pointer;font-size:14px;">&times;</span></span>';
  });
  container.innerHTML = html;
}

function confirmNewRoom() {
  if (selectedUsersForNewRoom.length === 0) {
    alert('Please select at least one user');
    return;
  }
  const memberIds = selectedUsersForNewRoom.map(u => u.id);
  const name = document.getElementById('new-room-name').value.trim();
  const roomType = selectedUsersForNewRoom.length > 1 ? 'GROUP' : 'ONE_TO_ONE';
  send('CREATE_ROOM', { memberIds: memberIds, roomName: name || null, roomType: roomType });
}

// === Utilities ===
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

// Start the app
init();
