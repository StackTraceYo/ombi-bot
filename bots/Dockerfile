FROM openjdk:8-jre

ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/plexbot/plexbot.jar


ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/share/plexbot/plexbot.jar"]
