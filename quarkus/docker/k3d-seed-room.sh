#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

# The PoC has no rooms API: seed a group room straight into the WSC database
# of a carbonio-k3d cluster so meetings can be created on it.
#
# Usage: k3d-seed-room.sh <owner-zimbra-id> [member-zimbra-id ...]
# Prints the room id. Get a zimbraId with a SOAP GetAccountInfoRequest (see
# carbonio-k3d tests/hurl/wsc/room-and-meeting.hurl) or from the admin console.
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "usage: $0 <owner-zimbra-id> [member-zimbra-id ...]" >&2
  exit 1
fi

KUBECTL="${KUBECTL:-kubectl}"
CONTEXT="${CONTEXT:-k3d-carbonio}"
NAMESPACE="${NAMESPACE:-carbonio}"
POD="${POD:-carbonio-postgres-0}"
DB="${DB:-carbonio-ws-collaboration-db}"

uuid_re='^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
for id in "$@"; do
  [[ "$id" =~ $uuid_re ]] || { echo "not a UUID: $id" >&2; exit 1; }
done

room_id=$(uuidgen | tr '[:upper:]' '[:lower:]')
owner=$1
shift

sql="BEGIN;
INSERT INTO chats.room (id, name, type, created_at, updated_at)
  VALUES ('$room_id', 'quarkus-poc', 'group', now(), now());
INSERT INTO chats.subscription (user_id, room_id, owner, created_at, updated_at)
  VALUES ('$owner', '$room_id', true, now(), now());"
for member in "$@"; do
  sql+="
INSERT INTO chats.subscription (user_id, room_id, owner, created_at, updated_at)
  VALUES ('$member', '$room_id', false, now(), now());"
done
sql+="
COMMIT;"

"$KUBECTL" --context "$CONTEXT" -n "$NAMESPACE" exec -i "$POD" -c postgres -- \
  psql -v ON_ERROR_STOP=1 -U admin -d "$DB" -q <<<"$sql"
echo "$room_id"
