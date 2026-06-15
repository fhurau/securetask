FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src src
RUN mvn verify

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && mkdir -p /app/data/uploads \
    && chown -R app:app /app

COPY --from=build --chown=app:app /workspace/target/securetask-backend-*.jar app.jar

EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
