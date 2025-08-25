#!/usr/bin/env bash

#
# SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only
#

OS=${2:-"ubuntu-jammy"}
HOST=$1

TMP_FOLDER="/tmp/$(date +%s)"
PACKAGE_NAME="carbonio-ws-collaboration"
ARCHITECTURE_SUFFIX="amd64"

rsync -acz --progress --stats artifacts/"${OS}"/"${PACKAGE_NAME}"*_"${ARCHITECTURE_SUFFIX}".deb root@"${HOST}":"${TMP_FOLDER}"/
ssh root@"${HOST}" "dpkg -i '${TMP_FOLDER}'/"*.deb
ssh -t root@"${HOST}" "pending-setups -a"
echo ""
ssh -q root@"${HOST}" "/bin/bash" <<EOF
systemctl restart carbonio-ws-collaboration
EOF
