ARG SIDECAR_IMAGE=registry.dev.zextras.com/dev/carbonio-sidecar:main
FROM --platform=$BUILDPLATFORM docker.io/library/alpine:3.23 AS consul-template
ARG TARGETARCH
ARG CONSUL_TEMPLATE_VERSION=0.42.1
RUN apk add --no-cache curl unzip && \
    curl -fsSL -o /tmp/ct.zip "https://releases.hashicorp.com/consul-template/${CONSUL_TEMPLATE_VERSION}/consul-template_${CONSUL_TEMPLATE_VERSION}_linux_${TARGETARCH}.zip" && \
    curl -fsSL -o /tmp/ct.sums "https://releases.hashicorp.com/consul-template/${CONSUL_TEMPLATE_VERSION}/consul-template_${CONSUL_TEMPLATE_VERSION}_SHA256SUMS" && \
    echo "$(grep "_linux_${TARGETARCH}.zip" /tmp/ct.sums | cut -d' ' -f1)  /tmp/ct.zip" | sha256sum -c - && \
    unzip -j /tmp/ct.zip consul-template -d /out && \
    chmod +x /out/consul-template

FROM ${SIDECAR_IMAGE}

RUN useradd -r -U carbonio-ws-collaboration

# Service registration
COPY --from=consul-template /out/consul-template /usr/local/bin/consul-template
COPY package/carbonio-ws-collaboration-template.hcl /consul/templates/carbonio-ws-collaboration-template.hcl

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

COPY --chmod=755 docker/wsc-sidecar/carbonio-ws-collaboration-hcl-entrypoint /usr/local/bin/carbonio-ws-collaboration-hcl-entrypoint
ENTRYPOINT ["carbonio-ws-collaboration-hcl-entrypoint"]
