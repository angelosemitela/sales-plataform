# Build em dois estágios: a imagem final leva só o JRE + o JAR (sem Maven nem código-fonte).
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml checkstyle.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
# Não rodar como root dentro do container.
RUN useradd --system --uid 10001 sales
USER sales
COPY --from=build /app/target/sales-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
