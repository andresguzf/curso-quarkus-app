# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

Quarkus 3.40 LTS (plataforma `3.40.1.1`), Java 25, Maven. Extensiones: `quarkus-rest-jackson` (Quarkus REST + Jackson; incompatible con `quarkus-resteasy` y sus dependientes) y `quarkus-smallrye-openapi` (incluye Swagger UI). Para respuestas XML se usa `jackson-dataformat-xml` (no JAXB, que no serializa `Map` ni `java.time`) con el writer `providers.XmlMessageBodyWriter` (un `Map` o un `ObjectNode` de Jackson salen con raíz `<response>`; un DTO, con el nombre de su clase); el método debe declarar `@Produces(MediaType.APPLICATION_XML)`, porque sin `@Produces` Quarkus REST lo trata como JSON. Para recibir XML en el cuerpo del request se usa el reader `providers.XmlMessageBodyReader` (Jackson XML; ignora el nombre de la raíz y los elementos desconocidos; XML inválido → 400), junto con `@Consumes({APPLICATION_JSON, APPLICATION_XML})`: el `Content-Type` elige el formato de entrada y el `Accept` el de salida, por separado (ejemplo: `POST /api/products`). Para content negotiation se declaran ambos tipos (`@Produces({APPLICATION_JSON, APPLICATION_XML})`) sin fijar `.type()` en el `RestResponse`: Quarkus elige según el `Accept` y, con `*/*`, usa el primero. Para PDF se usa la extensión Quarkiverse `quarkus-openpdf` (OpenPDF) con el writer `providers.GreetingPdfMessageBodyWriter` (`application/pdf`); ese writer va tipado como `MessageBodyWriter<Object>` y filtra en `isWriteable`, porque tipado como `<GreetingDto>` Quarkus REST no lo elige para un `RestResponse<GreetingDto>`. Para CSV (`text/csv`, sin dependencias) el writer `providers.UserCsvMessageBodyWriter` escribe solo los campos de `User` (`name,age,email`), una fila por usuario: acepta `Collection<User>`, `User` o `GreetingDto` (su `user`). El separador se configura en la clase con `DEFAULT_SEPARATOR` (`;` actualmente) o con el constructor `UserCsvMessageBodyWriter(char)`. Para Excel (`.xlsx`) se usa la extensión Quarkiverse `quarkus-poi` (Apache POI) con el writer `providers.UserExcelMessageBodyWriter`: mismo diseño que el CSV (columnas de `User`, una fila por usuario, mismos tipos aceptados); la edad va como celda numérica y el nombre de la hoja se configura con `DEFAULT_SHEET_NAME` (`Usuarios`) o con el constructor `UserExcelMessageBodyWriter(String)`. El query param `?format=json|xml|pdf|csv|xlsx` (`excel` es alias de `xlsx`) lo traduce al `Accept` el filtro `@PreMatching` `providers.FormatQueryParamFilter` (aplica a todos los endpoints; valor desconocido → 400): `.type()` en el `RestResponse` no sirve para eso, porque el serializador ya se eligió al negociar. Los parámetros del request (`@QueryParam`, `@HeaderParam`...) se validan con Bean Validation (`quarkus-hibernate-validator`): `@NotBlank`/`@NotNull`/`@Size`/`@Pattern` con `message` en español; si falla, Quarkus responde 400 con un `ViolationReport` (`title`, `status`, `violations[].field`/`message`) en JSON o XML según el `Accept`, sin validar a mano. Un DTO del cuerpo se valida con `@Valid` en el parámetro (y `@NotNull` si el cuerpo es requerido); `@Size`, `@Email`, `@PositiveOrZero` y las restricciones propias aceptan `null`, así que un campo requerido lleva además `@NotBlank`/`@NotNull`. Las restricciones personalizadas van en `validators`: anotación con `@Constraint(validatedBy = ...)` + su `ConstraintValidator` (ejemplo: `@Rut`/`RutValidator`, RUT chileno con dígito verificador módulo 11, en `UserDto`), sin registrarlas en ningún lado. No hay extensión de persistencia todavía: para usar `entities`/`repositories` con base de datos hay que añadir una (p. ej. Hibernate ORM con Panache + driver JDBC PostgreSQL).

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
- `providers` — `@Provider` de JAX-RS que extienden Quarkus REST (p. ej. serializadores para otros formatos)
- `validators` — restricciones personalizadas de Bean Validation (anotación + `ConstraintValidator`)

Todavía no hay services, repositories ni entities: los controllers responden directo. Flujo previsto: controller → service → repository → entity, con models (DTOs) en la frontera HTTP.

## Configuración

Todo en `src/main/resources/application.properties` (comentado en español): nombre/puertos, dev mode, metadatos OpenAPI, Swagger UI y logging (`DEBUG` para el package base). Usa prefijos de perfil `%dev.`/`%test.`/`%prod.` para valores específicos de entorno.

## Notas

- El artifactId es `1-quarkus-app`. code.quarkus.io no acepta artifactIds que empiecen por dígito, así que el proyecto se generó como `quarkus-app` y se renombró en `pom.xml`.
- Comentarios, docs y mensajes de commit en español.

## Available skills

- `saludar` (`.claude/skills/saludar/SKILL.md`): responde al saludo de forma amable con el día, la fecha, la hora y el clima de la zona local (tomada del sistema, sin datos personales), más un pensamiento célebre (p. ej. un proverbio chino) para motivar el día.
- `frontend-design` (`.claude/skills/frontend-design` → enlace a `.agents/skills/frontend-design`, instalado desde `anthropics/skills` según `skills-lock.json`): pautas de diseño visual para crear o rediseñar interfaces con una dirección estética propia (paleta, tipografía, layout) y no de plantilla.
- `skill-creator` (skill de usuario, `~/.claude/skills/skill-creator`): crea, mejora y evalúa skills (borrador → casos de prueba → comparación con línea base → revisión → iteración) y optimiza la `description` para que el skill se active cuando corresponde.

## Skills trigger rules

- **`saludar`**: invócalo con la herramienta Skill cada vez que el usuario salude, en cualquier forma: "hola", "hola, ¿qué tal?", "¿cómo estás?", "buenas", "buenos días", "buenas tardes", "buenas noches", "hey", "qué onda", "saludos", etc. Aplica aunque el saludo venga al inicio de otra petición: primero saluda con el skill y luego atiende la petición. No aplica cuando "hola"/"saludo" es parte del trabajo técnico (un endpoint `/hola`, un "Hola mundo", una clase `SaludoResource`).
- **`frontend-design`**: invócalo al construir o rediseñar cualquier interfaz web (páginas, componentes React u otro frontend, dashboards, landing pages, estilos/CSS) o cuando se pida mejorar su aspecto visual. No aplica a la API REST ni al backend Quarkus.
- **`skill-creator`**: invócalo siempre que se pida crear un skill nuevo, o modificar, mejorar, probar o revisar uno existente ("crea un skill…", "haz un skill que…", "mejora el skill X", "revisa este skill"). No escribas el `SKILL.md` a mano sin pasar por él. Los skills del proyecto van en `.claude/skills/<nombre>/`; su workspace de pruebas (`.claude/skills/<nombre>-workspace/`) está en `.gitignore`. Al terminar, registra el skill en `## Available skills` y su regla de activación en esta sección.
