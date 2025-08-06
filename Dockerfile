FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

COPY rewardo-api.jar /app/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=production", "rewardo-api.jar"]