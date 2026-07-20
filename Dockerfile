# Step 1: Build stage — compile the jar on the server
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar -x test

# Step 2: Run stage — install Stockfish and run the jar
FROM eclipse-temurin:17-jdk-jammy
RUN apt-get update && apt-get install -y stockfish && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]