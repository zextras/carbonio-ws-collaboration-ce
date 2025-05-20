# SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

.PHONY: compile test copy_jar stop_service start_service deploy

VM_HOSTNAME=${HOST}-wrk.demo.zextras.io
WS_SERVICE_NAME=carbonio-ws-collaboration

compile:
	mvn clean install -D skipTests

test:
	mvn clean install

copy_jar:
	scp carbonio-ws-collaboration-boot/target/${WS_SERVICE_NAME}-fatjar.jar root@${VM_HOSTNAME}:/usr/share/carbonio/${WS_SERVICE_NAME}.jar

stop_service:
	ssh root@${VM_HOSTNAME} "systemctl stop ${WS_SERVICE_NAME}"

start_service:
	ssh root@${VM_HOSTNAME} "systemctl start ${WS_SERVICE_NAME}"

deploy: compile stop_service copy_jar start_service
