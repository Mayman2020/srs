# syntax=docker/dockerfile:1
# Build from repository root: docker build -f deploy/docker/backend/Dockerfile .
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY SRS_System_backend/pom.xml .
COPY SRS_System_backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd -r ac && useradd -r -g ac -u 10001 ac
WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar
RUN chown ac:ac /app/app.jar
USER ac
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
