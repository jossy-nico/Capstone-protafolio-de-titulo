# 1) build del jar
FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

# 2) runtime liviano
FROM eclipse-temurin:17-jre
WORKDIR /app

# copia el jar generado
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
