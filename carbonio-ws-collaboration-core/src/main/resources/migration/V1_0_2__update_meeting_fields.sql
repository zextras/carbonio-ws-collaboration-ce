-- SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
--
-- SPDX-License-Identifier: AGPL-3.0-only

ALTER TABLE CHATS.MEETING
ADD NAME VARCHAR(128),
ADD MEETING_TYPE VARCHAR(32) NOT NULL,
ADD EXPIRATION TIMESTAMP,
ADD ACTIVE BOOLEAN DEFAULT FALSE;
