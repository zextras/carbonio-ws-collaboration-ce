#!/bin/bash

# SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

function build-all-artifacts() {
  version=$1
  artifacts_folder=$2
  no_docker=$3
  distro=$4
  deploy_on=$5

  declare -a distros=(
    #   "DISTRO  | NAME PRE VERSION   | NAME POST VERSION"
    "rocky-8 | carbonio-ws-collaboration- | -1.el8.x86_64.rpm"
    "ubuntu  | carbonio-ws-collaboration_ | -1_amd64.deb     "
  )
  distro_found=false
  for distros_index in "${!distros[@]}"; do
    IFS=' | ' read -r -a distros_item <<<"${distros[distros_index]}"
    if [[ "$distro" == "${distros_item[0]}" || "$distro" == "*" ]]; then
      distro_found=true
      print-banner "Building ${distros_item[0]} package"
      cp package/carbonio-ws-collaboration.service package/carbonio-ws-collaboration.original
      eval "build-${distros_item[0]}-artifact"
      cp package/carbonio-ws-collaboration.original package/carbonio-ws-collaboration.service
      rm package/carbonio-ws-collaboration.original
      if [[ "$distro" != "*" && "$deploy_on" != "*" ]]; then
        file_name="${distros_item[1]}$version${distros_item[2]}"
        print-banner "Publishing to $deploy_on"
        # uploading to the server
        echo "Uploading package ..."
        scp -o StrictHostKeyChecking=no -q "$artifacts_folder/${file_name}" "root@$deploy_on:"
        ret_val=$?
        if [ "$ret_val" -ne 0 ]; then
          echo "[ERROR] Uploading package failed !"
          exit 1
        fi
        echo "Uploading package done !"
        echo ""
        # installing the package in the server
        echo "Installing package ..."
        ssh root@"$deploy_on" /bin/bash << EOF
          dpkg -i ${file_name}
          rm -r ${file_name}
EOF
        ret_val=$?
        if [ "$ret_val" -ne 0 ]; then
          echo "[ERROR] Installing package failed !"
          exit 1
        fi
        echo "Installing package done"
        echo "Remember to enter the machine to run the pending-setups"
      fi
    fi
  done

  if [[ "$distro_found" == false ]]; then
    echo "[ERROR] Destination distro not found"
    exit 1
  fi

  print-banner "success"

}

function build-ubuntu-artifact() {
  if [ "$no_docker" = true ]; then
    mkdir /tmp/ws-collaboration
    mv * /tmp/ws-collaboration
    sudo pacur build ubuntu-focal /tmp/ws-collaboration
  else
    docker run \
      --rm --entrypoint "" \
      -v "$(pwd)":/tmp/ws-collaboration \
      -e VERSION="$1" \
      registry.dev.zextras.com/jenkins/pacur/ubuntu-20.04:v1 /bin/bash -c 'cd /tmp/ws-collaboration && pacur build ubuntu'
  fi
}

function build-rocky-8-artifact() {
  if [ "$no_docker" = true ]; then
    mkdir /tmp/ws-collaboration
    mv * /tmp/ws-collaboration
    sudo pacur build rocky-8 /tmp/ws-collaboration
  else
    docker run \
      --rm --entrypoint "" \
      -v "$(pwd)":/tmp/ws-collaboration \
      -e VERSION="$1" \
      registry.dev.zextras.com/jenkins/pacur/rocky-8:v1 /bin/bash -c 'cd /tmp/ws-collaboration && pacur build rocky-8'
  fi
}

function print-banner() {
  string_to_print=$1
  banner_string=""
  if [ ${#string_to_print} -lt 60 ]; then
    start_spaces=$((60 - ${#string_to_print}))
    start_spaces=$((start_spaces / 2))
    index=0
    while [ $index -lt $start_spaces ]; do
      banner_string="$banner_string "
      index=$((index + 1))
    done
    banner_string="$banner_string$string_to_print"
    index=$((index + ${#string_to_print}))
    while [ $index -lt 60 ]; do
      banner_string="$banner_string "
      index=$((index + 1))
    done
  else
    banner_string="$string_to_print"
  fi
  index=0
  border_string=""
  while [ $index -lt 72 ]; do
    border_string="$border_string*"
    index=$((index + 1))
  done
  echo ""
  echo "$border_string"
  echo "****  $banner_string  ****" | tr a-z A-Z
  echo "$border_string"
}

build-all-artifacts "$1" "$2" "$3" "$4" "$5"
