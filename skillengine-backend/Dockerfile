# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies first (only re-downloaded when pom.xml changes)
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Now copy source and build
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as a non-root user
RUN useradd -ms /bin/bash spring
USER spring

COPY --from=build /app/target/*.jar app.jar

# Render injects PORT at runtime; application.yml already reads ${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]