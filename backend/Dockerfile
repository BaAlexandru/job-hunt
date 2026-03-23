# Stage 1: Build the application
FROM eclipse-temurin:24-jdk-alpine AS builder
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY backend/ backend/
RUN chmod +x gradlew && ./gradlew :backend:bootJar --no-daemon

# Stage 2: Extract Spring Boot layers for optimal caching
FROM eclipse-temurin:24-jdk-alpine AS extractor
WORKDIR /extract
COPY --from=builder /build/backend/build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# Stage 3: Runtime image
FROM eclipse-temurin:24-jre-alpine AS runtime
WORKDIR /application

RUN apk update && apk upgrade --no-cache && \
    apk del --no-cache gnupg gnupg-dirmngr gnupg-gpgconf gnupg-keyboxd \
      gnupg-utils gnupg-wks-client gpg gpg-agent gpg-wks-server gpgsm gpgv 2>/dev/null; \
    rm -rf /var/cache/apk/* && \
    addgroup -S app && adduser -S app -G app

COPY --from=extractor --chown=app:app /extract/extracted/dependencies/ ./
COPY --from=extractor --chown=app:app /extract/extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=app:app /extract/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=app:app /extract/extracted/application/ ./

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseContainerSupport", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "application.jar"]
