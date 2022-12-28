SPDX-License-Identifier: AGPL-3.0-only

Carbonio Chats-CE manages a chat server-side

Copyright (C) 2022 Zextras <https://www.zextras.com>

This program is free software: you can redistribute it and/or modify it
under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, version 3 only of the License.
This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see [Licenses - GNU Project - Free
Software Foundation](https://www.gnu.org/licenses/licenses.html
"https://www.gnu.org/licenses/licenses.html")

See [COPYING](COPYING.md) file for the project license details

# Carbonio Chats CE

This is the official repository for Carbonio chats CE.

## 🔧 How to Build

Build using maven:

```shell
mvn clean install
```

---
If you also want to generate all artifacts, run

```shell
mvn clean install -P artifacts
```

---
If you want to generate only the artifact for a specific distro

```shell
mvn clean install -P artifacts -D distro=<1>
```

Where

1. distro value is the distro name (ubuntu or rocky-8)

----
If you want to generate only the artifact for a specific distro and deploy it in
a test server

```shell
mvn clean install -P artifacts -D distro=<1> -D deployOn=<2> -D debugMode=<3>
```

Where

1. distro value is the distro name (ubuntu or rocky-8)
2. deployOn value is the domain name or IP of the server to deploy the artifact
3. debugMode is a boolean value to indicate whether the installed Chat
   environment should have the debug port active

There's also a `noDocker` option which will build artifacts using a local pacur
instance instead of using the docker image, which is useful for CI pipeline.

---

## 🚀 How to Run

With the generated fat-jar:

```shell
java -jar ./boot/target/zextras-chats-ce-fatjar.jar
```

## How to change project version
To change the project versions is recommended to use the script;
```shell
version.sh set <1>
```
Where:
1. is the desired version