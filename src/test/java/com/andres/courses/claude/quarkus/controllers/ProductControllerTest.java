package com.andres.courses.claude.quarkus.controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import io.restassured.specification.RequestSpecification;

/**
 * Pruebas del recurso REST {@link ProductController}.
 * <p>
 * Primero los casos felices y, al final, los errores del request (cuerpo mal
 * formado y método no permitido).
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductControllerTest {

    private static final String PRODUCTS = "/api/products";

    /** Request JSON que lee los decimales como BigDecimal (exactos, no float). */
    private static RequestSpecification givenJson() {
        return given()
                .config(RestAssured.config().jsonConfig(
                        JsonConfig.jsonConfig().numberReturnType(NumberReturnType.BIG_DECIMAL)))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    @Test
    @Order(1)
    void crearProductoDevuelve201ConElProducto() {
        givenJson()
                .body(Map.of("name", "Teclado", "price", new BigDecimal("49.99"), "quantity", 10))
                .when().post(PRODUCTS)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                // exactamente name, price y quantity
                .body("$", aMapWithSize(3))
                .body("name", is("Teclado"))
                // el precio sale como número exacto, no como texto
                .body("price", is(new BigDecimal("49.99")))
                .body("quantity", is(10));
    }

    @Test
    @Order(2)
    void crearProductoDesdeJsonEnTexto() {
        // el mismo cuerpo escrito a mano, como lo enviaría curl o Postman
        givenJson()
                .body("""
                        {"name": "Mouse", "price": 15.5, "quantity": 3}
                        """)
                .when().post(PRODUCTS)
                .then()
                .statusCode(201)
                .body("name", is("Mouse"))
                .body("price", is(new BigDecimal("15.5")))
                .body("quantity", is(3));
    }

    @Test
    @Order(3)
    void crearProductoSinCamposDevuelveLosCamposEnNull() {
        // todavía sin validación: los campos que faltan llegan como null
        givenJson()
                .body("{}")
                .when().post(PRODUCTS)
                .then()
                .statusCode(201)
                .body("$", aMapWithSize(3))
                .body("name", nullValue())
                .body("price", nullValue())
                .body("quantity", nullValue());
    }

    @Test
    @Order(4)
    void crearProductoConPriceQueNoEsUnNumeroDevuelve400() {
        // Jackson no puede convertir el texto a BigDecimal
        givenJson()
                .body("""
                        {"name": "Teclado", "price": "caro", "quantity": 10}
                        """)
                .when().post(PRODUCTS)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    void crearProductoConJsonMalFormadoDevuelve400() {
        givenJson()
                .body("{\"name\": \"Teclado\", \"price\": ")
                .when().post(PRODUCTS)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    void metodoNoPermitidoEnProductsDevuelve405() {
        given()
                .when().get(PRODUCTS)
                .then()
                .statusCode(405);
    }
}
