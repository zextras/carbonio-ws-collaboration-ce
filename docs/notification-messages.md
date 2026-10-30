# Push notification messages

How chat and call push notifications reach the mobile apps, and what the apps receive. Only the Advanced
edition (`carbonio-ws-collaboration`) sends push notifications; the Community Edition sends none.

## Delivery chain

1. **WSC** builds the notification and posts it to `carbonio-notification-push` (`POST /notifications`). It runs
   on a dedicated bounded executor and is best-effort: a failure is logged, a full queue drops the push with a
   warning.
2. **`carbonio-notification-push`** looks up the registered devices of every receiver and calls the push lambda
   once per device token. It does no online or presence filtering: every registered device of every receiver is
   notified. It **replaces `title` with the sender id** and adds `senderName` to the data (the sender's full
   name, or e-mail when there is no name; absent when the user lookup fails).
3. **The push lambda** (`infra-aws-lambda-notification` 1.3.0 or later) delivers the notification:
   - **Android (FCM): data-only message.** There is no notification block: the app always renders the
     notification itself from the data.
   - **iOS (APNs) alert:** the lambda composes the banner from the data (see [iOS alert](#ios-alert)).
   - **iOS (APNs) VoIP:** only `new_meeting`, only for apps that registered a VoIP token. The payload is silent
     and the app reports the call to CallKit; those apps get no alert for `new_meeting`.
   - `answered_meeting` and `declined_meeting` are never sent to iOS. On Android they reach only the apps that
     registered a device name (older apps do not get them).

The `title` that WSC sends never reaches a device: `carbonio-notification-push` overwrites it. The notification
body never contains the sender's name: the Android app adds it from its own data, the iOS lambda puts it in
the alert subtitle.

## Notification types

| `notificationType` | Sent when | Receivers | `title` sent by WSC (overwritten) | `message` | WSC `data` fields |
|---|---|---|---|---|---|
| `new_message` | A text message is sent (`POST /rooms/{roomId}/messages`), or an attachment with a non-blank caption is uploaded (`POST /rooms/{roomId}/attachments`) | Room members except the sender and the members who muted the room | Room name in named rooms that are not ONE_TO_ONE, otherwise the sender's name | The text or caption; above 500 Unicode code points it is cut at 500 and ends with `…` | `roomId`, `roomName` (named rooms that are not ONE_TO_ONE only), `roomType` (`one_to_one`, `group` or `temporary`), `messageId`, `notificationType` |
| `new_meeting` | A meeting starts in a ONE_TO_ONE room | The other member | `Incoming Call` | `<caller's name> is calling you` | `meetingId`, `roomId`, `notificationType` |
| `declined_meeting` | A user declines a ONE_TO_ONE call | The decliner (to stop the ringing on their other devices) | `Incoming Call` | `<decliner's name> is calling you` | `meetingId`, `roomId`, `notificationType` |
| `answered_meeting` | A user joins a ONE_TO_ONE call started by the other member | The answerer (to stop the ringing on their other devices) | `Incoming Call` | `<answerer's name> is calling you` | `meetingId`, `roomId`, `notificationType` |

No notification is sent for blank texts, attachments without a caption, edits, deletes, reactions, forwards,
pins or system lines. There are no `DELETED_MESSAGE` or `EDITED_MESSAGE` notifications.

## Payloads

### WSC to carbonio-notification-push

```json
{
  "senderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "receiverIds": ["b2c3d4e5-f6a7-8901-bcde-f12345678901"],
  "title": "Design Team",
  "message": "Are you there?",
  "data": [
    { "field": "roomId", "value": "c3d4e5f6-a7b8-9012-cdef-123456789012" },
    { "field": "roomName", "value": "Design Team" },
    { "field": "roomType", "value": "group" },
    { "field": "messageId", "value": "d4e5f6a7-b8c9-0123-def0-234567890123" },
    { "field": "notificationType", "value": "new_message" }
  ]
}
```

### Android (FCM data)

Every value is a string. `title` is the sender id (set by `carbonio-notification-push`), `titleComplete` is the
legacy copy of `senderName` added by the lambda.

```json
{
  "message": {
    "token": "<FCM device token>",
    "data": {
      "senderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "title": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "message": "Are you there?",
      "senderName": "Alice Smith",
      "titleComplete": "Alice Smith",
      "roomId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
      "roomName": "Design Team",
      "roomType": "group",
      "messageId": "d4e5f6a7-b8c9-0123-def0-234567890123",
      "notificationType": "new_message"
    }
  }
}
```

A `new_meeting` carries `meetingId`, `roomId` and `notificationType` instead of the room and message fields,
with `message` = `<caller's name> is calling you`.

### iOS alert

The lambda sets `alert.title` to `senderName`. For `new_message` with a `roomName`, `alert.title` is the room
name and `alert.subtitle` is `senderName`. `alert.body` is the `message`. `thread-id` is the `roomId`. The data
travels in `custom` (without `title` and `message`).

```json
{
  "aps": {
    "alert": {
      "title": "Design Team",
      "subtitle": "Alice Smith",
      "body": "Are you there?"
    },
    "mutable-content": 1,
    "thread-id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "sound": "default"
  },
  "custom": {
    "senderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "senderName": "Alice Smith",
    "titleComplete": "Alice Smith",
    "roomId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "roomName": "Design Team",
    "roomType": "group",
    "messageId": "d4e5f6a7-b8c9-0123-def0-234567890123",
    "notificationType": "new_message"
  }
}
```

In a ONE_TO_ONE room there is no `roomName`: the banner is title = sender, body = text, no subtitle.

### iOS VoIP (`new_meeting` only)

```json
{
  "aps": {},
  "custom": {
    "senderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "senderName": "Alice Smith",
    "titleComplete": "Alice Smith",
    "meetingId": "e5f6a7b8-c9d0-1234-ef01-345678901234",
    "roomId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "notificationType": "new_meeting"
  }
}
```
