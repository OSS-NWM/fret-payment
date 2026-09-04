# Stage 1: build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

COPY src ./src
RUN ./mvnw -B -ntp -DskipTests package

# Stage 2: runtime
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /build/target/fret-payment-*.jar /app/fret-payment.jar

USER app
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/fret-payment.jar"]
