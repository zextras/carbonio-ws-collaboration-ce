# API & WebSocket Versioning Log

This document tracks internal changes related to API versioning, both for the REST API and the WebSocket.

---

## Version 1.6.11

### Changes (1.6.11)

- **WebSocket**: Added `MessageBrokerDisconnected` event — emitted when the backend loses the
  connection to the message broker, so clients can surface a degraded-state indicator.
- **WebSocket**: Added `MessageBrokerRestored` event — emitted when the backend reconnects to
  the message broker, so clients can clear the degraded-state indicator.

---

## Version 1.6.10

### Changes (1.6.10)

- **API**: Added POST `/meetings/{meetingId}/decline` endpoint — allows the current user to decline
  an incoming meeting invitation. Returns 204 on success, 404 if the meeting is not found.
- **XMPP**: Backend now sends a `meetingStarted` stanza to the room when a one-to-one meeting
  starts, so clients can update the chat history accordingly.
- **XMPP**: Backend now sends a `meetingEnded` stanza to the room when a one-to-one meeting
  ends, so clients can update the chat history accordingly.
- **XMPP**: Backend now sends a `meetingDeclined` stanza to the room when a user declines a
  meeting invitation, so other participants are notified via the chat history.

---

## Version 1.6.7 (Released with Carbonio 26.3.0)

### Changes (1.6.7)

- **API**: Updated PUT `/rooms/{roomId}/clear` endpoint to clear all messages and attachments of
  a temporary room.

## Version 1.6.6 (Released with Carbonio 26.3.0)

### Changes (1.6.6)

- **API**: Added PUT `/meetings/${meetingId}/audio/iceRestart` endpoint
- **API**: Added PUT `/meetings/${meetingId}/video/iceRestart` endpoint

## Version 1.6.3 (Released with Carbonio 25.12.0)

### Changes (1.6.3)

- **API**: `meetings/${meetingId}/stop` endpoint also take care of removing non-moderators from the room
  when the meeting ends letting clients call only `meetings/${meetingId}/leave` for temporary rooms.

## Version 1.6.2 (Released with Carbonio 25.9.0)

### Changes (1.6.2)

- **WebSocket**: Renamed events' type

## Version 1.6.1 (Released with Carbonio 25.9.0)

### Changes (1.6.1)

- **API**: Added PUT `rooms/${roomId}/attachments` endpoint

## Version 1.6.0 (Released with Carbonio 25.9.0)

### Start versioning
