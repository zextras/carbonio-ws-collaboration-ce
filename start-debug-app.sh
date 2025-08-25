#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

java -Djava.net.preferIPv4Stack=true \
  -Xms1024m \
  -Xmx2048m \
  -XX:+UseZGC \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006 \
  -jar carbonio-ws-collaboration-boot/target/carbonio-ws-collaboration-fatjar.jar
