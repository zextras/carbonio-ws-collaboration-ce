#!/bin/bash

echo "Provisioning startup"

MAILBOX_INSTANCE=$1
DOMAIN="demo$MAILBOX_INSTANCE.zextras.io"

zmprov cd "$DOMAIN"
zmprov ca "test$MAILBOX_INSTANCE@$DOMAIN" password
zmprov ca "admin$MAILBOX_INSTANCE@$DOMAIN" password zimbraIsAdminAccount TRUE

echo "Provisioning completed"
