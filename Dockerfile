# Build stage
FROM eclipse-temurin:23-jdk AS build

# Set the working directory inside the container
WORKDIR /app

# Gradle
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# Copy Gradle configuration and source files
COPY build.gradle settings.gradle ./
COPY src src

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

RUN ./gradlew clean build --no-daemon

# Package stage
FROM eclipse-temurin:23-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the built .jar file from the build stage
COPY --from=build /app/build/libs/*-SNAPSHOT.jar /app/app.jar

# Define the entry point for the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]