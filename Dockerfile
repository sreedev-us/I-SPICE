# Build stage
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
RUN chmod +x gradlew
RUN ./gradlew build -x test

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-root user and group
RUN addgroup --system spring && adduser --system --ingroup spring spring

# Create necessary directories and set ownership before switching users
RUN mkdir -p /app/logs /app/uploads && \
    chown -R spring:spring /app/logs /app/uploads

# Copy jar explicitly assigning ownership to the new user
COPY --from=build --chown=spring:spring /app/build/libs/*.jar app.jar

# Switch to the non-root user
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
