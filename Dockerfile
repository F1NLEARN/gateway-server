FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

COPY gateway-server/gradle gradle
COPY gateway-server/gradlew gradlew
COPY gateway-server/build.gradle build.gradle
COPY gateway-server/settings.gradle settings.gradle
COPY gateway-server/src src

RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]