package com.andres.courses.claude.quarkus.controllers;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Pruebas de integración de {@link UserController}: reutiliza las pruebas
 * de {@link UserControllerTest}, pero contra el artefacto empaquetado
 * ({@code target/quarkus-app/quarkus-run.jar} o el ejecutable nativo).
 * <p>
 * Las ejecuta failsafe en la fase {@code verify}; se omiten por defecto
 * ({@code skipITs=true}).
 */
@QuarkusIntegrationTest
class UserControllerIT extends UserControllerTest {
}
