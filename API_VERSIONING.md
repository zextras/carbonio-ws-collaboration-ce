# API & WebSocket Versioning Log

This document tracks internal changes related to API versioning, both for the REST API and the WebSocket.

---

## Version 1.6.14

### Changes (1.6.14)

- **API**: Extended GET `/rooms/{roomId}/attachments` with a new optional `mimeTypeCategory`
  query parameter (enum: `MEDIA` | `DOCUMENTS`). `MEDIA` filters image and video attachments
  (`image/`, `video/` MIME type prefixes); `DOCUMENTS` filters every other MIME type (including
  audio). The parameter is mutually exclusive with the existing free-text `mimeType` filter:
  supplying both in the same request returns `400 Bad Request`.
- **API**: Extended the GET `/rooms/{roomId}/attachments` response with a new `total` field — the
  total number of attachments matching the requested room and filters, excluding cursor pagination.
- **API**: Added GET `/preview/video/{fileId}/{area}/` endpoint — returns a preview (the video's
  first frame, as an image) of the video identified by `fileId`, scaled to `area` (`width x height`, e.g. `320x240`).
  Optional query parameters mirror the image preview endpoint: `quality` (image quality enum), `output_format` (output
  image format enum) and `crop` (boolean, default `false`)
- **API**: Added GET `/preview/video/{fileId}/{area}/thumbnail/` endpoint — returns a thumbnail (the
  video's first frame, as an image) of the video identified by `fileId`, scaled to `area` (`width x height`, e.g.
  `320x240`). Optional query parameters mirror the image preview endpoint: `quality` (image quality enum),
  `output_format` (output image format enum) and `crop` (boolean, default `false`).

---

## Version 1.6.13

### Changes (1.6.13)

- **API**: Added PUT `/meetings/${meetingId}/screen/iceRestart` endpoint — triggers WebRTC
  ICE restart for the current user's screen-share (publisher) stream. Mirrors the existing
  video ICE restart; the inbound side is already covered by the video ICE restart endpoint.

---

## Version 1.6.12

### Changes (1.6.12)

- **API**: Extended GET `/rooms/{roomId}/attachments` with 8 new optional query parameters:
  - `userId` (UUID) — filter attachments by uploader
  - `mimeType` (string) — filter by exact MIME type or prefix (e.g. `image/`)
  - `timestampAfter` (date-time) — filter attachments created strictly after this timestamp
  - `timestampBefore` (date-time) — filter attachments created strictly before this timestamp
  - `minSize` (int64) — filter attachments with size ≥ N bytes
  - `maxSize` (int64) — filter attachments with size ≤ N bytes
  - `orderBy` (enum: `createdAt` | `size`, default `createdAt`) — field to sort by;
      switching to `size` changes the keyset pagination cursor
  - `orderDirection` (enum: `asc` | `desc`, default `desc`) — sort direction
- **API**: Added DELETE `/attachments` endpoint — bulk deletes a list of attachments by their identifiers.
  The request body is `{ "attachmentIds": ["uuid", ...] }` (min 1 item). The requesting user must own
  each attachment or be a room owner of the attachment's room. Guests are forbidden.

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
