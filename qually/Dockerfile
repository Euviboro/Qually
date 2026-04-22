# ── Stage 1: Build ────────────────────────────────────────────
# Use an official Maven image that includes Java 21 to compile and package the app.
FROM maven:3.9-eclipse-temurin-21 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven config first — Docker caches this layer separately.
# If only your source code changes (not pom.xml), Maven dependencies
# won't be re-downloaded on every build, making it much faster.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy the source code and build the JAR, skipping tests
# (tests would fail here because there's no running DB in the build environment).
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Run ──────────────────────────────────────────────
# Use a smaller image that only has Java (no Maven) to run the compiled JAR.
# This keeps the final image lean — the build tools aren't shipped to production.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy only the compiled JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# The port your Spring Boot app listens on
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]