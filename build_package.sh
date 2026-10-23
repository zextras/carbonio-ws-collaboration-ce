#!/bin/bash

#
# SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only
#

#
# Build carbonio-ws-collaboration-ce native packages with yap.
#
# Usage:
#   ./build_package.sh [distro ...]
#
#   distro   One or more target distributions (default: ubuntu-jammy).
#            Supported: ubuntu-jammy ubuntu-focal rocky-8 rocky-9
#            Pass "all" to build every supported distribution.
#
# Environment knobs:
#   DOCKER=true      Build inside the prebuilt docker.io/m0rf30/yap-<distro>
#                    images instead of the locally installed `yap` binary
#                    (local yap is the default).
#   YAP_TAG=latest   Image tag for docker mode (default: latest). Local yap
#                    always uses :latest (yap resolves it internally).
#   PLATFORM=linux/amd64
#                    Container platform. Defaults to linux/amd64 because the
#                    PKGBUILD targets arch=('x86_64'). On Apple Silicon / arm64
#                    hosts this is REQUIRED: yap selects the apt/dnf mirror from
#                    the build-host arch, so an arm64 container would resolve
#                    the ports.ubuntu.com (non-Intel) mirror and fail to find
#                    amd64 packages ("aptrepo update fetched zero indexes").
#

set -euo pipefail

SUPPORTED=(ubuntu-jammy ubuntu-focal rocky-8 rocky-9)
DOCKER="${DOCKER:-false}"
YAP_TAG="${YAP_TAG:-latest}"
PLATFORM="${PLATFORM:-linux/amd64}"
IMAGE_ORG="docker.io/m0rf30/yap-"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${REPO_ROOT}"

log() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
err() { printf '\033[1;31mERROR:\033[0m %s\n' "$*" >&2; }

is_supported() {
  local candidate="$1" known
  for known in "${SUPPORTED[@]}"; do
    [[ "${candidate}" == "${known}" ]] && return 0
  done
  return 1
}

# --- Resolve target distributions -------------------------------------------
declare -a DISTROS=()
if [[ $# -eq 0 ]]; then
  DISTROS=(ubuntu-jammy)
elif [[ "$1" == "all" ]]; then
  DISTROS=("${SUPPORTED[@]}")
else
  DISTROS=("$@")
fi

for distro in "${DISTROS[@]}"; do
  if ! is_supported "${distro}"; then
    err "Unsupported distro '${distro}'. Supported: ${SUPPORTED[*]} (or 'all')."
    exit 1
  fi
done

# --- Tooling checks ---------------------------------------------------------
command -v docker >/dev/null 2>&1 || { err "docker not found on PATH."; exit 1; }
if [[ "${DOCKER}" != "true" ]]; then
  command -v yap >/dev/null 2>&1 || {
    err "yap not found on PATH. Install it (https://github.com/M0Rf30/yap) or re-run with DOCKER=true."
    exit 1
  }
fi

# --- Stage the fat JAR (once) -----------------------------------------------
log "Copying fat JAR into package/ ..."
shopt -s nullglob
jars=(carbonio-ws-collaboration-boot/target/carbonio-ws-collaboration-ce-*-fatjar.jar)
shopt -u nullglob
if [[ ${#jars[@]} -eq 0 ]]; then
  err "No fat JAR in carbonio-ws-collaboration-boot/target/. Run 'mvn clean install -Pprod' first."
  exit 1
elif [[ ${#jars[@]} -gt 1 ]]; then
  err "Expected exactly one fat JAR but found ${#jars[@]}: ${jars[*]}"
  exit 1
fi
cp "${jars[0]}" package/carbonio-ws-collaboration-ce.jar

# --- Build helpers ----------------------------------------------------------
# Always refresh the image so we build against the latest published one.
prepull() {
  local image="$1"
  log "Pulling ${image} (${PLATFORM}) ..."
  docker pull --platform "${PLATFORM}" "${image}"
}

build_with_local_yap() {
  local distro="$1"
  prepull "${IMAGE_ORG}${distro}:latest"
  log "Building '${distro}' with local yap (platform=${PLATFORM}) ..."
  # yap shells out to docker; DOCKER_DEFAULT_PLATFORM forces the dispatched
  # build container (and its pull) onto the target platform.
  DOCKER_DEFAULT_PLATFORM="${PLATFORM}" yap build "${distro}" "${REPO_ROOT}"
}

build_with_docker() {
  local distro="$1"
  local image="${IMAGE_ORG}${distro}:${YAP_TAG}"
  local tty=()
  [[ -t 1 ]] && tty=(-t)
  log "Building '${distro}' in ${image} (platform=${PLATFORM}) ..."
  docker run --rm -i "${tty[@]}" \
    --platform "${PLATFORM}" \
    --pull always \
    --entrypoint yap \
    -v "${REPO_ROOT}":/tmp/staging \
    "${image}" \
    build "${distro}" /tmp/staging
}

# yap writes to <root>/artifacts (yap.json "output"); move into a per-distro
# subdir so distros that share a package format (jammy/focal .deb) don't clash.
collect_artifacts() {
  local distro="$1"
  local dest="artifacts/${distro}"
  mkdir -p "${dest}"
  shopt -s nullglob
  local produced=(
    artifacts/*.deb artifacts/*.rpm artifacts/*.apk artifacts/*.pkg.tar.zst
    artifacts/*.cdx.* artifacts/*.spdx.*
  )
  shopt -u nullglob
  if [[ ${#produced[@]} -eq 0 ]]; then
    err "No package artifacts produced for '${distro}'."
    exit 1
  fi
  mv -f "${produced[@]}" "${dest}/"
  log "Artifacts for '${distro}' -> ${dest}/"
}

# --- Run --------------------------------------------------------------------
for distro in "${DISTROS[@]}"; do
  if [[ "${DOCKER}" == "true" ]]; then
    build_with_docker "${distro}"
  else
    build_with_local_yap "${distro}"
  fi
  collect_artifacts "${distro}"
done

log "Done. Built: ${DISTROS[*]}"
