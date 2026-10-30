# Chat Migration Guide — MongooseIM/XMPP → REST + WebSocket

Reference for client developers migrating off the MongooseIM (XMPP) chat backend.

This document maps **operations**: what you used to do over XMPP, and what you call instead.
It deliberately does not document request bodies, query parameters or response schemas — read those from the specs:

| Spec | Path | Covers |
|---|---|---|
| REST (OpenAPI 3) | `carbonio-ws-collaboration-openapi/src/main/resources/api.yaml` | Every REST operation. The JAX-RS interfaces are generated from this file, so it is always in sync with the routes. |
| WebSocket (AsyncAPI) | `carbonio-ws-collaboration-openapi/src/main/resources/asyncapi.yaml` | All WebSocket events — both the `DomainEvent` hierarchy (Room, Meeting, connection events) and the `WsEvent` hierarchy (chat, typing, presence). Java server stubs are generated from this file. |

## Which backend am I talking to?

Every REST response carries an **`X-WSC-API-VERSION`** header. `>= 2.0.0` means the REST/WebSocket backend
described here. A missing or lower version means the legacy MongooseIM backend.

Practical consequences of the new architecture:

- **All writes are REST.** The HTTP response is the acknowledgement — there is no WebSocket ack event.
- **The WebSocket is push-only for chat**, except typing (§3).
- **One socket, not two.** The legacy setup needed a second connection to MongooseIM
  at `/services/messaging/ws-xmpp`; that is gone.
- **Message ids are generated server-side.** Correlate optimistic placeholders with the `tempId`
  you send, not with a client-generated id.

---

## 1. XMPP → REST

| Old operation (XMPP) | XMPP mechanism | New operation (REST) |
|---|---|---|
| Send message | `message/groupchat` + `<markable>` | `POST /rooms/{roomId}/messages` |
| Send reply | + `<reply>` (`urn:xmpp:reply:0`) | `POST /rooms/{roomId}/messages` (`replyToId`) |
| Edit message | Fastening (`urn:xmpp:fasten:0`) + `<edit>` (`zextras:xmpp:edit:0`) | `PUT /rooms/{roomId}/messages/{messageId}/edit` |
| Delete message | Fastening + `<retract>` (`urn:esl:message-retract-by-stanza-id:0`) | `DELETE /rooms/{roomId}/messages/{messageId}` |
| Add reaction | Fastening + `<reaction>` (`zextras:xmpp:reaction:0`) | `POST /rooms/{roomId}/messages/{messageId}/reactions/{reaction}` |
| Remove reaction | same, empty `<body>` | `DELETE /rooms/{roomId}/messages/{messageId}/reactions/{reaction}` |
| Forward message | MAM fetch of the source stanza, then `POST /rooms/{roomId}/forward` | `POST /rooms/{roomId}/forward` — server-side, no client fetch |
| Load history | MAM (`urn:xmpp:mam:2`) + RSM paging | `GET /rooms/{roomId}/timeline` |
| Jump to a message | MAM `to-id` / date-bounded queries | `GET /rooms/{roomId}/timeline` (`around`) |
| Full-text search | MAM + `full-text-search` field | `GET /rooms/{roomId}/messages/search` |
| Export full history | MAM open-ended `start` | `GET /rooms/{roomId}/messages/export` |
| Mark as read | `<displayed>` (`urn:xmpp:chat-markers:0`) | `PUT /rooms/{roomId}/read` |
| Read markers of a room | IQ `esl:xmpp:smart-markers:0` | *no dedicated call needed* — see §2 |
| Pin message | IQ `urn:xmpp:pin:0` | `PUT /rooms/{roomId}/messages/{messageId}/pin` |
| Unpin message | IQ `zextras:iq:pin` + `action="delete"` | `DELETE /rooms/{roomId}/messages/{messageId}/pin` |
| Get pinned messages | IQ `zextras:iq:pin` | `GET /rooms/{roomId}/pin` |
| Load inbox | IQ `erlang-solutions.com:xmpp:inbox:0` | `GET /inbox` |
| Last activity of a user | IQ `jabber:iq:last`, one call per JID | `POST /presence/batch`, one call for N users |

Notes:

- **Pin used two namespaces in the legacy backend** (`urn:xmpp:pin:0` to create,
  `zextras:iq:pin` to read/delete). That inconsistency is gone — one REST resource.
- `GET /rooms/{roomId}/messages` also exists (cursor pagination). `GET /rooms/{roomId}/timeline`
  is the richer endpoint and the recommended one: it returns messages and system events in one
  discriminated list, supports `before`/`after`/`around` anchoring, and includes read markers.
- Both `POST /rooms/{roomId}/forward` (multi-target) and
  `POST /rooms/{roomId}/messages/{messageId}/forward` (single) exist. The former is the one in general use.

## 2. Data that used to need its own request

Two things that were separate XMPP round-trips now arrive inside responses you already make:

- **Read markers** ride on `GET /rooms/{roomId}/timeline` (`markers`) and on each `GET /inbox` entry (`markers`).
  `GET /rooms/{roomId}/read` exists if you need them standalone.
- **Member presence** rides on each `GET /inbox` entry (`room.members[].online` / `.lastActivity`).
  `POST /presence/batch` exists for arbitrary user sets.

## 3. Operations that stay on WebSocket

Not everything became REST. Ephemeral, high-frequency signals stay on the socket.

| Operation | Old (XMPP) | New (WebSocket) | Direction |
|---|---|---|---|
| Typing started | `<composing>` chatstate | action `Typing`, `status: "started"` | client → server |
| Typing stopped | `<paused>` chatstate | action `Typing`, `status: "stopped"` | client → server |
| Typing notification | `<composing>` stanza | event `Typing` | server → client |
| Presence announce | `<presence/>` stanza | *implicit* — opening the socket announces you | client → server |
| Presence offline | `<presence type="unavailable"/>` | *implicit* — closing your last socket (no grace period) | client → server |
| Presence notification | `<presence>` stanza | event `PresenceChanged` | server → client |
| Keepalive | IQ `urn:xmpp:ping` | action `ping` → event `Pong`, plus native WS ping/pong frames | bidirectional |

**Presence is asymmetric — worth knowing:**

- There is **no outbound presence action**. You never subscribe or announce; the socket lifecycle does it.
- Initial state comes from REST (`GET /inbox`, or `POST /presence/batch`, at most 100 user ids per request).
- Live changes come from the `PresenceChanged` event. Events, `/presence/batch` and `/inbox` all carry `online` and `lastActivity` (last time the user went offline, also while online); the client decides to show `lastActivity` only when offline.
- Transition events are best-effort: on every `WebsocketConnected` (first connect and reconnects) reload presence from REST; it is sent only once the socket is subscribed, so nothing published after it is missed.

Typing, by contrast, is WebSocket in both directions.

## 4. WebSocket events (server → client)

### Connection and session model

- **Endpoint:** `ws://<host>/events`
- **Sub-protocol negotiation:** declare the application version as the requested sub-protocol
  (e.g. `Sec-WebSocket-Protocol: 2.0.0`). Clients that omit it are treated as version `1.6.0`
  and receive downgraded event names (`WebsocketConnected` → `websocketConnected`;
  Room/Meeting events to `SCREAMING_SNAKE_CASE`) — always declare your version. A handshake that
  offers only unsupported versions is rejected with `400`.
- **Authentication:** the HTTP session must be authenticated before the WebSocket handshake;
  unauthenticated connections are rejected.
- **Keep-alive:** the server sends a native WebSocket PING frame every 30 seconds;
  standard libraries handle PONG automatically.

On connect the server sends `WebsocketConnected` as soon as the socket is subscribed to events,
carrying the `queueId` the client must pass as the `queue-id` HTTP header in all meeting REST calls.

### Common envelope

Every event is a JSON object with at least:

| Field | Type | Description |
|---|---|---|
| `type` | string | Event discriminator (PascalCase, unless the client did not declare a sub-protocol version — see above). |
| `sentDate` | string (ISO 8601) | UTC timestamp when the event was generated. |

**Exact payload schemas are defined in and generated from
`carbonio-ws-collaboration-openapi/src/main/resources/asyncapi.yaml`** — that file is the single
source of truth for both the `DomainEvent` and `WsEvent` hierarchies. Do not rely on schema copies elsewhere.

### Per-event catalog

**Connection events** (direct WebSocket, no message broker):

| Event | Emitted when | Audience |
|---|---|---|
| `WebsocketConnected` | Connection is established and subscribed to events | The connecting client only |
| `MessageBrokerDisconnected` | Server loses its RabbitMQ connection; meeting API calls that require an active session return `403` while this persists | All connected clients |
| `MessageBrokerRestored` | RabbitMQ connection is restored; meeting API calls become available again | All connected clients |
| `Pong` | Client sends a legacy `ping` message (prefer native WS PING frames) | The requesting client only |

**Room events** (delivered via message broker to each member's personal queue):

| Event | Emitted when | Audience |
|---|---|---|
| `RoomCreated` | A new room is created | All room members |
| `RoomUpdated` | Room name or description changes | All room members |
| `RoomDeleted` | Room is deleted | All room members |
| `RoomMemberAdded` | A user is added to a room | All room members |
| `RoomMemberRemoved` | A user is removed from a room | All room members |
| `RoomOwnerPromoted` | A member gains owner rights | All room members |
| `RoomOwnerDemoted` | A member loses owner rights | All room members |
| `RoomMuted` | A member mutes room notifications | The member who muted |
| `RoomUnmuted` | A member unmutes room notifications | The member who unmuted |
| `RoomHistoryCleared` | Room message history is cleared | All room members |
| `RoomPictureChanged` | Room avatar is updated (use `updatedAt` to invalidate caches) | All room members |
| `RoomPictureDeleted` | Room avatar is removed | All room members |

**Meeting events** (delivered via message broker to each participant's personal queue):

| Event | Emitted when | Audience |
|---|---|---|
| `MeetingCreated` | A meeting is created for a room | All room members |
| `MeetingDeleted` | A meeting is deleted | All room members |
| `MeetingStarted` | The first participant joins (meeting goes live) | All room members |
| `MeetingStopped` | The last participant leaves (meeting ends) | All meeting participants |
| `MeetingDeclined` | A user declines the meeting invitation | All meeting participants |
| `MeetingParticipantJoined` | A participant joins the active meeting | All meeting participants |
| `MeetingParticipantLeft` | A participant leaves the active meeting, or a moderator kicks them out (`reason`: `LEFT` or `KICKED`) | All room members, the kicked user included |
| `MeetingParticipantClashed` | A second session of the same user joins (first session is kicked) | The displaced session only |
| `MeetingAudioStreamChanged` | A participant's audio is muted/unmuted (by self or moderator) | All meeting participants |
| `MeetingAudioAnswered` | WebRTC answer for the audio PeerConnection is ready | The requesting participant only |
| `MeetingMediaStreamChanged` | A participant's video or screen stream toggles on or off | All meeting participants |
| `MeetingSdpOffered` | Video server sends a new SDP offer for renegotiation | The relevant participant |
| `MeetingSdpAnswered` | WebRTC answer for a media PeerConnection is ready | The requesting participant only |
| `MeetingParticipantTalking` | Video server detects a change in talking activity | All meeting participants |
| `MeetingParticipantSubscribed` | Video server confirms stream subscriptions for a participant | The subscribing participant only |
| `MeetingRecordingStarted` | Recording begins | All meeting participants |
| `MeetingRecordingStopped` | Recording ends | All meeting participants |
| `MeetingParticipantHandRaised` | A hand is raised or lowered (by participant or moderator) | All meeting participants |
| `MeetingParticipantHandRaisedList` | A participant joins a meeting that already has hands raised (initial state sync) | The joining participant only |

**Chat and presence events** (`WsEvent` family):

| Event | Emitted when | Audience |
|---|---|---|
| `MessageReceived` | `POST /rooms/{roomId}/messages` or attachment upload completes | All room members; **sender also receives a `tempId` echo** (see below) |
| `MessageForwarded` | `POST /rooms/{roomId}/forward` | All members of the target room |
| `MessageEdited` | `PUT .../messages/{messageId}/edit` | All room members |
| `MessageDeleted` | `DELETE .../messages/{messageId}` | All room members |
| `MessagePinned` | `PUT .../messages/{messageId}/pin` | All room members |
| `MessageUnpinned` | `DELETE .../messages/{messageId}/pin` of the room pin (unpinning any other message is a 204 no-op: no system line, no event), or `DELETE .../messages/{messageId}` of the pinned message (sent right after `MessageDeleted`, `unpinnedBy` = the deleter) | All room members |
| `ReactionChanged` | `POST` or `DELETE .../reactions/{reaction}` | All room members |
| `ReadUpdated` | `PUT /rooms/{roomId}/read` that moves the marker forward (a same or older item writes nothing and sends nothing) | All room members |
| `PresenceChanged` | A user opens their first or closes their last WebSocket, across all instances | Online users sharing at least one room with the affected user |
| `Typing` | Client sends a `Typing` WebSocket action | All room members except the sender |

### Chat message event details

`MessageReceived` and `MessageForwarded` carry a nested `message` object of the `TimelineMessage`
shape (full message payload including id, text, attachments, reply chain, etc.).

`MessageReceived` additionally carries a top-level `tempId` field that is echoed **only to the sender**
and is omitted for all other recipients. Use `tempId` to reconcile optimistic-UI placeholders:
match the `tempId` you sent in the REST request to the `tempId` in the event, then replace the
placeholder with the real message. Do not use a client-generated id — message ids are server-assigned.

Delta events (`MessageEdited`, `MessageDeleted`) are intentionally thin: they reference the target
message via `messageId` (a foreign key) and carry only the changed fields — `text` and `editedAt`
for edits; `deletedBy` and `deletedAt` for deletes. Clients update their local copy in place
rather than re-fetching.

`MessagePinned`, `MessageUnpinned`, and `ReactionChanged` likewise identify the message by `messageId`
and carry only the delta (pin actor/timestamp, or reaction emoji + operation).

**Events that create a system line.** Some actions persist a system event (a `chats.system_event`
row, exposed as `SystemEvent{id, actorId, ...}` in the timeline) and announce it through the events
below. Each carries the author and `systemEventId`, the id of the row created by the same action
(omitted when the action creates none, e.g. meetings in GROUP rooms). The author always equals the
row's `actorId`, and `systemEventId` is the id to use in `GET /rooms/{roomId}/timeline` and in
`PUT /rooms/{roomId}/read` `{itemId}`, so a live line reconciles with the timeline row and can be
counted as unread when the author is not the current user. The row is committed before the event is
dispatched.

Next to every `systemEventId` (and `nameSystemEventId` / `descriptionSystemEventId`) the event carries
`systemEventCreatedAt` (`nameSystemEventCreatedAt` / `descriptionSystemEventCreatedAt`), the row's
`createdAt`: the same instant as the timeline item's `createdAt` and as the `itemCreatedAt` that
`PUT /rooms/{roomId}/read` with that id stores. Date a live line with it, never with `sentDate`
(later): a line dated by `sentDate` stays after the read marker and looks unread forever. It is
present exactly when the id is. Compare it as an instant; the offset may differ from the timeline's.

| Event | System line | Author field | `systemEventId` |
|---|---|---|---|
| `MessagePinned` | `MESSAGE_PINNED` | `pinnedBy` | always |
| `MessageUnpinned` | `MESSAGE_UNPINNED` | `unpinnedBy` | always |
| `RoomMemberAdded` | `MEMBER_ADDED` | `addedBy` | always |
| `RoomMemberRemoved` | `MEMBER_REMOVED` | `removedBy` (the leaving user on self-leave; the first room owner when the removal is automatic, i.e. temporary-room clean-up, as the release did) | always |
| `MeetingStarted` | `MEETING_STARTED` | `starterUser` | ONE_TO_ONE rooms only |
| `MeetingStopped` | `MEETING_ENDED` | `endedBy` (the user whose action stopped the meeting) | ONE_TO_ONE rooms only, and only if the meeting had started |
| `MeetingDeclined` | `MEETING_DECLINED` | `userId` (the decliner) | ONE_TO_ONE rooms only |

Room changes write native system events too. All five carry the current user as `actorId`, and are
returned by the timeline like the others (`content` is the parsed JSON object):

| System line | Written when | `content` |
|---|---|---|
| `ROOM_NAME_CHANGED` | `PUT /rooms/{roomId}`, the name actually changed | `{"changedBy": <user>, "name": <new name>}` |
| `ROOM_DESCRIPTION_CHANGED` | `PUT /rooms/{roomId}`, the description actually changed (also to `""`) | `{"changedBy": <user>, "description": <new description>}` |
| `ROOM_PICTURE_UPDATED` | `PUT /rooms/{roomId}/picture` | `{"updatedBy": <user>}` |
| `ROOM_PICTURE_DELETED` | `DELETE /rooms/{roomId}/picture` | `{"deletedBy": <user>}` |
| `ROOM_HISTORY_CLEARED` | `PUT /rooms/{roomId}/clear` on a TEMPORARY room (the owner clears for everyone) | `{"clearedBy": <user>}` |

An update that changes nothing writes no event. If name and description change in one request, two
rows are written, name first, with `createdAt` 1 ms apart. `ROOM_HISTORY_CLEARED` is written after the
purge, so it is the only row left in the room. The per-user clear of GROUP and ONE_TO_ONE rooms
(`cleared_at`) writes no event.

| Event | System line | Author field | `systemEventId` |
|---|---|---|---|
| `RoomUpdated` | `ROOM_NAME_CHANGED`, `ROOM_DESCRIPTION_CHANGED` | `updatedBy` (always) | `nameSystemEventId` only if the name changed, `descriptionSystemEventId` only if the description changed |
| `RoomPictureChanged` | `ROOM_PICTURE_UPDATED` | `updatedBy` (always) | `systemEventId`, always |
| `RoomPictureDeleted` | `ROOM_PICTURE_DELETED` | `deletedBy` (always) | `systemEventId`, always |
| `RoomHistoryCleared` | `ROOM_HISTORY_CLEARED` | `clearedBy` | `systemEventId`; both present only for TEMPORARY rooms, absent for the per-user clear |

A member add writes one `MEMBER_ADDED` row per added user (`addedUserIds` holds that single user;
the rows of one request are 1 ms apart, in request order) and sends one `RoomMemberAdded` per user,
each carrying the `systemEventId`/`systemEventCreatedAt` of its own row, to every member of the room
after the add (the added users included). A user listed twice is added once; an empty list is
rejected with 400. Room creation lines are not announced through these events.

`MessageSent` (the legacy XMPP-era name) is removed; `MessageReceived` replaces it for both
send and attachment-upload notifications.

**Ordering.** Every event carries a database timestamp. The broker preserves FIFO per session,
but clients should still insert by `(createdAt, id)` — that matches the server's own ordering
(`ORDER BY createdAt, id`).

## 5. WebSocket messages (client → server)

Only two inbound messages are handled: `Typing` (see §3) and `ping`. Anything else gets an `Error`
frame. Message writes are REST-only — there is no WebSocket send/edit/delete/forward.

## 6. Operations whose path did not change

These were already plain REST and never touched XMPP. Same method, same path, same behaviour.

| Domain | Operations |
|---|---|
| Rooms | `listRooms`, `insertRoom`, `getRoom`, `updateRoom`, `deleteRoom`, `getRoomPicture`, `updateRoomPicture`, `deleteRoomPicture`, `muteRoom`, `unmuteRoom`, `clearRoomHistory`, `getMeetingByRoomId` |
| Members | `listRoomMembers`, `insertRoomMembers`, `deleteRoomMember`, `insertOwner`, `deleteOwner`, `updateRoomOwners` |
| Attachments | `listRoomAttachmentsInfo`, `insertAttachmentMultipart`, `bulkDeleteRoomAttachments`, `deleteAttachment`, `getAttachmentInfo`, `getAttachment` |
| Preview | `getImagePreview`, `getImageThumbnail`, `getPdfPreview`, `getPdfThumbnail`, `getVideoPreview`, `getVideoThumbnail` |
| Meetings | `listMeeting`, `createMeeting`, `getMeeting`, `deleteMeeting`, `startMeeting`, `stopMeeting`, `joinMeeting`, `leaveMeeting`, `kickMeetingParticipant`, `declineMeeting` |
| Meeting media / RTC | `updateMediaStream`, `answerRtcMediaStream`, `updateSubscriptionsMediaStream`, `updateAudioStream`, `offerRtcAudioStream`, `iceRestartAudio`, `iceRestartVideo`, `iceRestartScreen`, `updateHandStatus` |
| Users / info | `getUsers`, `getUser`, `getCapabilities`, `getToken` |
| Health | `getHealthStatus`, `isLive`, `isReady` |

These keep their method and path; the rooms, members and meetings operations now also write the system
events listed in §4. Room and Meeting WebSocket events were already delivered over this socket, never over
XMPP; several of them now carry extra fields (the author and the `systemEventId`/`systemEventCreatedAt` of
the system line, see §4).

Attachment upload changed:

- `insertAttachmentMultipart` is `POST /rooms/{roomId}/attachments` (it was `PUT`) with the form fields
  `file`, `contentLength` (required), `description`, `replyToId`, `area` and `tempId`. The message id is
  generated by the server. It answers `201` with the created `Message`, and `404` when `replyToId` is not a
  message of the room visible to the caller. The notification is a `MessageReceived` event (it was an
  `attachmentAdded` stanza).
- The `application/octet-stream` upload (`insertAttachment`, file metadata in headers) was removed.

## 7. Removed, with no replacement

| Old operation | Notes |
|---|---|
| Roster / contact list (IQ `jabber:iq:roster`) | The roster concept is gone. There is no contacts endpoint. Presence no longer depends on mutual subscriptions — it follows shared rooms. |
| Mutual subscription on 1:1 room creation | Was a server-side roster side effect; no equivalent and none needed. |
| Affiliation messages (`urn:xmpp:muclight:0#affiliations`) | Were already superseded by configuration messages; now `RoomMemberAdded` / `RoomMemberRemoved`. |

Room configuration messages (`roomNameChanged`, `roomPictureUpdated`, `memberAdded`, …) were XMPP
notifications, not operations — they are now the corresponding `Room*` events in §4.
