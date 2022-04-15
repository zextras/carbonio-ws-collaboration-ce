-- SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
--
-- SPDX-License-Identifier: AGPL-3.0-only

CREATE SCHEMA IF NOT EXISTS CHATS;

CREATE TABLE IF NOT EXISTS CHATS.CHATS_USER
(
    ID               VARCHAR(64) PRIMARY KEY,
    LAST_SEEN        TIMESTAMP,
    STATUS_MESSAGE   VARCHAR(256) DEFAULT '' NOT NULL,
    HASH             VARCHAR(256) UNIQUE     NOT NULL,
    CREATED_AT       TIMESTAMP               NOT NULL,
    UPDATED_AT       TIMESTAMP               NOT NULL
);
CREATE UNIQUE INDEX IDX_USER_HASH ON CHATS.CHATS_USER (HASH);


CREATE TABLE IF NOT EXISTS CHATS.ROOM
(
    ID                 VARCHAR(64) PRIMARY KEY,
    NAME               VARCHAR(128)        NOT NULL,
    DESCRIPTION        VARCHAR(256),
    HASH               VARCHAR(256) UNIQUE NOT NULL,
    TYPE               VARCHAR(32)         NOT NULL,
    PASSWORD           VARCHAR(256),
    PICTURE_UPDATED_AT TIMESTAMP,
    CREATED_AT         TIMESTAMP           NOT NULL,
    UPDATED_AT         TIMESTAMP           NOT NULL
);
CREATE UNIQUE INDEX IDX_ROOM_HASH ON CHATS.ROOM (HASH);


CREATE TABLE IF NOT EXISTS CHATS.SUBSCRIPTION
(
    USER_ID    VARCHAR(64),
    ROOM_ID    VARCHAR(64),
    JOINED_AT  TIMESTAMP,
    OWNER      BOOLEAN DEFAULT FALSE,
    EXTERNAL   BOOLEAN DEFAULT FALSE,
    TEMPORARY  BOOLEAN DEFAULT FALSE,
    CREATED_AT TIMESTAMP NOT NULL,
    UPDATED_AT TIMESTAMP NOT NULL,

    PRIMARY KEY (USER_ID, ROOM_ID),
    CONSTRAINT FK_SUBSCRIPTION_ROOM__ROOM_ID FOREIGN KEY (ROOM_ID) REFERENCES CHATS.ROOM (ID) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS CHATS.ROOM_USER_SETTINGS
(
    USER_ID     VARCHAR(64),
    ROOM_ID     VARCHAR(64),
    MUTED_UNTIL TIMESTAMP,
    CLEARED_AT  TIMESTAMP,
    CREATED_AT  TIMESTAMP NOT NULL,
    UPDATED_AT  TIMESTAMP NOT NULL,

    PRIMARY KEY (USER_ID, ROOM_ID),
    CONSTRAINT FK_ROOM_USER_SETTINGS__ROOM_ID FOREIGN KEY (ROOM_ID) REFERENCES CHATS.ROOM (ID) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS CHATS.FILE_METADATA
(
    ID            VARCHAR(64) PRIMARY KEY,
    NAME          VARCHAR(256) NOT NULL,
    ORIGINAL_SIZE INT          NOT NULL,
    MIME_TYPE     VARCHAR(64)  NOT NULL,
    TYPE          VARCHAR(32)  NOT NULL,
    USER_ID       VARCHAR(64)  NOT NULL,
    ROOM_ID       VARCHAR(64)  NOT NULL,
    CREATED_AT    TIMESTAMP    NOT NULL,
    UPDATED_AT    TIMESTAMP    NOT NULL,

    CONSTRAINT FK_FILE__ROOM_ID FOREIGN KEY (ROOM_ID) REFERENCES CHATS.ROOM (ID)
);
CREATE INDEX FILE_METADATA_CREATED_AT__ID ON CHATS.FILE_METADATA (CREATED_AT, ID);
