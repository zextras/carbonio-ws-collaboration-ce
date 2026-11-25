-- SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
--
-- SPDX-License-Identifier: AGPL-3.0-only

-- Rolls the WSC database back from 2.0.x to the 1.x schema (CE): the 2.x tables are dropped and the 1.x tables
-- kept untouched in schema chats_v1 by the upgrade move back to schema chats, so objects and data are those of
-- the moment before the upgrade. Everything written on 2.x is lost. It refuses to run if chats_v1 is incomplete or
-- if chats holds a table that is not a WSC table.
-- psql -d carbonio-ws-collaboration-db -v ON_ERROR_STOP=1 -f <this file>
BEGIN;

DO $$
DECLARE
  missing text;
  unknown text;
BEGIN
  SELECT string_agg(t, ', ') INTO missing
  FROM unnest(ARRAY['chats_user', 'room', 'subscription', 'room_user_settings', 'file_metadata', 'meeting',
                    'participant', 'videoserver_meeting', 'videoserver_session']) AS t
  WHERE to_regclass(format('chats_v1.%I', t)) IS NULL;
  SELECT string_agg(tablename, ', ' ORDER BY tablename) INTO unknown
  FROM pg_tables
  WHERE schemaname = 'chats'
    AND tablename NOT IN ('chats_user', 'room', 'subscription', 'room_user_settings', 'file_metadata', 'meeting',
                          'participant', 'videoserver_meeting', 'videoserver_session', 'message', 'message_reaction',
                          'read_marker', 'message_pin', 'system_event', 'ws_session', 'user_last_seen',
                          'message_send_dedup', 'flyway_schema_history');
  IF unknown IS NOT NULL THEN
    RAISE EXCEPTION 'schema chats holds tables that are not WSC tables (%): nothing was rolled back', unknown;
  END IF;
  IF to_regnamespace('chats_v1') IS NULL OR missing IS NOT NULL THEN
    RAISE EXCEPTION 'schema chats_v1 with the 1.x tables is missing (missing: %): nothing was rolled back',
      coalesce(missing, 'all');
  END IF;
END $$;

DO $$
DECLARE
  t record;
BEGIN
  FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'chats' AND tablename <> 'flyway_schema_history' LOOP
    EXECUTE format('DROP TABLE chats.%I CASCADE', t.tablename);
  END LOOP;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm' AND extnamespace = 'chats'::regnamespace) THEN
    DROP EXTENSION pg_trgm;
  END IF;
END $$;

DO $$
DECLARE
  t record;
BEGIN
  FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'chats_v1' LOOP
    EXECUTE format('ALTER TABLE chats_v1.%I SET SCHEMA chats', t.tablename);
  END LOOP;
END $$;

DROP SCHEMA chats_v1;

DELETE FROM chats.flyway_schema_history WHERE version LIKE '2.%';

COMMIT;

ALTER ROLE "carbonio-ws-collaboration-db" RESET synchronous_commit;
ALTER ROLE "carbonio-ws-collaboration-db" RESET jit;
