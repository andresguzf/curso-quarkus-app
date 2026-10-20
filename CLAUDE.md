# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

Quarkus 3.40 LTS (plataforma `3.40.1.1`), Java 25, Maven. Extensiones: `quarkus-rest-jackson` (Quarkus REST + Jackson; incompatible con `quarkus-resteasy` y sus dependientes) y `quarkus-smallrye-openapi` (incluye Swagger UI). No hay extensión de persistencia todavía: para usar `entities`/`repositories` con base de datos hay que añadir una (p. ej. Hibernate ORM con Panache + driver JDBC).

Maven no está instalado globalmente: usa siempre el wrapper `./mvnw`.

## Comandos

```bash
./mvnw quarkus:dev                         # dev mode: live coding/hot reload, Dev UI, debugger en :5005
./mvnw compile                             # compilar
./mvnw test                                # tests (@QuarkusTest arranca la app en el puerto 8081)
./mvnw test -Dtest=NombreTest              # un solo test
./mvnw test -Dtest=NombreTest#metodo       # un solo método
./mvnw package                             # target/quarkus-app/quarkus-run.jar (no es über-jar)
./mvnw package -Dquarkus.package.jar.type=uber-jar
./mvnw package -Dnative                    # nativo (añade -Dquarkus.native.container-build=true sin GraalVM)
```

Los tests de integración (failsafe) se omiten por defecto (`skipITs=true`); solo se activan con el perfil `native`.

En dev mode (puerto 8080):
- Swagger UI: `/q/swagger-ui` (también incluida en producción por `quarkus.swagger-ui.always-include=true`)
- OpenAPI: `/q/openapi`
- Dev UI: `/q/dev-ui`

## Arquitectura

Package base `com.andres.courses.claude.quarkus`, organizado por capas:

- `controllers` — recursos REST (JAX-RS/Jakarta REST) que exponen la API
- `models` — DTOs de entrada/salida de la API
- `services` — lógica de negocio; los controllers delegan aquí
- `repositories` — acceso a datos
- `entities` — entidades del dominio persistidas

Los packages solo contienen `package-info.java` por ahora; no hay código de aplicación. Flujo previsto: controller → service → repository → entity, con models (DTOs) en la frontera HTTP.

## Configuración

Todo en `src/main/resources/application.properties` (comentado en español): nombre/puertos, dev mode, metadatos OpenAPI, Swagger UI y logging (`DEBUG` para el package base). Usa prefijos de perfil `%dev.`/`%test.`/`%prod.` para valores específicos de entorno.

## Notas

- El artifactId es `1-quarkus-app`. code.quarkus.io no acepta artifactIds que empiecen por dígito, así que el proyecto se generó como `quarkus-app` y se renombró en `pom.xml`.
- Comentarios, docs y mensajes de commit en español.
