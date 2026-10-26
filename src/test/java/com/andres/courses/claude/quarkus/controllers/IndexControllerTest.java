package com.andres.courses.claude.quarkus.controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
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
import io.restassured.path.xml.XmlPath;

/**
 * Pruebas del recurso REST {@link IndexController}.
 * <p>
 * {@code @QuarkusTest} arranca la aplicación en el puerto de test (8081) y
 * RestAssured hace peticiones HTTP reales contra ella.
 * <p>
 * Las pruebas se ejecutan en orden: primero {@code /api/greeting}, en el
 * orden de sus campos (estructura, message, user, date); luego
 * {@code /api/user}, {@code /api/greeting-map} y, al final, los casos de
 * error. Ninguna prueba depende del orden de los campos en el JSON.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IndexControllerTest {

    private static final String GREETING = "/api/greeting";
    private static final String USER = "/api/user";
    private static final String GREETING_MAP = "/api/greeting-map";

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
    void userDevuelve200YElJsonCompleto() {
        Map<String, Object> json = given()
                .accept(ContentType.JSON)
                .when().get(USER)
                .then()
                // status y content type que fija el ResponseBuilder
                .statusCode(200)
                .statusLine(containsString("200 OK"))
                .header("Content-Type", is("application/json"))
                // exactamente name, age y email, con sus valores y tipos
                .body("$", aMapWithSize(3))
                .body("name", is("Andres"))
                .body("age", is(40))
                .body("email", is("andres@correo.example"))
                .extract().jsonPath().getMap("$");

        assertThat(json.keySet(), containsInAnyOrder("name", "age", "email"));
    }

    @Test
    @Order(6)
    void greetingMapDevuelve202YElXmlCompleto() {
        LocalDateTime antes = LocalDateTime.now();
        XmlPath xml = given()
                .accept(ContentType.XML)
                .when().get(GREETING_MAP)
                .then()
                // status, content type y cabecera propia que fija el ResponseBuilder
                .statusCode(202)
                .statusLine(containsString("202 Accepted"))
                .header("Content-Type", is("application/xml"))
                .header("X-send-message", is("Hola que tal este es un mensaje o data oculto en el header del response"))
                // en XML todos los valores llegan como texto
                .body("response.message", is("Hola mundo Quarkus!"))
                .body("response.user.name", is("Andres"))
                .body("response.user.age", is("40"))
                .body("response.user.email", is("andres@correo.example"))
                .body("response.date", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?"))
                .extract().xmlPath();
        LocalDateTime despues = LocalDateTime.now();

        // exactamente message, user y date en la raíz, y name, age y email en user
        assertThat(xml.getList("response.children().collect { it.name() }", String.class),
                containsInAnyOrder("message", "user", "date"));
        assertThat(xml.getList("response.user.children().collect { it.name() }", String.class),
                containsInAnyOrder("name", "age", "email"));

        LocalDateTime date = LocalDateTime.parse(xml.getString("response.date"));
        assertThat(date, greaterThanOrEqualTo(antes));
        assertThat(date, lessThanOrEqualTo(despues));
    }

    @Test
    @Order(7)
    void greetingMapSinXmlEnElAcceptDevuelve406() {
        given()
                .accept(ContentType.JSON)
                .when().get(GREETING_MAP)
                .then()
                .statusCode(406);
    }

    @Test
    @Order(8)
    void metodoNoPermitidoDevuelve405() {
        given()
                .when().post(GREETING)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(9)
    void metodoNoPermitidoEnUserDevuelve405() {
        given()
                .when().post(USER)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(10)
    void metodoNoPermitidoEnGreetingMapDevuelve405() {
        given()
                .when().post(GREETING_MAP)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(11)
    void rutaInexistenteDevuelve404() {
        given()
                .when().get("/api/no-existe")
                .then()
                .statusCode(404);
    }
}
