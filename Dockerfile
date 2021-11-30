FROM openjdk:11-jdk

EXPOSE 8081:8081

RUN mkdir /app

COPY ./boot/target/zextras-chats-community-jar-with-dependencies.jar /app/

WORKDIR /app

CMD ["java -jar zextras-chats-community-jar-with-dependencies.jar"]