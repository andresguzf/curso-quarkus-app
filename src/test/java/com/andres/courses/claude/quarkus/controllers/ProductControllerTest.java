package com.andres.courses.claude.quarkus.controllers;

import static com.andres.courses.claude.quarkus.models.Product.NAME_MAX_LENGTH;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

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
import io.restassured.path.xml.XmlPath;
import io.restassured.specification.RequestSpecification;

/**
 * Pruebas del recurso REST {@link ProductController}.
 * <p>
 * Primero los casos felices en JSON, luego en XML (enviando y recibiendo en
 * cada formato), después las validaciones de cada campo ({@code name},
 * {@code price}, {@code quantity}) y, al final, los errores del request
 * (cuerpo mal formado, Content-Type y Accept no soportados y método no
 * permitido).
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductControllerTest {

    private static final String PRODUCTS = "/api/products";

    /** El mismo producto que en JSON, en XML: los campos son elementos hijos de la raíz. */
    private static final String PRODUCT_XML = """
            <Product>
                <name>Teclado</name>
                <price>49.99</price>
                <quantity>10</quantity>
            </Product>
            """;

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
    void crearProductoConLosValoresLimiteDevuelve201() {
        // nombre del largo máximo, el menor precio con 4 decimales y cantidad 0
        String name = "a".repeat(NAME_MAX_LENGTH);
        givenJson()
                .body(Map.of("name", name, "price", new BigDecimal("0.0001"), "quantity", 0))
                .when().post(PRODUCTS)
                .then()
                .statusCode(201)
                .body("name", is(name))
                .body("price", is(new BigDecimal("0.0001")))
                .body("quantity", is(0));
    }

    @Test
    @Order(4)
    void crearProductoSinAcceptRespondeEnJson() {
        // */* (lo que envía RestAssured por defecto): Quarkus usa el primero de @Produces
        given()
                .contentType(ContentType.JSON)
                .accept("*/*")
                .body(Map.of("name", "Teclado", "price", new BigDecimal("49.99"), "quantity", 10))
                .when().post(PRODUCTS)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("name", is("Teclado"));
    }

    @Test
    @Order(5)
    void crearProductoEnviandoXmlYRecibiendoXml() {
        XmlPath xml = given()
                .contentType(ContentType.XML)
                .accept(ContentType.XML)
                .body(PRODUCT_XML)
                .when().post(PRODUCTS)
                .then()
                .statusCode(201)
                .contentType(ContentType.XML)
                // un record se serializa con el nombre de su clase como raíz
                .body("Product.name", is("Teclado"))
                .body("Product.price", is("49.99"))
                .body("Product.quantity", is("10"))
                .extract().xmlPath();

        // exactamente name, price y quantity
        assertThat(xml.getList("Product.children().collect { it.name() }", String.class),
                containsInAnyOrder("name", "price", "quantity"));
    }

    @Test
    @Order(6)
    void crearProductoEnviandoXmlYRecibiendoJson() {
        givenJson()
                .contentType(ContentType.XML)
                .body(PRODUCT_XML)
                .when().post(PRODUCTS)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("$", aMapWithSize(3))
                .body("name", is("Teclado"))
                .body("price", is(new BigDecimal("49.99")))
                .body("quantity", is(10));
    }

    @Test
    @Order(7)
    void crearProductoEnviandoJsonYRecibiendoXml() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.XML)
                .body(Map.of("name", "Mouse", "price", new BigDecimal("15.5"), "quantity", 3))
                .when().post(PRODUCTS)
                .then()
                .statusCode(201)
                .contentType(ContentType.XML)
                .body("Product.name", is("Mouse"))
                .body("Product.price", is("15.5"))
                .body("Product.quantity", is("3"));
    }

    @Test
    @Order(8)
    void crearProductoEnXmlIgnoraElNombreDeLaRaizYLosElementosDesconocidos() {
        givenJson()
                .contentType(ContentType.XML)
                .body("""
                        <producto>
                            <name>Monitor</name>
                            <price>199.90</price>
                            <quantity>2</quantity>
                            <color>negro</color>
                        </producto>
                        """)
                .when().post(PRODUCTS)
                .then()
                .statusCode(201)
                .body("$", aMapWithSize(3))
                .body("name", is("Monitor"))
                .body("price", is(new BigDecimal("199.90")))
                .body("quantity", is(2));
    }

    @Test
    @Order(9)
    void crearProductoSinCuerpoDevuelve400() {
        givenJson()
                .when().post(PRODUCTS)
                .then()
                .statusCode(400)
                .body("$", aMapWithSize(1))
                .body("product", contains("El cuerpo del request es requerido: envía un producto"));
    }

    @Test
    @Order(10)
    void crearProductoSinCamposDevuelveTodasLasViolaciones() {
        givenJson()
                .body("{}")
                .when().post(PRODUCTS)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                // formato simple: cada campo con la lista de sus mensajes
                .body("$", aMapWithSize(3))
                .body("name", contains("El nombre es requerido"))
                .body("price", contains("El precio es requerido"))
                .body("quantity", contains("La cantidad es requerida"));
    }

    @Test
    @Order(11)
    void crearProductoConNameEnBlancoDevuelve400() {
        givenJson()
                .body(Map.of("name", "   ", "price", new BigDecimal("49.99"), "quantity", 10))
                .when().post(PRODUCTS)
                .then()
                .statusCode(400)
                .body("$", aMapWithSize(1))
                .body("name", contains("El nombre es requerido"));
    }

    @Test
    @Order(12)
    void crearProductoConNameDemasiadoLargoDevuelve400() {
        givenJson()
                .body(Map.of("name", "a".repeat(NAME_MAX_LENGTH + 1), "price", new BigDecimal("49.99"), "quantity", 10))
                .when().post(PRODUCTS)
                .then()
                .statusCode(400)
                .body("$", aMapWithSize(1))
                .body("name", contains("El nombre del producto admite hasta " + NAME_MAX_LENGTH + " caracteres"));
    }

    @Test
    @Order(13)
    void crearProductoConPriceCeroONegativoDevuelve400() {
        for (String price : new String[] { "0", "-10.50" }) {
            givenJson()
                    .body(Map.of("name", "Teclado", "price", new BigDecimal(price), "quantity", 10))
                    .when().post(PRODUCTS)
                    .then()
                    .statusCode(400)
                    .body("$", aMapWithSize(1))
                    .body("price", contains("El precio debe ser mayor que 0"));
        }
    }

    @Test
    @Order(14)
    void crearProductoConPriceConMasDeCuatroDecimalesDevuelve400() {
        givenJson()
                .body(Map.of("name", "Teclado", "price", new BigDecimal("49.99999"), "quantity", 10))
                .when().post(PRODUCTS)
                .then()
                .statusCode(400)
                .body("$", aMapWithSize(1))
                .body("price", contains("El precio admite hasta 4 decimales"));
    }

    @Test
    @Order(15)
    void crearProductoConQuantityNegativaDevuelve400() {
        givenJson()
                .body(Map.of("name", "Teclado", "price", new BigDecimal("49.99"), "quantity", -1))
                .when().post(PRODUCTS)
                .then()
                .statusCode(400)
                .body("$", aMapWithSize(1))
                .body("quantity", contains("La cantidad debe ser mayor o igual a 0"));
    }

    @Test
    @Order(16)
    void crearProductoEnXmlInvalidoDevuelveLasViolacionesEnXml() {
        // las mismas reglas aplican al XML; con Accept XML el reporte también sale en XML
        String body = given()
                .contentType(ContentType.XML)
                .accept(ContentType.XML)
                .body("<Product><name></name><price>-1</price><quantity>10</quantity></Product>")
                .when().post(PRODUCTS)
                .then()
                .statusCode(400)
                .contentType(ContentType.XML)
                .extract().asString();

        assertThat(body, containsString("<name>"));
        assertThat(body, containsString("El nombre es requerido"));
        assertThat(body, containsString("<price>"));
        assertThat(body, containsString("El precio debe ser mayor que 0"));
    }

    @Test
    @Order(17)
    void crearProductoConXmlVacioDevuelve400() {
        // sin contenido no hay XML que leer: el reader responde 400
        given()
                .contentType(ContentType.XML)
                .when().post(PRODUCTS)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(18)
    void crearProductoEnXmlConPriceQueNoEsUnNumeroDevuelve400() {
        given()
                .contentType(ContentType.XML)
                .body("<Product><name>Teclado</name><price>caro</price><quantity>10</quantity></Product>")
                .when().post(PRODUCTS)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(19)
    void crearProductoConXmlMalFormadoDevuelve400() {
        given()
                .contentType(ContentType.XML)
                .body("<Product><name>Teclado</name><price>")
                .when().post(PRODUCTS)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(20)
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
    @Order(21)
    void crearProductoConJsonMalFormadoDevuelve400() {
        givenJson()
                .body("{\"name\": \"Teclado\", \"price\": ")
                .when().post(PRODUCTS)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(22)
    void crearProductoConContentTypeNoSoportadoDevuelve415() {
        given()
                .contentType(ContentType.TEXT)
                .body("Teclado;49.99;10")
                .when().post(PRODUCTS)
                .then()
                .statusCode(415);
    }

    @Test
    @Order(23)
    void crearProductoConAcceptNoSoportadoDevuelve406() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.HTML)
                .body(Map.of("name", "Teclado", "price", new BigDecimal("49.99"), "quantity", 10))
                .when().post(PRODUCTS)
                .then()
                .statusCode(406);
    }

    @Test
    @Order(24)
    void metodoNoPermitidoEnProductsDevuelve405() {
        given()
                .when().get(PRODUCTS)
                .then()
                .statusCode(405);
    }
}
