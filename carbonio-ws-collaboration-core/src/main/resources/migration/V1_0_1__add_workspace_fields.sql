ALTER TABLE CHATS.ROOM
    ADD COLUMN PARENT_ID          VARCHAR(64),
    ADD COLUMN RANK               INTEGER,
    ADD CONSTRAINT FK_ROOM_PARENT_ID__ROOM_ID FOREIGN KEY (PARENT_ID) REFERENCES CHATS.ROOM (ID) ON DELETE CASCADE;

ALTER TABLE CHATS.ROOM_USER_SETTINGS
    ADD COLUMN  RANK        INTEGER;