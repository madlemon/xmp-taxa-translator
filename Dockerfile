FROM eclipse-temurin:25-jdk-alpine

LABEL org.opencontainers.image.source=https://github.com/madlemon/xmp-taxa-translator
LABEL authors="Kevin Klocke"

WORKDIR /app
COPY build/libs/app.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
