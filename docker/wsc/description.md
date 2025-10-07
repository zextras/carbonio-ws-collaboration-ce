# Carbonio WSC image

## Initial Setup

First, clone the submodule project
[https://github.com/zextras/carbonio-base-dockerization](https://github.com/zextras/carbonio-base-dockerization)
and follow the provided instructions.

### MacOS Requirements

On macOS, set the default Docker platform to `linux/amd64`:

```bash
export DOCKER_DEFAULT_PLATFORM=linux/amd64
```

Then build the `carbonio-message-broker` service.

As a first approach, the override method was not used. Instead, two separate
docker-compose processes are launched, both attached to the same Docker network.

Due to network naming constraints, the wsc compose must be started first,
followed by the base compose.

From the root of the wsc project, you can run:

```bash
docker compose -f docker/docker-compose.yaml -f docker/docker-compose-local-dev.yaml up
```

or

```bash
docker compose -f docker/docker-compose.yaml watch

```

Then launch the base stack from the subfolder: : `docker/base/carbonio-base-dockerization/docker-compose-ce.yaml`:

```bash
docker compose -f docker-compose-ce.yaml up
```

## Running WSC

WSC can be launched in two modes:

### Editor-Integrated Mode (e.g. VSCode)

This mode allows you to work directly inside the container, enabling debugging
features from within the editor. To use this mode, specific plugins are
required:

```text
github.copilot
github.copilot-chat
oracle.oracle-java
redhat.java
shabirmean.ebean-enhancement
visualstudioexptteam.intellicode-api-usage-examples
visualstudioexptteam.vscodeintellicode
vscjava.vscode-gradle
vscjava.vscode-java-debug
vscjava.vscode-java-dependency
vscjava.vscode-java-pack
vscjava.vscode-java-test
vscjava.vscode-maven
wx-chevalier.google-java-format
```

You can install the required extensions either via the **Extensions UI** in VSCode or from the **command line**.

From command line, add the previous extensions in a text file and run in VSCode terminal:

```bash
cat extensions.txt | xargs -n 1 code --install-extension
```

You can modify files and restart the project in debug mode directly from the editor running inside the container.

### Docker Watch Mode

This mode leverages Docker's file
[watch](https://docs.docker.com/compose/how-tos/file-watch/) functionality. When
a project file changes, the corresponding container is automatically rebuilt and
restarted to reflect the update.
