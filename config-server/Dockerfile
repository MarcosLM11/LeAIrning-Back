FROM bellsoft/liberica-runtime-container:jdk-all-25-musl AS build

WORKDIR /app

COPY config-server/pom.xml config-server/mvnw ./
COPY config-server/.mvn .mvn

RUN ./mvnw dependency:go-offline -B

COPY config-server/src src

RUN ./mvnw clean package -DskipTests -B && \
  jdeps --ignore-missing-deps \
    --print-module-deps \
    --multi-release 25 \
    target/*.jar > /tmp/modules.txt && \
  jlink --compress=zip-9  \
    --strip-debug \
    --strip-java-debug-attributes \
    --no-header-files --no-man-pages \
    --add-modules $(cat /tmp/modules.txt),java.desktop,java.instrument,java.management,java.naming,java.security.jgss,jdk.unsupported \
    --output /custom-jre

FROM bellsoft/alpaquita-linux-base:musl

RUN addgroup -S app && \
    adduser -S app -G app

WORKDIR /app

COPY --from=build --chown=app:app /custom-jre /opt/java
COPY --from=build --chown=app:app /app/target/*.jar app.jar
COPY --chown=app:app config-repo /app/config-repo

ENV PATH="/opt/java/bin:$PATH"
ENV CONFIG_REPO_PATH=file:/app/config-repo

USER app

EXPOSE 8888

HEALTHCHECK --interval=30s \
     --timeout=3s \
     --start-period=60s \
     --retries=3 \
     CMD wget -q --spider http://localhost:8888/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]