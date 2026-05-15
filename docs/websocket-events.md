# WebSocket events

The `/events` WebSocket endpoint is used to deliver real-time events to connected clients and to
receive a small set of control messages from them.

## Connection

- **Endpoint:** `ws://<host>/events`
- **Sub-protocol negotiation:** the client should declare the application version as the requested
  sub-protocol (e.g. `Sec-WebSocket-Protocol: 1.8.0`). Clients that do not declare a sub-protocol
  are treated as version `1.6.0` for backwards compatibility.
- **Authentication:** the HTTP session must be authenticated before the WebSocket handshake. The
  server rejects unauthenticated connections.
- **Keep-alive:** the server sends a native WebSocket PING frame every 30 seconds. Clients must
  reply with a PONG frame (handled automatically by all standard WebSocket libraries).

Once the connection is established the server immediately sends a `WebsocketConnected` event
containing the `queueId` that the client must pass as the `queue-id` header in subsequent REST API
calls related to meetings.

---

## Common fields

Every event message is a JSON object that includes at least:

| Field      | Type              | Description                                                                    |
| ---------- | ----------------- | ------------------------------------------------------------------------------ |
| `type`     | string            | Event discriminator (values listed below)                                      |
| `sentDate` | string (ISO 8601) | UTC timestamp when the event was generated, e.g. `"2025-05-11T10:30:00+00:00"` |

---

## Server → Client messages

### WebsocketConnected

Sent immediately after the WebSocket connection is opened. The `queueId` must be passed as the
`queue-id` header in all REST meeting API calls that require an active session.

```json
{
  "type": "WebsocketConnected",
  "sentDate": "[1]",
  "queueId": "[2]"
}
```

Where:

1. timestamp when the connection was accepted
2. UUID identifying this WebSocket session — pass it as the `queue-id` HTTP header

---

### MessageBrokerDisconnected

Broadcast to all connected clients when the server loses its connection to the message broker
(RabbitMQ). While this condition persists, meeting-related API calls that require an active session
will return `403`. No fields beyond the base ones.

```json
{
  "type": "MessageBrokerDisconnected",
  "sentDate": "[1]"
}
```

Where:

1. timestamp when the disconnection was detected

---

### MessageBrokerRestored

Broadcast to all connected clients when the message broker connection is restored. After this event
meeting-related API calls become available again.

```json
{
  "type": "MessageBrokerRestored",
  "sentDate": "[1]"
}
```

Where:

1. timestamp when the connection was restored

---

### Pong

Sent in response to a client-side `ping` message (see [Client → Server](#client--server-messages)).
This is a legacy mechanism; WebSocket native PING/PONG frames should be preferred.

```json
{
  "type": "Pong",
  "sentDate": "[1]"
}
```

Where:

1. timestamp when the pong was generated

---

## Room events (via message broker)

These events are delivered through the message broker to each room member's personal queue.

### RoomCreated

```json
{
  "type": "RoomCreated",
  "sentDate": "[1]",
  "roomId": "[2]"
}
```

Where:

1. timestamp when the room was created
2. UUID of the new room

---

### RoomUpdated

```json
{
  "type": "RoomUpdated",
  "sentDate": "[1]",
  "roomId": "[2]",
  "name": "[3]",
  "description": "[4]"
}
```

Where:

1. timestamp of the update
2. UUID of the room
3. new room name
4. new room description

---

### RoomDeleted

```json
{
  "type": "RoomDeleted",
  "sentDate": "[1]",
  "roomId": "[2]"
}
```

Where:

1. timestamp when the room was deleted
2. UUID of the deleted room

---

### RoomMemberAdded

```json
{
  "type": "RoomMemberAdded",
  "sentDate": "[1]",
  "roomId": "[2]",
  "userId": "[3]",
  "isOwner": "[4]"
}
```

Where:

1. timestamp of the addition
2. UUID of the room
3. UUID of the added user
4. `true` if the user was added as owner, `false` otherwise

---

### RoomMemberRemoved

```json
{
  "type": "RoomMemberRemoved",
  "sentDate": "[1]",
  "roomId": "[2]",
  "userId": "[3]"
}
```

Where:

1. timestamp of the removal
2. UUID of the room
3. UUID of the removed user

---

### RoomOwnerPromoted

```json
{
  "type": "RoomOwnerPromoted",
  "sentDate": "[1]",
  "roomId": "[2]",
  "userId": "[3]"
}
```

Where:

1. timestamp of the promotion
2. UUID of the room
3. UUID of the user who became owner

---

### RoomOwnerDemoted

```json
{
  "type": "RoomOwnerDemoted",
  "sentDate": "[1]",
  "roomId": "[2]",
  "userId": "[3]"
}
```

Where:

1. timestamp of the demotion
2. UUID of the room
3. UUID of the user who lost owner rights

---

### RoomMuted

```json
{
  "type": "RoomMuted",
  "sentDate": "[1]",
  "roomId": "[2]"
}
```

Where:

1. timestamp when the room was muted
2. UUID of the room

---

### RoomUnmuted

```json
{
  "type": "RoomUnmuted",
  "sentDate": "[1]",
  "roomId": "[2]"
}
```

Where:

1. timestamp when the room was unmuted
2. UUID of the room

---

### RoomHistoryCleared

```json
{
  "type": "RoomHistoryCleared",
  "sentDate": "[1]",
  "roomId": "[2]",
  "clearedAt": "[3]"
}
```

Where:

1. timestamp when the event was generated
2. UUID of the room
3. timestamp when the history was cleared

---

### RoomPictureChanged

```json
{
  "type": "RoomPictureChanged",
  "sentDate": "[1]",
  "roomId": "[2]",
  "updatedAt": "[3]"
}
```

Where:

1. timestamp when the event was generated
2. UUID of the room
3. timestamp when the picture was updated (use to invalidate caches)

---

### RoomPictureDeleted

```json
{
  "type": "RoomPictureDeleted",
  "sentDate": "[1]",
  "roomId": "[2]"
}
```

Where:

1. timestamp when the picture was deleted
2. UUID of the room

---

## Meeting events (via message broker)

### MeetingCreated

```json
{
  "type": "MeetingCreated",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "roomId": "[3]"
}
```

Where:

1. timestamp when the meeting was created
2. UUID of the new meeting
3. UUID of the room the meeting belongs to

---

### MeetingDeleted

```json
{
  "type": "MeetingDeleted",
  "sentDate": "[1]",
  "meetingId": "[2]"
}
```

Where:

1. timestamp when the meeting was deleted
2. UUID of the deleted meeting

---

### MeetingStarted

```json
{
  "type": "MeetingStarted",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "starterUser": "[3]",
  "startedAt": "[4]"
}
```

Where:

1. timestamp when the event was generated
2. UUID of the meeting
3. UUID of the user who started the meeting
4. timestamp when the meeting started

---

### MeetingStopped

```json
{
  "type": "MeetingStopped",
  "sentDate": "[1]",
  "meetingId": "[2]"
}
```

Where:

1. timestamp when the meeting was stopped
2. UUID of the meeting

---

### MeetingDeclined

```json
{
  "type": "MeetingDeclined",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]"
}
```

Where:

1. timestamp when the meeting was declined
2. UUID of the meeting
3. UUID of the user who declined the invitation

---

### MeetingParticipantJoined

```json
{
  "type": "MeetingParticipantJoined",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]"
}
```

Where:

1. timestamp when the participant joined
2. UUID of the meeting
3. UUID of the participant

---

### MeetingParticipantLeft

```json
{
  "type": "MeetingParticipantLeft",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]"
}
```

Where:

1. timestamp when the participant left
2. UUID of the meeting
3. UUID of the participant

---

### MeetingParticipantClashed

Sent to a user when they join a meeting but another session of the same user is already
participating. The previous session is kicked.

```json
{
  "type": "MeetingParticipantClashed",
  "sentDate": "[1]",
  "meetingId": "[2]"
}
```

Where:

1. timestamp when the clash was detected
2. UUID of the meeting

---

### MeetingAudioStreamChanged

```json
{
  "type": "MeetingAudioStreamChanged",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]",
  "moderatorId": "[4]",
  "active": "[5]"
}
```

Where:

1. timestamp of the change
2. UUID of the meeting
3. UUID of the participant whose audio changed
4. UUID of the moderator who forced the change, or `null` if the participant changed their own audio
5. `true` if audio is now active, `false` if muted

---

### MeetingAudioAnswered

Sent when the WebRTC answer for the audio PeerConnection is available.

```json
{
  "type": "MeetingAudioAnswered",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]",
  "sdp": "[4]"
}
```

Where:

1. timestamp when the answer was generated
2. UUID of the meeting
3. UUID of the participant
4. SDP answer string

---

### MeetingMediaStreamChanged

```json
{
  "type": "MeetingMediaStreamChanged",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]",
  "mediaType": "[4]",
  "active": "[5]"
}
```

Where:

1. timestamp of the change
2. UUID of the meeting
3. UUID of the participant
4. media type: `VIDEO` or `SCREEN`
5. `true` if the stream is now active, `false` otherwise

---

### MeetingSdpOffered

Sent when the video server sends a new SDP offer for renegotiation.

```json
{
  "type": "MeetingSdpOffered",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]",
  "mediaType": "[4]",
  "sdp": "[5]"
}
```

Where:

1. timestamp when the offer was generated
2. UUID of the meeting
3. UUID of the participant
4. media type: `AUDIO`, `VIDEO`, or `SCREEN`
5. SDP offer string

---

### MeetingSdpAnswered

Sent when the WebRTC answer for a media PeerConnection is available.

```json
{
  "type": "MeetingSdpAnswered",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]",
  "mediaType": "[4]",
  "sdp": "[5]"
}
```

Where:

1. timestamp when the answer was generated
2. UUID of the meeting
3. UUID of the participant
4. media type: `AUDIO`, `VIDEO`, or `SCREEN`
5. SDP answer string

---

### MeetingParticipantTalking

Sent when the video server detects a change in the participant's talking activity.

```json
{
  "type": "MeetingParticipantTalking",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]",
  "isTalking": "[4]"
}
```

Where:

1. timestamp of the detection
2. UUID of the meeting
3. UUID of the participant
4. `true` if the participant started talking, `false` if they stopped

---

### MeetingParticipantSubscribed

Sent when the video server confirms the stream subscriptions for a participant.

```json
{
  "type": "MeetingParticipantSubscribed",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]",
  "streams": "[4]"
}
```

Where:

1. timestamp when the subscriptions were confirmed
2. UUID of the meeting
3. UUID of the participant
4. array of subscribed stream objects, each with:
   - `type` (string) — stream type
   - `userId` (UUID) — UUID of the stream owner
   - `mid` (string) — media ID assigned by the video server

---

### MeetingRecordingStarted

```json
{
  "type": "MeetingRecordingStarted",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]"
}
```

Where:

1. timestamp when the recording started
2. UUID of the meeting
3. UUID of the user who started the recording

---

### MeetingRecordingStopped

```json
{
  "type": "MeetingRecordingStopped",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]"
}
```

Where:

1. timestamp when the recording stopped
2. UUID of the meeting
3. UUID of the user who stopped the recording

---

### MeetingParticipantHandRaised

```json
{
  "type": "MeetingParticipantHandRaised",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "userId": "[3]",
  "moderatorId": "[4]",
  "raised": "[5]",
  "handRaisedAt": "[6]"
}
```

Where:

1. timestamp when the event was generated
2. UUID of the meeting
3. UUID of the participant
4. UUID of the moderator who lowered the hand, or `null` if the participant acted on their own
5. `true` if the hand was raised, `false` if lowered
6. timestamp when the hand was raised, or `null` if it was lowered

---

### MeetingParticipantHandRaisedList

Sent to a participant who joins a meeting that already has hands raised, to synchronise the initial state.

```json
{
  "type": "MeetingParticipantHandRaisedList",
  "sentDate": "[1]",
  "meetingId": "[2]",
  "participants": "[3]"
}
```

Where:

1. timestamp when the event was generated
2. UUID of the meeting
3. array of UUIDs of participants who currently have their hand raised

---

## Client → Server messages

### ping (deprecated)

Legacy application-level ping. The server replies with a `Pong` event. Use WebSocket native PING
frames instead.

```json
{
  "type": "ping"
}
```

The `type` field is case-insensitive: `ping`, `PING`, and `Ping` are all accepted.

---

### IceRestart

Sent by the client when it needs to restart the ICE negotiation for a meeting session (e.g. after
a network change). Triggers a new WebRTC negotiation for the affected meeting.

```json
{
  "type": "IceRestart",
  "meetingId": "[1]"
}
```

Where:

1. UUID of the meeting for which ICE restart is requested
