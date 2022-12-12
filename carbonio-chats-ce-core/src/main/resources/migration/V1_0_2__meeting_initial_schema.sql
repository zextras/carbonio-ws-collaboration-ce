-- SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
--
-- SPDX-License-Identifier: AGPL-3.0-only

CREATE TABLE IF NOT EXISTS CHATS.MEETING
(
    ID         VARCHAR(64) PRIMARY KEY,
    ROOM_ID    VARCHAR(64) NOT NULL,
    CREATED_AT TIMESTAMP   NOT NULL,
    CONSTRAINT FK_MEETING_ROOM__ROOM_ID FOREIGN KEY (ROOM_ID) REFERENCES CHATS.ROOM (ID) ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS CHATS.PARTICIPANT
(
    USER_ID       VARCHAR(64),
    MEETING_ID    VARCHAR(64),
    SESSION_ID    VARCHAR(64),
    MICROPHONE_ON BOOLEAN DEFAULT FALSE,
    CAMERA_ON     BOOLEAN DEFAULT FALSE,
    CREATED_AT    TIMESTAMP NOT NULL,
    UPDATED_AT    TIMESTAMP NOT NULL,

    PRIMARY KEY (USER_ID, MEETING_ID, SESSION_ID),
    CONSTRAINT FK_SUBSCRIPTION_ROOM__ROOM_ID FOREIGN KEY (MEETING_ID) REFERENCES CHATS.MEETING (ID) ON DELETE CASCADE
);