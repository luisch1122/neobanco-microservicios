# syntax=docker/dockerfile:1

# ---------------------------------------------------------------
# Etapa 1: build - compila el proyecto con Maven Wrapper y Java 25
# ---------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copiamos primero solo lo necesario para resolver dependencias,
# aprovechando la cache de capas de Docker en builds posteriores.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked \
	--mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
	--mount=type=secret,id=github_token,env=GITHUB_TOKEN \
	--mount=type=secret,id=github_actor,env=GITHUB_ACTOR \
	chmod +x mvnw && ./mvnw -q -B dependency:go-offline
    

# Ahora copiamos el codigo fuente y empaquetamos el jar
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked \
	--mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
	--mount=type=secret,id=github_token,env=GITHUB_TOKEN \
	--mount=type=secret,id=github_actor,env=GITHUB_ACTOR \
	./mvnw -q -B clean package -DskipTests

# ---------------------------------------------------------------
# Etapa 2: runtime - imagen final, solo con el JRE y el jar generado
# ---------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Ejecutar como usuario sin privilegios, no como root
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
USER appuser

COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

# Puerto interno de la aplicacion (server.port en application.properties)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
