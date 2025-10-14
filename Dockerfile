# Dockerfile

# jdk17 Image Start
FROM openjdk:17

ARG JAR_FILE=build/libs/order-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} order_Backend.jar
ENTRYPOINT ["java","-jar","-Duser.timezone=Asia/Seoul","order_Backend.jar"]