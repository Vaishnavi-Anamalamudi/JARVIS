FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
RUN groupadd --system adaptivegateway && useradd --system --gid adaptivegateway adaptivegateway
COPY --from=build /workspace/target/adaptive-api-gateway-backend-0.1.0.jar app.jar
USER adaptivegateway

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
