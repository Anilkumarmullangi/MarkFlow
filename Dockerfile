# Build the application and run its tests inside a reproducible Java 21/Maven image.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY frontend frontend
RUN mvn -B -f backend/pom.xml clean package

# Small runtime image; the frontend is packaged into Spring Boot's static resources.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/backend/target/markdown-to-html-converter-pro.jar /app/app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
