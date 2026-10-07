# ---- Build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package && cp target/*.jar app.jar

# ---- Runtime ----
FROM eclipse-temurin:17-jre
ENV TZ=America/Mexico_City \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true" \
    PORT=8081 \
    APP_UPLOADS_DIR=/app/uploads \
    APP_QZ_CERT_PATH=/app/qz-keys/digital-certificate.txt \
    APP_QZ_KEY_PATH=/app/qz-keys/private-key.pem
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/uploads /app/qz-keys
COPY --from=build /build/app.jar app.jar
EXPOSE 8081
# Persistir en Coolify: /app/uploads (imagenes/comprobantes) y /app/qz-keys (llaves QZ Tray)
VOLUME ["/app/uploads", "/app/qz-keys"]
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
