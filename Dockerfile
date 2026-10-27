# syntax=docker/dockerfile:1

FROM eclipse-temurin:25.0.4.1_1-jdk-noble@sha256:ba0f0e1a33864105e1d454e88105c617ab40f71062db1f306bf58be32a3f8e5e AS build

WORKDIR /build

COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./
COPY src/ src/

RUN chmod +x mvnw

# The local Maven repository is cached by BuildKit; tests are skipped because they need Docker.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp package -Dmaven.test.skip=true \
    && cp target/psygcv-0.0.1-SNAPSHOT.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25.0.4.1_1-jre-noble@sha256:3f2223fe04b750aa5a8179e29b5c9a1ddf7665fc9bb08bfe11ad01d143bc13a5 AS runtime

RUN groupadd --gid 1001 app \
    && useradd --uid 1001 --gid app --home-dir /application \
       --shell /usr/sbin/nologin app

WORKDIR /application

COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

# The heap is sized from the container limit; override JAVA_TOOL_OPTIONS if needed.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080

USER app

ENTRYPOINT ["java", "-jar", "application.jar"]
