# --- Stage 1: Build the JAR inside Docker (No local Java/Maven needed) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and source code into container
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src

# Compile and package the application
RUN mvn -q package -DskipTests

# --- Stage 2: Run the built application ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the compiled JAR from Stage 1
COPY --from=build /app/target/assessment-service-1.0.0.jar app.jar

# Expose the application port
EXPOSE 8106

# Start the application
ENTRYPOINT ["java","-jar","app.jar"]
