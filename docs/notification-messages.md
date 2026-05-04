# Notification messages details

The purpose of this document is sharing details about the structure of notification messages
which mobile clients are going to handle.

## Message types

### NEW_MESSAGE

- **Title:** roomId
- **Body:** text message
- **(ImageUrl):** URL for the preview *(optional)*
- **Data:**
  - `senderId`
  - `roomName`
  - `roomType`
  - `notificationType`

### DELETED_MESSAGE

- **Title:** roomId
- **Data:**
  - `senderId`
  - `roomName`
  - `roomType`
  - `notificationType`
  - `originalMessageId`

### EDITED_MESSAGE

- **Title:** roomId
- **Body:** text message
- **(ImageUrl):** URL for the preview *(optional)*
- **Data:**
  - `senderId`
  - `roomName`
  - `roomType`
  - `notificationType`
  - `originalMessageId`

---

## Notification payloads

### Android — New message

```json
{
  "message": {
    "token": "firebase-device-token-xyz",
    "data": {
      "senderId": "user-123-uuid",
      "title": "room-id",
      "message": "Hello world!",
      "roomId": "room-789",
      "roomName": "Beautiful room",
      "roomType": "group",
      "notificationType": "new_message"
    }
  }
}
```

> `roomName` is optional. `roomType` can be `"group"` or `"one_to_one"`.

### Android — New meeting

```json
{
  "message": {
    "token": "firebase-device-token-xyz",
    "data": {
      "senderId": "user-123-uuid",
      "title": "Incoming call",
      "message": "John Doe is calling you",
      "meetingId": "meeting-456",
      "roomId": "room-789",
      "notificationType": "new_meeting"
    }
  }
}
```

### iOS — New message

```json
{
  "aps": {
    "alert": {
      "body": "Hello world!"
    },
    "mutable-content": 1
  },
  "custom": {
    "senderId": "fake-user-id",
    "notificationType": "new_message",
    "title": "room-id",
    "roomName": "Beautiful room",
    "roomType": "group",
    "roomId": "room-id"
  }
}
```

> `roomName` is optional. `roomType` can be `"group"` or `"one_to_one"`.

### iOS — New meeting

```json
{
  "aps": {
    "alert": {
      "body": "John Doe is calling you"
    },
    "mutable-content": 1
  },
  "custom": {
    "meetingId": "meeting-id",
    "senderId": "fake-user-id",
    "notificationType": "new_meeting",
    "title": "Incoming Call",
    "roomId": "room-id"
  }
}
```
