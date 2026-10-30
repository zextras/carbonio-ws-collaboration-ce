-- SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
--
-- SPDX-License-Identifier: AGPL-3.0-only

-- (room, sender, tempId) -> message id of the first send; rows older than 1 hour are expired and purged.
CREATE TABLE IF NOT EXISTS CHATS.MESSAGE_SEND_DEDUP
(
    ROOM_ID    VARCHAR(64) NOT NULL,
    SENDER_ID  VARCHAR(64) NOT NULL,
    TEMP_ID    VARCHAR(64) NOT NULL,
    MESSAGE_ID VARCHAR(64) NOT NULL,
    CREATED_AT TIMESTAMP   NOT NULL DEFAULT NOW(),

    PRIMARY KEY (ROOM_ID, SENDER_ID, TEMP_ID)
);

CREATE INDEX IF NOT EXISTS MESSAGE_SEND_DEDUP_CREATED_AT_IDX ON CHATS.MESSAGE_SEND_DEDUP (CREATED_AT);
