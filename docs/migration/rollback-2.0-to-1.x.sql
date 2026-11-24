-- SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
--
-- SPDX-License-Identifier: AGPL-3.0-only

-- Rolls the WSC database back from 2.0.x to the 1.x schema (CE).
-- psql -d carbonio-ws-collaboration-db -v ON_ERROR_STOP=1 -v zone="'<zone>'" -f <this file>
-- <zone>: the expression of the startup line "Flyway 1.x time conversion zone:" of the upgrade.
BEGIN;

DROP TABLE chats.message_reaction, chats.message_pin, chats.read_marker, chats.system_event,
           chats.ws_session, chats.user_last_seen, chats.message_send_dedup, chats.message;
DROP INDEX chats.subscription_room_id_idx, chats.room_user_settings_room_id_idx;
ALTER TABLE chats.file_metadata DROP COLUMN area, DROP COLUMN chat_message_id;

-- Rows created on 2.x have no 1.x value: they get the 2.x time as wall clock of the 1.x zone.
-- Rows that existed before the upgrade get back their 1.x values, untouched since then.
UPDATE chats.room
SET picture_updated_at_v1 = picture_updated_at AT TIME ZONE :zone,
    created_at_v1         = created_at AT TIME ZONE :zone,
    updated_at_v1         = updated_at AT TIME ZONE :zone
WHERE created_at_v1 IS NULL;
ALTER TABLE chats.room
    DROP COLUMN picture_updated_at,
    DROP COLUMN created_at,
    DROP COLUMN updated_at;
ALTER TABLE chats.room RENAME COLUMN picture_updated_at_v1 TO picture_updated_at;
ALTER TABLE chats.room RENAME COLUMN created_at_v1 TO created_at;
ALTER TABLE chats.room RENAME COLUMN updated_at_v1 TO updated_at;
ALTER TABLE chats.room
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE chats.subscription
SET joined_at_v1  = joined_at AT TIME ZONE :zone,
    created_at_v1 = created_at AT TIME ZONE :zone,
    updated_at_v1 = updated_at AT TIME ZONE :zone
WHERE created_at_v1 IS NULL;
ALTER TABLE chats.subscription
    DROP COLUMN joined_at,
    DROP COLUMN created_at,
    DROP COLUMN updated_at;
ALTER TABLE chats.subscription RENAME COLUMN joined_at_v1 TO joined_at;
ALTER TABLE chats.subscription RENAME COLUMN created_at_v1 TO created_at;
ALTER TABLE chats.subscription RENAME COLUMN updated_at_v1 TO updated_at;
ALTER TABLE chats.subscription
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE chats.room_user_settings
SET muted_until_v1 = muted_until AT TIME ZONE :zone,
    cleared_at_v1  = cleared_at AT TIME ZONE :zone,
    created_at_v1  = created_at AT TIME ZONE :zone,
    updated_at_v1  = updated_at AT TIME ZONE :zone
WHERE created_at_v1 IS NULL;
ALTER TABLE chats.room_user_settings
    DROP COLUMN muted_until,
    DROP COLUMN cleared_at,
    DROP COLUMN created_at,
    DROP COLUMN updated_at;
ALTER TABLE chats.room_user_settings RENAME COLUMN muted_until_v1 TO muted_until;
ALTER TABLE chats.room_user_settings RENAME COLUMN cleared_at_v1 TO cleared_at;
ALTER TABLE chats.room_user_settings RENAME COLUMN created_at_v1 TO created_at;
ALTER TABLE chats.room_user_settings RENAME COLUMN updated_at_v1 TO updated_at;
ALTER TABLE chats.room_user_settings
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE chats.file_metadata
SET created_at_v1 = created_at AT TIME ZONE :zone,
    updated_at_v1 = updated_at AT TIME ZONE :zone
WHERE created_at_v1 IS NULL;
ALTER TABLE chats.file_metadata
    DROP COLUMN created_at,
    DROP COLUMN updated_at;
ALTER TABLE chats.file_metadata RENAME COLUMN created_at_v1 TO created_at;
ALTER TABLE chats.file_metadata RENAME COLUMN updated_at_v1 TO updated_at;
ALTER TABLE chats.file_metadata
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE chats.meeting
SET created_at_v1 = created_at AT TIME ZONE :zone,
    started_at_v1 = started_at AT TIME ZONE :zone,
    expiration_v1 = expiration AT TIME ZONE :zone
WHERE created_at_v1 IS NULL;
ALTER TABLE chats.meeting
    DROP COLUMN created_at,
    DROP COLUMN started_at,
    DROP COLUMN expiration;
ALTER TABLE chats.meeting RENAME COLUMN created_at_v1 TO created_at;
ALTER TABLE chats.meeting RENAME COLUMN started_at_v1 TO started_at;
ALTER TABLE chats.meeting RENAME COLUMN expiration_v1 TO expiration;
ALTER TABLE chats.meeting
    ALTER COLUMN created_at SET NOT NULL;

UPDATE chats.participant
SET created_at_v1     = created_at AT TIME ZONE :zone,
    updated_at_v1     = updated_at AT TIME ZONE :zone,
    hand_raised_at_v1 = hand_raised_at AT TIME ZONE :zone
WHERE created_at_v1 IS NULL;
ALTER TABLE chats.participant
    DROP COLUMN created_at,
    DROP COLUMN updated_at,
    DROP COLUMN hand_raised_at;
ALTER TABLE chats.participant RENAME COLUMN created_at_v1 TO created_at;
ALTER TABLE chats.participant RENAME COLUMN updated_at_v1 TO updated_at;
ALTER TABLE chats.participant RENAME COLUMN hand_raised_at_v1 TO hand_raised_at;
ALTER TABLE chats.participant
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

UPDATE chats.chats_user
SET created_at_v1 = created_at AT TIME ZONE :zone,
    updated_at_v1 = updated_at AT TIME ZONE :zone
WHERE created_at_v1 IS NULL;
ALTER TABLE chats.chats_user
    DROP COLUMN created_at,
    DROP COLUMN updated_at;
ALTER TABLE chats.chats_user RENAME COLUMN created_at_v1 TO created_at;
ALTER TABLE chats.chats_user RENAME COLUMN updated_at_v1 TO updated_at;
ALTER TABLE chats.chats_user
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm' AND extnamespace = 'chats'::regnamespace) THEN
    DROP EXTENSION pg_trgm;
  END IF;
END $$;

DELETE FROM chats.flyway_schema_history WHERE version LIKE '2.%';

COMMIT;

ALTER ROLE "carbonio-ws-collaboration-db" RESET synchronous_commit;
ALTER ROLE "carbonio-ws-collaboration-db" RESET jit;
