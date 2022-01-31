#!/bin/bash

# SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

if [[ "$1" != "setup" ]]; then
  echo "Syntax: carbonio-chats <setup> to automatically setup the service"
  exit 1;
fi

# decrypt the bootstrap token, asking the password to the sys admin
# --setup check for SETUP_CONSUL_TOKEN env. variable and uses it
# to avoid re-asking for the password
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

POLICY_NAME='carbonio-chats-ce-policy'
POLICY_DESCRIPTION='Preview service policy for service and sidecar proxy'
POLICY_RULES="$(cat <<EOF
"key_prefix" = {
  "carbonio-chats-ce/" = {
    "policy" = "read"
  }
}
"node_prefix" = {
  "" = {
    "policy" = "read"
  }
}
"service" = {
  "carbonio-chats-ce" = {
    "policy" = "write"
  }
  "carbonio-chats-ce-sidecar-proxy" = {
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
  "name": "carbonio-chats-ce",
  "protocol": "http"
}
EOF

if [[ ! -f "/etc/zextras/carbonio-chats-ce/token" ]]; then
    # create the token
    consul acl token create -format json -policy-name "${POLICY_NAME}" -description "Token for carbonio-chats-ce/$(hostname -A)" |
      jq -r '.SecretID' > /etc/zextras/carbonio-chats-ce/token;
    chown carbonio-chats-ce:carbonio-chats-ce /etc/zextras/carbonio-chats-ce/token
    chmod 0600 /etc/zextras/carbonio-chats-ce/token

    # populate the config to allow configuration generation
    # something like: consul kv put carbonio-chats-ce/resolution 128

    # to pass the token to consul-template we need to inject it to a env. variable
    # since it doesn't accept a file as an argument
    # !!IMPORTANT!!: make the consul token available to the application only if the
    # application is explicitly contacting consul (to fetch configuration or other stuff)
    mkdir -p /etc/systemd/system/carbonio-chats-ce.service.d/
    cat >/etc/systemd/system/carbonio-chats-ce.service.d/override.conf <<EOF
[Service]
Environment="CONSUL_HTTP_TOKEN=$(cat /etc/zextras/carbonio-chats-ce/token)"
EOF
    chmod 0600 /etc/systemd/system/carbonio-chats-ce.service.d/override.conf
    systemctl daemon-reload
fi

consul reload

# limit token visibility as much as possible
export -n CONSUL_HTTP_TOKEN

systemctl restart carbonio-chats-ce.service
systemctl restart carbonio-chats-ce-sidecar.service