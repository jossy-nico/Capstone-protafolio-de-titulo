# Construcci├│n reproducible usando el Gradle Wrapper del m├│dulo.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle* ./
COPY settings.gradle* ./
COPY src ./src

RUN chmod +x gradlew \
    && ./gradlew --no-daemon clean bootJar \
    && JAR="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)" \
    && test -n "$JAR" \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring spring

COPY --from=build --chown=spring:spring /workspace/app.jar /app/app.jar

USER spring:spring
EXPOSE 8484
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
