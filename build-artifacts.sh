#!/bin/bash

# SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
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
    "rocky-8 | carbonio-chats-ce- | -1.el8.x86_64.rpm"
    "ubuntu  | carbonio-chats-ce_ | -1_amd64.deb     "
  )
  distro_found=false
  for distros_index in "${!distros[@]}"; do
    IFS=' | ' read -r -a distros_item <<<"${distros[distros_index]}"
    if [[ "$distro" == "${distros_item[0]}" || "$distro" == "" ]]; then
      distro_found=true
      print-banner "Building ${distros_item[0]} package"
      eval "build-${distros_item[0]}-artifact"

      if [[ "$distro" != "" && "$deploy_on" != "" ]]; then
        file_name="${distros_item[1]}$version${distros_item[2]}"
        print-banner "Publishing to $deploy_on"
        # uploading to the server
        echo "Uploading package ..."
        scp -o StrictHostKeyChecking=no -q "$artifacts_folder/${file_name}" "root@$deploy_on:$file_name"
        ret_val=$?
        if [ "$ret_val" -ne 0 ]; then
          echo "[ERROR] Uploading package failed !"
          exit 1
        fi
        echo "Uploading package done !"
        echo ""
        # installing the package in the server
        echo "Installing package ..."
        ssh -o StrictHostKeyChecking=no -q root@"$deploy_on" <<EOF
dpkg -i ${file_name}
rm ${file_name}
service carbonio-chats restart
service carbonio-chats-sidecar restart
EOF
        ret_val=$?
        if [ "$ret_val" -ne 0 ]; then
          echo "[ERROR] Installing package failed !"
          exit 1
        fi
        echo "Installing package done !"
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
    mkdir /tmp/chats
    cp * /tmp/chats
    cd /tmp/chats || exit
    pacur build ubuntu
  else
    docker run \
      --rm --entrypoint "" \
      -v "$(pwd)":/tmp/chats \
      -e VERSION="$1" \
      registry.dev.zextras.com/jenkins/pacur/ubuntu-18.04:v1 /bin/bash -c 'cd /tmp/chats && pacur build ubuntu'
  fi
}

function build-rocky-8-artifact() {
  if [ "$no_docker" = true ]; then
    mkdir /tmp/chats
    cp * /tmp/chats
    cd /tmp/chats || exit
    pacur build rocky-8
  else
    docker run \
      --rm --entrypoint "" \
      -v "$(pwd)":/tmp/chats \
      -e VERSION="$1" \
      registry.dev.zextras.com/jenkins/pacur/rocky-8:v1 /bin/bash -c 'cd /tmp/chats && pacur build rocky-8'
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
