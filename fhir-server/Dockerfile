FROM eclipse-temurin:21-jdk AS build

WORKDIR /src

# Wrapper and build scripts first: this layer changes rarely, so the Gradle distribution and the
# plugins stay cached while source edits rebuild only the layers below.
COPY gradle ./gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon --quiet help

COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar

EXPOSE 18083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
