ALTER TABLE CHATS.ROOM
    ADD COLUMN MEETING_ID VARCHAR(64);
ALTER TABLE CHATS.ROOM
    ADD CONSTRAINT fk_orders_customers FOREIGN KEY (MEETING_ID) REFERENCES CHATS.MEETING (ID) ON DELETE SET NULL ;
