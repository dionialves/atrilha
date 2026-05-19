# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
# Manifestos do frontend (Tailwind v4 via frontend-maven-plugin): pre-copiados
# para que o `npm ci` da fase generate-resources tenha o lockfile disponivel.
COPY package.json package-lock.json tailwind.config.js ./
COPY src ./src
RUN ./mvnw -B -q -DskipTests package \
    && cp target/*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S -G app app \
    && apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8084
ENV JAVA_TOOL_OPTIONS=""
ENV SPRING_PROFILES_ACTIVE=prod
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:8084/health || exit 1
ENTRYPOINT ["sh","-c","exec java $JAVA_TOOL_OPTIONS -jar /app/app.jar"]
