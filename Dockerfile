# =========================================================
# Western Union Bank — multi-stage Docker build
# Stage 1: build the jar with Maven + JDK 21
# Stage 2: run it on a slim JRE 21 image
# =========================================================

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies separately from source changes
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---------------------------------------------------------

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S wu && adduser -S wu -G wu
COPY --from=build /build/target/westernunion-bank.jar app.jar
RUN chown wu:wu app.jar
USER wu

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
