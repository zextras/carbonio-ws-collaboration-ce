ALTER TABLE CHATS.ROOM
    ADD COLUMN MEETING_ID VARCHAR(64);
ALTER TABLE CHATS.ROOM
    ADD CONSTRAINT FK_ROOM__MEETING FOREIGN KEY (MEETING_ID) REFERENCES CHATS.MEETING (ID) ON DELETE SET NULL;
ALTER TABLE CHATS.FILE_METADATA
    DROP CONSTRAINT FK_FILE__ROOM_ID;
ALTER TABLE CHATS.FILE_METADATA
    ADD CONSTRAINT FK_FILE__ROOM_ID FOREIGN KEY (ROOM_ID) REFERENCES CHATS.ROOM (ID) ON DELETE SET NULL;