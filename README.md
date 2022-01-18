# Carbonio Chats CE

This is the official repository for Carbonio chats CE.

## 🔧 How to Build

Build using maven:

```shell
mvn clean install
```

If you also want to generate artifacts, run
```shell
mvn clean install -P openapi-generation,build-artifacts
```

## 🚀 How to Run

With the generated fat-jar:

```shell
java -jar ./boot/target/zextras-chats-community-jar-with-dependencies.jar
```
