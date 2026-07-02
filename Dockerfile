FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn -B -DskipTests dependency:go-offline

COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

RUN mkdir -p /app/uploads && chown -R app:app /app

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
