# ---- Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S enlaza && adduser -S enlaza -G enlaza
WORKDIR /app

COPY --from=build /app/target/enlaza-*.jar app.jar

USER enlaza
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
