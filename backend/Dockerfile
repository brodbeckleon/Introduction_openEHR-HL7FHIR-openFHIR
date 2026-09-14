# The build context is the repository root, not backend/: build.gradle copies the FHIR Connect
# mappings out of ../openfhir-bootstrap, so they have to be reachable from here.
FROM eclipse-temurin:21-jdk AS build

WORKDIR /src

# Wrapper and build scripts first: this layer changes rarely, so the Gradle distribution and the
# plugins stay cached while source edits rebuild only the layers below.
COPY backend/gradle ./gradle
COPY backend/gradlew backend/settings.gradle backend/build.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon --quiet help

# /src/../openfhir-bootstrap is exactly where processResources looks for the mappings.
COPY openfhir-bootstrap /openfhir-bootstrap
COPY backend/src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar

EXPOSE 18080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
