#!/bin/bash

function change-project-version() {
  new_version="$1"
  if [[ "$new_version" = "" ]]; then
	  echo "Version can not be null"
	  exit 1
  fi
  IFS='.' read -r -a new_version_parts <<< "$new_version"

  if [[ ${#new_version_parts[@]} -ne 3 ]]; then
    echo "Version must have 3 parts"
    exit 1
  fi
  old_version=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive org.codehaus.mojo:exec-maven-plugin:1.6.0:exec)
  IFS='.' read -r -a old_version_parts <<< "$old_version"
  re='^[0-9]+$'
  i=0
  is_greater=false
  while [[ $i -lt 3 ]]; do
    if ! [[ ${new_version_parts[i]} =~ $re ]] ; then
      echo "Every version part must be a number"
      exit 1
    fi
    new_part=${new_version_parts[i]}
    old_part=${old_version_parts[i]}
    if [[ $((new_part)) -gt $((old_part)) ]]; then
      is_greater=true
    fi
    i=$((i+1))
  done

  if [[ $is_greater = false ]]; then
      echo "Version cannot be less or equals of the previous"
      exit 1
  fi

  old_version=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive org.codehaus.mojo:exec-maven-plugin:1.6.0:exec)

  mvn versions:set -DnewVersion="$new_version"
  mvn versions:commit

  sed "s/  version: $old_version/  version: $new_version/" "$chats_api_file" -i
  sed "s/  version: $old_version/  version: $new_version/" "$chats_internal_api_file" -i
}

function check-project-version() {
  maven_version=$1
  check-yaml-version "$maven_version" "$chats_api_file"
  if [[ $? -eq 1 ]]; then
    echo "The chats-api.yaml version is incorrect"
    exit 1
  fi
  if [[ $? -eq 2 ]]; then
    echo "The chats-api.yaml version not found"
    exit 1
  fi
  check-yaml-version "$maven_version" "$chats_internal_api_file"
  if [[ $? -eq 1 ]]; then
    echo "The chats-internal-api.yaml version is incorrect"
    exit 1
  fi
  if [[ $? -eq 2 ]]; then
    echo "The chats-internal-api.yaml version not found"
    exit 1
  fi
  exit 0
}

function check-yaml-version() {
  while read line; do
    str=${line##*( )}
    if [[ "$str" == version* ]]; then
      str=${str#version}
      str=${str#:}
      str=$(echo $str | sed -e 's/^[[:space:]]*//')
      if [[ "$str" == "$1" ]]; then
        exit 0
      else
        echo "The chats-api.yaml version is incorrect"
        exit 1
      fi
    fi
  done < "$2"
  exit 2
}

function main() {
  command=$1
  if [[ "$command" == "set" ]]; then
    change-project-version "$2"
    exit $?
  fi
  if [[ "$command" == "check" ]]; then
    check-project-version "$2"
    exit $?
  fi

  echo "Invalid command"
  echo "Usage: version COMMAND VERSION"
  echo "  COMMAND: [set, check]"
  echo "    SET sets a new project version passed as parameter"
  echo "    CHECK: check that the version is adequate to the one passed by parameter"
  exit 1
}
chats_api_file="carbonio-chats-ce-openapi/src/main/resources/openapi/chats-api.yaml"
chats_internal_api_file="carbonio-chats-ce-openapi/src/main/resources/openapi/chats-internal-api.yaml"
main "$1" "$2"