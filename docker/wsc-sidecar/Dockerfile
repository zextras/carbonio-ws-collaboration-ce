ARG SIDECAR_IMAGE=registry.dev.zextras.com/dev/carbonio-sidecar:main
FROM ${SIDECAR_IMAGE}

# Service registration
COPY package/carbonio-ws-collaboration-template.hcl /tmp/carbonio-ws-collaboration-template.hcl
RUN mkdir -p /consul/config && sed -e '/{{-/d' -e 's|{{ [$]service_id }}|00000000-0000-0000-0000-000000000000|' \
      /tmp/carbonio-ws-collaboration-template.hcl > /consul/config/carbonio-ws-collaboration.hcl \
    && rm /tmp/carbonio-ws-collaboration-template.hcl

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
