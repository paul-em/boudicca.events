FROM docker.io/openjdk:17-alpine

EXPOSE 8080

COPY build/libs/publisher-html.jar /opt/boudicca-html/
WORKDIR /opt/boudicca-html/
CMD ["java", "-Dserver.port=8080", "-jar", "publisher-html.jar"]