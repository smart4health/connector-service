FROM openjdk:17-jdk-slim

ARG JAR_FILE
ENV TZ Europe/Berlin
ENV D4L_FHIRPACKAGEDIRECTORY=/app/fhir-package/

WORKDIR /app

RUN useradd --system --user-group hmx
USER hmx

COPY fhir-package/ ${D4L_FHIRPACKAGEDIRECTORY}
COPY ${JAR_FILE} /app/app.jar

ENTRYPOINT java -jar app.jar