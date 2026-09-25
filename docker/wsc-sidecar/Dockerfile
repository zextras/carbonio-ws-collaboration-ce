FROM registry.dev.zextras.com/dev/carbonio-sidecar:devel

# Service registration
COPY package/carbonio-ws-collaboration.hcl /consul/config/

# Setup files
COPY package/service-protocol.json /etc/carbonio/ws-collaboration/service-discover/
COPY package/intentions.json       /etc/carbonio/ws-collaboration/service-discover/
COPY package/policies.json         /etc/carbonio/ws-collaboration/service-discover/

# Setup script
COPY --chmod=755 package/carbonio-ws-collaboration-setup /usr/local/bin/carbonio-ws-collaboration-setup

COPY --chmod=755 package/carbonio-ws-collaboration-configs /usr/local/bin/carbonio-ws-collaboration-configs

ENV SERVICE_NAME=carbonio-ws-collaboration
ENV SETUP_SCRIPT="carbonio-ws-collaboration-setup"
ENV TOKEN_FILE=/etc/carbonio/ws-collaboration/service-discover/token
