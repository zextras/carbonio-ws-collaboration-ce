docker run \
  --rm --entrypoint "" \
  -v "$(pwd)":/tmp/chats \
  -e VERSION="$1" \
  registry.dev.zextras.com/jenkins/pacur/ubuntu-18.04:v1 /bin/bash -c 'cd /tmp/chats && pacur build ubuntu' &&
docker run \
  --rm --entrypoint "" \
  -v "$(pwd)":/tmp/chats \
  -e VERSION="$1" \
  registry.dev.zextras.com/jenkins/pacur/centos-8:v1 /bin/bash -c 'cd /tmp/chats && pacur build centos'