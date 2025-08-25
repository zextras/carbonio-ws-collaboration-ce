#!/bin/bash

#
# SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only
#

OS=${1:-"ubuntu-jammy"}

if [[ -z $OS ]]
then
  echo "Please provide an OS as argument: (ubuntu-jammy, rocky-8)"
  exit 1
fi

# Step 2: Copy the fat JAR to the package directory
echo "Copying fat JAR to package directory..."
cp carbonio-ws-collaboration-boot/target/carbonio-ws-collaboration-fatjar.jar package/

# Step 3: Run yap to build packages
if [[ $OS == "ubuntu-jammy" ]]
then
  docker run -it --rm \
    --entrypoint=yap \
    -v "$(pwd)"/artifacts/"$OS":/artifacts \
    -v "$(pwd)":/tmp/staging \
    docker.io/m0rf30/yap-ubuntu-jammy:1.8 \
    build ubuntu-jammy /tmp/staging
elif [[ $OS == "ubuntu-focal" ]]
then
  docker run -it --rm \
    --entrypoint=yap \
    -v "$(pwd)"/artifacts/"$OS":/artifacts \
    -v "$(pwd)":/tmp/staging \
    docker.io/m0rf30/yap-ubuntu-focal:1.8 \
    build ubuntu-focal /tmp/staging
elif [[ $OS == "rocky-8" ]]
then
  docker run -it --rm \
    --entrypoint=yap \
    -v "$(pwd)"/artifacts/"$OS":/artifacts \
    -v "$(pwd)":/tmp/staging \
    docker.io/m0rf30/yap-rocky-8:1.10 \
    build rocky-8 /tmp/staging
fi
