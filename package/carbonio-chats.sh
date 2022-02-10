#!/bin/bash

# SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

# decrypt the bootstrap token, asking the password to the sys admin
# --setup check for SETUP_CONSUL_TOKEN env. variable and uses it
# to avoid re-asking for the password
if [[ $(id -u) -ne 0 ]]; then
  echo "Please run as root"
  exit 1
fi

if [[ "$1" != "setup" ]]; then
  echo "Syntax: carbonio-storages <setup> to automatically setup the service"
  exit 1
fi

echo -n "insert the cluster credential password: "
export CONSUL_HTTP_TOKEN=$(service-discover bootstrap-token --setup)
EXIT_CODE="$?"
echo ""
if [[ "${EXIT_CODE}" != "0" ]]; then
  echo "cannot access to bootstrap token"
  exit 1;
fi
# limit secret visibility as much as possible
export -n SETUP_CONSUL_TOKEN

POLICY_NAME='carbonio-chats-policy'
POLICY_DESCRIPTION='Carbonio chats policy for service and sidecar proxy'
POLICY_RULES="$(cat <<EOF
"key_prefix" = {
  "carbonio-chats/" = {
    "policy" = "read"
  }
}
"node_prefix" = {
  "" = {
    "policy" = "read"
  }
}
"service" = {
  "carbonio-chats" = {
    "policy" = "write"
  }
  "carbonio-chats-sidecar-proxy" = {
    "policy" = "write"
  }
}
EOF
)"

# create or update policy for the specific service (this will be shared across cluster)
consul acl policy create -name "${POLICY_NAME}" -description "${POLICY_DESCRIPTION}" -rules "${POLICY_RULES}" >/dev/null 2>&1
if [[ "$?" != "0" ]]; then
    consul acl policy update -no-merge -name "${POLICY_NAME}" -description "${POLICY_DESCRIPTION}" -rules "${POLICY_RULES}"
    if [[ "$?" != "0" ]]; then
      echo "Setup failed: Cannot update policy for ${POLICY_NAME}"
      exit 1
    fi
fi

# declare the service as http
cat <<EOF | consul config write -
{
  "kind": "service-defaults",
  "name": "carbonio-chats",
  "protocol": "http"
}
EOF

if [[ ! -f "/etc/carbonio/chats/token" ]]; then
    # create the token
    consul acl token create -format json -policy-name "${POLICY_NAME}" -description "Token for carbonio-chats/$(hostname -A)" |
      jq -r '.SecretID' > /etc/carbonio/chats/token;
    chown carbonio-chats:carbonio-chats /etc/carbonio/chats/token
    chmod 0600 /etc/carbonio/chats/token

    # populate the config to allow configuration generation
    # something like: consul kv put carbonio-chats/resolution 128

    # to pass the token to consul-template we need to inject it to a env. variable
    # since it doesn't accept a file as an argument
    # !!IMPORTANT!!: make the consul token available to the application only if the
    # application is explicitly contacting consul (to fetch configuration or other stuff)
    mkdir -p /etc/systemd/system/carbonio-chats.service.d/
    cat >/etc/systemd/system/carbonio-chats.service.d/override.conf <<EOF
[Service]
Environment="CONSUL_HTTP_TOKEN=$(cat /etc/carbonio/chats/token)"
EOF
    chmod 0600 /etc/systemd/system/carbonio-chats.service.d/override.conf
    systemctl daemon-reload
fi

consul reload

# limit token visibility as much as possible
export -n CONSUL_HTTP_TOKEN

systemctl restart carbonio-chats.service
systemctl restart carbonio-chats-sidecar.service