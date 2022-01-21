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
1. distro value is the distro name
----
If you want to generate only the artifact for a specific distro
and deploy it in a test server
```shell
mvn clean install -P artifacts -D distro=<1> -D deploy-on=<2>
```
Where
1. distro value is the distro name
2. deploy-on value is the domain name or IP of the server to deploy the artifact
---
## 🚀 How to Run
With the generated fat-jar:

```shell
java -jar ./boot/target/zextras-chats-community-jar-with-dependencies.jar
```
