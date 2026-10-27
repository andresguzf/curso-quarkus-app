package com.andres.courses.claude.quarkus.controllers;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Pruebas de integración de {@link ProductController}: reutiliza las pruebas
 * de {@link ProductControllerTest}, pero contra el artefacto empaquetado
 * ({@code target/quarkus-app/quarkus-run.jar} o el ejecutable nativo).
 * <p>
 * Las ejecuta failsafe en la fase {@code verify}; se omiten por defecto
 * ({@code skipITs=true}).
 */
@QuarkusIntegrationTest
class ProductControllerIT extends ProductControllerTest {
}
