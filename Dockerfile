FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/secure-banking-1.0.0.jar app.jar
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 appuser \
    && chown -R appuser:appuser /app
USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
