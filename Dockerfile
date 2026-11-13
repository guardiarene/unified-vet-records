# syntax=docker/dockerfile:1

FROM eclipse-temurin:25.0.4.1_1-jdk-noble@sha256:a9c5ddd386f629518bd500ecfabf7d1e5d32d7ffed735d5c3b1604ae92c830f4 AS build

WORKDIR /build

COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./
COPY src/ src/

RUN chmod +x mvnw

# The local Maven repository is cached by BuildKit; tests are skipped because they need Docker.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp package -Dmaven.test.skip=true \
    && cp target/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25.0.4.1_1-jre-noble@sha256:ce8543ddd3070ccbeaac2106a74c9387ddeb8d47a1c43348345715c1ef245c7f AS runtime

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
