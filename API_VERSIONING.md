# API & WebSocket Versioning Log

This document tracks internal changes related to API versioning, both for the REST API and the WebSocket.

---

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
