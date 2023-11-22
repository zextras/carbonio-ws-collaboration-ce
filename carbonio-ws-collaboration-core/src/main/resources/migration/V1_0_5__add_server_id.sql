-- SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
--
-- SPDX-License-Identifier: AGPL-3.0-only

ALTER TABLE CHATS.VIDEOSERVER_MEETING
    ADD COLUMN SERVER_ID VARCHAR(64);
