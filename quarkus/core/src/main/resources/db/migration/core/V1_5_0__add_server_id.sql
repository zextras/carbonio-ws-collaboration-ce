-- SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
--
-- SPDX-License-Identifier: AGPL-3.0-only

-- Inert in CE (single VideoServer): the column is nullable and never written by CE.
-- It exists so the shared VideoServerMeeting entity (consumed from this jar by Advanced,
-- which persists a server id for multi-VideoServer routing) maps cleanly on both editions.
ALTER TABLE CHATS.VIDEOSERVER_MEETING
    ADD COLUMN SERVER_ID VARCHAR(64);
