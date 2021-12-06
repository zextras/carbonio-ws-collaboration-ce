CREATE SCHEMA IF NOT EXISTS CHATS;

CREATE TABLE IF NOT EXISTS CHATS.CHATS_USER
(
    ID               VARCHAR(64) PRIMARY KEY,
    LAST_SEEN        TIMESTAMP,
    STATUS_MESSAGE   VARCHAR(256) DEFAULT '' NOT NULL,
    IMAGE            BYTEA,
    IMAGE_UPDATED_AT TIMESTAMP,
    HASH             VARCHAR(256) UNIQUE     NOT NULL,
    CREATED_AT       TIMESTAMP               NOT NULL,
    UPDATED_AT       TIMESTAMP               NOT NULL
);
CREATE UNIQUE INDEX IDX_USER_HASH ON CHATS.CHATS_USER (HASH);


CREATE TABLE IF NOT EXISTS CHATS.ROOM
(
    ID          VARCHAR(64) PRIMARY KEY,
    NAME        VARCHAR(128)        NOT NULL,
    DESCRIPTION VARCHAR(256),
    HASH        VARCHAR(256) UNIQUE NOT NULL,
    DOMAIN      VARCHAR(256),
    TYPE        VARCHAR(32)         NOT NULL,
    PASSWORD    VARCHAR(256),
    CREATED_AT  TIMESTAMP           NOT NULL,
    UPDATED_AT  TIMESTAMP           NOT NULL
);
CREATE UNIQUE INDEX IDX_ROOM_HASH ON CHATS.ROOM (HASH);


CREATE TABLE IF NOT EXISTS CHATS.ROOM_IMAGE
(
    ROOM_ID    VARCHAR(64) PRIMARY KEY,
    IMAGE      BYTEA,
    CREATED_AT TIMESTAMP NOT NULL,
    UPDATED_AT TIMESTAMP NOT NULL,
    CONSTRAINT FK_ROOM_IMAGE__ROOM_ID FOREIGN KEY (ROOM_ID) REFERENCES CHATS.ROOM (ID)
);


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


CREATE TABLE IF NOT EXISTS CHATS.MESSAGE
(
    ID             VARCHAR(64) PRIMARY KEY,
    SENT_AT        TIMESTAMP                     NOT NULL,
    EDIT_AT        TIMESTAMP,
    MESSAGE_TYPE   VARCHAR(64) DEFAULT 'MESSAGE' NOT NULL,
    USER_ID        VARCHAR(64)                   NOT NULL,
    ROOM_ID        VARCHAR(64)                   NOT NULL,
    TEXT           VARCHAR(4096),
    REACTION       VARCHAR(4096),
    TYPE_EXTRAINFO VARCHAR(4096),
    DELETED        BOOLEAN     DEFAULT FALSE     NOT NULL,
    REPLIED_TO     VARCHAR(64),
    FORWARDED_FROM VARCHAR(64),
    CONSTRAINT FK_MESSAGE_ROOM__ROOM_ID FOREIGN KEY (ROOM_ID) REFERENCES CHATS.ROOM (ID) ON DELETE CASCADE,
    CONSTRAINT FK_MESSAGE_REPLIED__MESSAGE_ID FOREIGN KEY (REPLIED_TO) REFERENCES CHATS.MESSAGE (ID)
);


CREATE TABLE IF NOT EXISTS CHATS.MESSAGE_READ
(
    USER_ID    VARCHAR(64),
    ROOM_ID    VARCHAR(64),
    MESSAGE_ID VARCHAR(64),
    READ_AT    TIMESTAMP,
    PRIMARY KEY (USER_ID, ROOM_ID, MESSAGE_ID),
    CONSTRAINT FK_MESSAGE_READ_ROOM__ROOM_ID FOREIGN KEY (ROOM_ID) REFERENCES CHATS.ROOM (ID) ON DELETE CASCADE,
    CONSTRAINT FK_MESSAGE_READ_MESSAG__MSG_ID FOREIGN KEY (MESSAGE_ID) REFERENCES CHATS.MESSAGE (ID)
);


CREATE TABLE IF NOT EXISTS CHATS.ATTACHMENT
(
    ID            VARCHAR(64) PRIMARY KEY,
    NAME          VARCHAR(256) NOT NULL,
    ORIGINAL_SIZE INT          NOT NULL,
    MIME_TYPE     VARCHAR(64)  NOT NULL,
    USER_ID       VARCHAR(64)  NOT NULL,
    ROOM_ID       VARCHAR(64)  NOT NULL,
    CONTENT       BYTEA, -- DELETE ME AS SOON AS POSSIBLE
    CREATED_AT    TIMESTAMP    NOT NULL,
    UPDATED_AT    TIMESTAMP    NOT NULL,
    CONSTRAINT FK_ATTACHMENT__ROOM_ID FOREIGN KEY (ROOM_ID) REFERENCES CHATS.ROOM (ID)
);
CREATE UNIQUE INDEX IDX_ATTACHMENT_USER ON CHATS.ATTACHMENT (USER_ID);
CREATE UNIQUE INDEX IDX_ATTACHMENT_ROOM ON CHATS.ATTACHMENT (ROOM_ID);
