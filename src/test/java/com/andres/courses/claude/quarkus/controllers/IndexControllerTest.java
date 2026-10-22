package com.andres.courses.claude.quarkus.controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

/**
 * Pruebas del recurso REST {@link IndexController}.
 * <p>
 * {@code @QuarkusTest} arranca la aplicación en el puerto de test (8081) y
 * RestAssured hace peticiones HTTP reales contra ella.
 * <p>
 * Las pruebas se ejecutan en el orden de los campos de la respuesta:
 * estructura, message, user, date y, al final, los casos de error.
 * El controller arma la respuesta con {@code Map.of}, que no garantiza el
 * orden de los campos en el JSON, así que ninguna prueba depende de él.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IndexControllerTest {

    private static final String GREETING = "/api/greeting";

    @Test
    @Order(1)
    void greetingDevuelve200YLosCamposEsperados() {
        Map<String, Object> json = given()
                .accept(ContentType.JSON)
                .when().get(GREETING)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().jsonPath().getMap("$");

        assertThat(json.keySet(), containsInAnyOrder("message", "user", "date"));
        assertThat(((Map<?, ?>) json.get("user")).keySet(), containsInAnyOrder("name", "age", "email"));
    }

    @Test
    @Order(2)
    void greetingDevuelveElMessage() {
        given()
                .when().get(GREETING)
                .then()
                .statusCode(200)
                .body("message", is("Hola mundo Quarkus!"));
    }

    @Test
    @Order(3)
    void greetingDevuelveElUser() {
        given()
                .when().get(GREETING)
                .then()
                .statusCode(200)
                .body("user", aMapWithSize(3))
                .body("user.name", is("Andres"))
                .body("user.age", is(40))
                .body("user.email", is("andres@correo.example"));
    }

    @Test
    @Order(4)
    void greetingDevuelveLaDateDelMomentoDeLaPeticion() {
        LocalDateTime antes = LocalDateTime.now();
        JsonPath json = given()
                .when().get(GREETING)
                .then()
                .statusCode(200)
                // LocalDateTime en ISO-8601, sin zona horaria
                .body("date", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?"))
                .extract().jsonPath();
        LocalDateTime despues = LocalDateTime.now();

        LocalDateTime date = LocalDateTime.parse(json.getString("date"));
        assertThat(date, greaterThanOrEqualTo(antes));
        assertThat(date, lessThanOrEqualTo(despues));
    }

    @Test
    @Order(5)
    void metodoNoPermitidoDevuelve405() {
        given()
                .when().post(GREETING)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(6)
    void rutaInexistenteDevuelve404() {
        given()
                .when().get("/api/no-existe")
                .then()
                .statusCode(404);
    }
}
