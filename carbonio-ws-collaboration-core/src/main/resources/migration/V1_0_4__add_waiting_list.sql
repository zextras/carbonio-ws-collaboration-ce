-- SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
--
-- SPDX-License-Identifier: AGPL-3.0-only

CREATE TABLE IF NOT EXISTS CHATS.WAITING_PARTICIPANT
(
    ID                 VARCHAR(64) PRIMARY KEY,
    USER_ID            VARCHAR(36) NOT NULL,
    MEETING_ID         VARCHAR(36) NOT NULL,
    QUEUE_ID           VARCHAR(36) NOT NULL,
    STATUS             VARCHAR(64) NOT NULL,
    UNIQUE             (user_id, meeting_id)
);