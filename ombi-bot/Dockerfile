FROM openjdk:8-jre-slim

ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/ombibot/ombibot.jar


ENTRYPOINT ["java", "-jar", "/usr/share/ombibot/ombibot.jar"]
