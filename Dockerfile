# syntax=docker/dockerfile:1

FROM eclipse-temurin:26.0.2_10-jdk-noble@sha256:abe71c9b7140affd9a80d6ed313c428e3393d4d18c235823b1745670d4702053 AS build

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

FROM eclipse-temurin:26.0.2_10-jre-noble@sha256:0140636ef128fa014041fa12dae3a9de53b6a6c6f055271f4e6a7417ea34012c AS runtime

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
