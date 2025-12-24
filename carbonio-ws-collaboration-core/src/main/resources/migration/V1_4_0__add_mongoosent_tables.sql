-- SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
--
-- SPDX-License-Identifier: AGPL-3.0-only

-- ============================================================================
-- MongoosENT Chat System - Complete separation from mongoose-based chat
-- All tables prefixed with MONGOOSENT_ for clear separation
-- ============================================================================

-- Enable pg_trgm extension for full-text search with partial matching
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ============================================================================
-- MONGOOSENT_ROOM - Chat rooms (1-to-1 or group)
-- ============================================================================
CREATE TABLE IF NOT EXISTS CHATS.MONGOOSENT_ROOM
(
    ID          VARCHAR(64) PRIMARY KEY,
    NAME        VARCHAR(256),                           -- NULL for 1-to-1 chats
    DESCRIPTION TEXT,
    TYPE        VARCHAR(32) NOT NULL,                   -- 'ONE_TO_ONE' or 'GROUP'
    CREATED_BY  VARCHAR(64) NOT NULL,                   -- User who created the room
    CREATED_AT  TIMESTAMP   NOT NULL DEFAULT NOW(),
    UPDATED_AT  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX MONGOOSENT_ROOM_TYPE_IDX ON CHATS.MONGOOSENT_ROOM (TYPE);
CREATE INDEX MONGOOSENT_ROOM_CREATED_BY_IDX ON CHATS.MONGOOSENT_ROOM (CREATED_BY);

-- ============================================================================
-- MONGOOSENT_ROOM_MEMBER - Room membership
-- ============================================================================
CREATE TABLE IF NOT EXISTS CHATS.MONGOOSENT_ROOM_MEMBER
(
    ROOM_ID    VARCHAR(64) NOT NULL,
    USER_ID    VARCHAR(64) NOT NULL,
    IS_OWNER   BOOLEAN     NOT NULL DEFAULT FALSE,
    MUTED      BOOLEAN     NOT NULL DEFAULT FALSE,
    JOINED_AT  TIMESTAMP   NOT NULL DEFAULT NOW(),

    PRIMARY KEY (ROOM_ID, USER_ID),
    CONSTRAINT FK_MEMBER_ROOM FOREIGN KEY (ROOM_ID) REFERENCES CHATS.MONGOOSENT_ROOM (ID) ON DELETE CASCADE
);

CREATE INDEX MONGOOSENT_ROOM_MEMBER_USER_IDX ON CHATS.MONGOOSENT_ROOM_MEMBER (USER_ID);

-- ============================================================================
-- MONGOOSENT_MESSAGE - Chat messages (compressed view)
-- Edits update the record directly, deletes set the deleted flag and clear text
-- ============================================================================
CREATE TABLE IF NOT EXISTS CHATS.MONGOOSENT_MESSAGE
(
    ID                VARCHAR(64) PRIMARY KEY,
    ROOM_ID           VARCHAR(64)  NOT NULL,
    SENDER_ID         VARCHAR(64)  NOT NULL,
    TEXT              TEXT         NOT NULL,
    REPLY_TO_ID       VARCHAR(64),                      -- Reference to parent message if this is a reply
    FORWARDED_FROM_ID VARCHAR(64),                      -- Reference to original message if forwarded
    FORWARDED_BY      VARCHAR(64),                      -- User who forwarded the message
    EDITED            BOOLEAN      NOT NULL DEFAULT FALSE,
    DELETED           BOOLEAN      NOT NULL DEFAULT FALSE,
    CREATED_AT        TIMESTAMP    NOT NULL DEFAULT NOW(),
    UPDATED_AT        TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT FK_MSG_ROOM FOREIGN KEY (ROOM_ID) REFERENCES CHATS.MONGOOSENT_ROOM (ID) ON DELETE CASCADE,
    CONSTRAINT FK_MSG_REPLY FOREIGN KEY (REPLY_TO_ID) REFERENCES CHATS.MONGOOSENT_MESSAGE (ID) ON DELETE SET NULL,
    CONSTRAINT FK_MSG_FORWARD FOREIGN KEY (FORWARDED_FROM_ID) REFERENCES CHATS.MONGOOSENT_MESSAGE (ID) ON DELETE SET NULL
);

-- Index for querying messages by room (most common query)
CREATE INDEX MONGOOSENT_MSG_ROOM_CREATED_IDX ON CHATS.MONGOOSENT_MESSAGE (ROOM_ID, CREATED_AT DESC);

-- Index for querying messages by sender
CREATE INDEX MONGOOSENT_MSG_SENDER_IDX ON CHATS.MONGOOSENT_MESSAGE (SENDER_ID);

-- GIN index with pg_trgm for full-text search with partial matching
CREATE INDEX MONGOOSENT_MSG_TEXT_TRGM_IDX ON CHATS.MONGOOSENT_MESSAGE USING GIN (TEXT gin_trgm_ops);

-- ============================================================================
-- MONGOOSENT_MESSAGE_REACTION - Emoji reactions to messages
-- ============================================================================
CREATE TABLE IF NOT EXISTS CHATS.MONGOOSENT_MESSAGE_REACTION
(
    MESSAGE_ID VARCHAR(64) NOT NULL,
    USER_ID    VARCHAR(64) NOT NULL,
    REACTION   VARCHAR(32) NOT NULL,                    -- Unicode emoji
    CREATED_AT TIMESTAMP   NOT NULL DEFAULT NOW(),

    PRIMARY KEY (MESSAGE_ID, USER_ID, REACTION),
    CONSTRAINT FK_REACTION_MSG FOREIGN KEY (MESSAGE_ID) REFERENCES CHATS.MONGOOSENT_MESSAGE (ID) ON DELETE CASCADE
);

CREATE INDEX MONGOOSENT_REACTION_MSG_IDX ON CHATS.MONGOOSENT_MESSAGE_REACTION (MESSAGE_ID);

-- ============================================================================
-- MONGOOSENT_MESSAGE_READ - Read markers per user per room
-- Stores the last read message ID for each user in each room
-- ============================================================================
CREATE TABLE IF NOT EXISTS CHATS.MONGOOSENT_MESSAGE_READ
(
    USER_ID    VARCHAR(64) NOT NULL,
    ROOM_ID    VARCHAR(64) NOT NULL,
    MESSAGE_ID VARCHAR(64) NOT NULL,                    -- Last read message
    READ_AT    TIMESTAMP   NOT NULL DEFAULT NOW(),

    PRIMARY KEY (USER_ID, ROOM_ID),
    CONSTRAINT FK_READ_ROOM FOREIGN KEY (ROOM_ID) REFERENCES CHATS.MONGOOSENT_ROOM (ID) ON DELETE CASCADE,
    CONSTRAINT FK_READ_MSG FOREIGN KEY (MESSAGE_ID) REFERENCES CHATS.MONGOOSENT_MESSAGE (ID) ON DELETE CASCADE
);

-- ============================================================================
-- MONGOOSENT_USER_PRESENCE - Online/offline status
-- ============================================================================
CREATE TABLE IF NOT EXISTS CHATS.MONGOOSENT_USER_PRESENCE
(
    USER_ID          VARCHAR(64) PRIMARY KEY,
    ONLINE           BOOLEAN   NOT NULL DEFAULT FALSE,
    LAST_ACTIVITY_AT TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX MONGOOSENT_PRESENCE_ONLINE_IDX ON CHATS.MONGOOSENT_USER_PRESENCE (ONLINE) WHERE ONLINE = TRUE;

-- ============================================================================
-- MONGOOSENT_MESSAGE_ATTACHMENT - File attachments linked to messages
-- MESSAGE_ID is NULL for pending uploads (not yet linked to a message)
-- ============================================================================
CREATE TABLE IF NOT EXISTS CHATS.MONGOOSENT_MESSAGE_ATTACHMENT
(
    ID            VARCHAR(64) PRIMARY KEY,
    MESSAGE_ID    VARCHAR(64),                        -- NULL for pending uploads, set when linked
    USER_ID       VARCHAR(64) NOT NULL,               -- Uploader (owner in storages)
    FILE_NAME     VARCHAR(512) NOT NULL,
    MIME_TYPE     VARCHAR(256) NOT NULL,
    FILE_SIZE     BIGINT NOT NULL,
    DELETED       BOOLEAN NOT NULL DEFAULT FALSE,     -- Soft delete (blob removed from storages)
    CREATED_AT    TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT FK_ATTACH_MSG FOREIGN KEY (MESSAGE_ID) REFERENCES CHATS.MONGOOSENT_MESSAGE (ID) ON DELETE CASCADE
);

-- Index for finding attachments by message
CREATE INDEX MONGOOSENT_ATTACH_MSG_IDX ON CHATS.MONGOOSENT_MESSAGE_ATTACHMENT (MESSAGE_ID) WHERE MESSAGE_ID IS NOT NULL;

-- Partial index for orphan cleanup job: only indexes pending attachments (MESSAGE_ID IS NULL)
-- This index stays small regardless of total attachment count
CREATE INDEX MONGOOSENT_ATTACH_ORPHAN_IDX ON CHATS.MONGOOSENT_MESSAGE_ATTACHMENT (CREATED_AT) WHERE MESSAGE_ID IS NULL AND DELETED = FALSE;

-- ============================================================================
-- MONGOOSENT_MESSAGE_EVENT - Append-only event log for message operations
-- This table is WRITE-ONLY: no indexes (except PK), never queried in normal operation
-- Used for audit trail, compliance, and debugging purposes
-- ============================================================================
CREATE TABLE IF NOT EXISTS CHATS.MONGOOSENT_MESSAGE_EVENT
(
    ID          VARCHAR(64) PRIMARY KEY,
    MESSAGE_ID  VARCHAR(64) NOT NULL,
    ROOM_ID     VARCHAR(64) NOT NULL,
    USER_ID     VARCHAR(64) NOT NULL,
    EVENT_TYPE  VARCHAR(32) NOT NULL,
    PAYLOAD     JSONB,
    CREATED_AT  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- NO ADDITIONAL INDEXES: This table is append-only, never queried in normal operation
