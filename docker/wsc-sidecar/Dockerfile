ARG SIDECAR_IMAGE=registry.dev.zextras.com/dev/carbonio-sidecar:main
FROM --platform=$BUILDPLATFORM docker.io/library/alpine:3.20 AS hcl
COPY package/carbonio-ws-collaboration-template.hcl /carbonio-ws-collaboration-template.hcl
RUN sed -E 's/\{\{ or \(env "[A-Z0-9_]+"\) "([^"]*)" \}\}/\1/g' \
    /carbonio-ws-collaboration-template.hcl > /carbonio-ws-collaboration.hcl \
    && ! grep -q '{{' /carbonio-ws-collaboration.hcl

FROM ${SIDECAR_IMAGE}

RUN useradd -r -U carbonio-ws-collaboration

# Service registration
COPY --from=hcl /carbonio-ws-collaboration.hcl /consul/config/

# Setup files
COPY package/service-protocol.json /etc/carbonio/ws-collaboration/service-discover/
COPY package/intentions.json       /etc/carbonio/ws-collaboration/service-discover/
COPY package/policies.json         /etc/carbonio/ws-collaboration/service-discover/

# Setup script
COPY --chmod=755 package/carbonio-ws-collaboration-setup /usr/local/bin/carbonio-ws-collaboration-setup

COPY --chmod=755 package/carbonio-ws-collaboration-configs /usr/local/bin/carbonio-ws-collaboration-configs
COPY --chmod=755 package/carbonio-ws-collaboration-configs-cleanup /usr/local/bin/carbonio-ws-collaboration-configs-cleanup

ENV SERVICE_NAME=carbonio-ws-collaboration
ENV SETUP_SCRIPT="carbonio-ws-collaboration-setup"
ENV TOKEN_FILE=/etc/carbonio/ws-collaboration/service-discover/token
