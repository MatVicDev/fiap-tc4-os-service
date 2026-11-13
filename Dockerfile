FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw .
COPY .mvn ./.mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Datadog APM: o agent Java instrumenta HTTP, JDBC e Spring AMQP e propaga o trace
# context nos headers das mensagens RabbitMQ — é o que liga os spans dos três
# microsserviços num único trace distribuído da Saga.
RUN wget -O dd-java-agent.jar https://dtdg.co/latest-java-tracer

RUN addgroup -S oficina && adduser -S oficina -G oficina
COPY --from=build /app/target/*.jar app.jar
RUN chown oficina:oficina app.jar dd-java-agent.jar
USER oficina

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-javaagent:/app/dd-java-agent.jar -XX:MaxRAMPercentage=75"
ENV DD_LOGS_INJECTION=true

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
