-- SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
--
-- SPDX-License-Identifier: AGPL-3.0-only

ALTER TABLE CHATS.FILE_METADATA
    ALTER COLUMN MIME_TYPE TYPE VARCHAR(256);
