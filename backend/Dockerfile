FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:17-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
RUN mkdir /app/uploads && chown app:app /app/uploads
COPY --from=build /build/target/haritha-vastra-1.0.0.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
