ALTER TABLE CHATS.ROOM
  DROP CONSTRAINT FK_ROOM_PARENT_ID__ROOM_ID,
  DROP COLUMN PARENT_ID,
  DROP COLUMN RANK;

ALTER TABLE CHATS.ROOM_USER_SETTINGS
  DROP COLUMN RANK;