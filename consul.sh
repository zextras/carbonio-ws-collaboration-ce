#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

docker run -d \
   --name=consul-server \
   -p 8500:8500 \
   -p 8600:8600/udp \
   -e CONSUL_BIND_INTERFACE=eth0 \
   hashicorp/consul:latest \
   agent -server -ui -node=server-1 -bootstrap-expect=1 -client=0.0.0.0
