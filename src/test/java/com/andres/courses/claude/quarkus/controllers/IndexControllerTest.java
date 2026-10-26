package com.andres.courses.claude.quarkus.controllers;

import static com.andres.courses.claude.quarkus.providers.UserCsvMessageBodyWriter.TEXT_CSV;
import static com.andres.courses.claude.quarkus.providers.GreetingPdfMessageBodyWriter.APPLICATION_PDF;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

/**
 * Pruebas del recurso REST {@link IndexController}.
 * <p>
 * {@code @QuarkusTest} arranca la aplicación en el puerto de test (8081) y
 * RestAssured hace peticiones HTTP reales contra ella.
 * <p>
 * Las pruebas se ejecutan en orden: primero {@code /api/greeting}, en el
 * orden de sus campos (estructura, message, user, date); luego
 * {@code /api/user}, {@code /api/greeting-map}, {@code /api/greeting-xml}
 * (JSON, XML, PDF y CSV, por header Accept y por query param format) y, al
 * final, los casos de error. Ninguna prueba depende del orden de los campos
 * en el JSON o el XML.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IndexControllerTest {

    private static final String GREETING = "/api/greeting";
    private static final String USER = "/api/user";
    private static final String GREETING_MAP = "/api/greeting-map";
    private static final String GREETING_XML = "/api/greeting-xml";

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
    void greetingMapDevuelve200YElXmlCompleto() {
        LocalDateTime antes = LocalDateTime.now();
        XmlPath xml = given()
                .accept(ContentType.XML)
                .when().get(GREETING_MAP)
                .then()
                // status y cabecera propia que fija el ResponseBuilder; el content type
                // sale de @Produces (Quarkus le añade ;charset=UTF-8)
                .statusCode(200)
                .statusLine(containsString("200 OK"))
                .contentType(ContentType.XML)
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
    void greetingXmlDevuelve200YElXmlCompleto() {
        LocalDateTime antes = LocalDateTime.now();
        XmlPath xml = given()
                .accept(ContentType.XML)
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .statusLine(containsString("200 OK"))
                .contentType(ContentType.XML)
                // el elemento raíz toma el nombre del DTO; en XML los valores llegan como texto
                .body("GreetingDto.message", is("Hola mundo Quarkus!"))
                .body("GreetingDto.user.name", is("Andres"))
                .body("GreetingDto.user.age", is("40"))
                .body("GreetingDto.user.email", is("andres@correo.example"))
                .body("GreetingDto.date", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?"))
                .extract().xmlPath();
        LocalDateTime despues = LocalDateTime.now();

        // exactamente message, user y date en la raíz, y name, age y email en user
        assertThat(xml.getList("GreetingDto.children().collect { it.name() }", String.class),
                containsInAnyOrder("message", "user", "date"));
        assertThat(xml.getList("GreetingDto.user.children().collect { it.name() }", String.class),
                containsInAnyOrder("name", "age", "email"));

        LocalDateTime date = LocalDateTime.parse(xml.getString("GreetingDto.date"));
        assertThat(date, greaterThanOrEqualTo(antes));
        assertThat(date, lessThanOrEqualTo(despues));
    }

    @Test
    @Order(9)
    void greetingXmlConAcceptJsonDevuelveElJsonCompleto() {
        Map<String, Object> json = given()
                .accept(ContentType.JSON)
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                // el mismo DTO que en XML, ahora con los tipos de JSON (age es número)
                .body("$", aMapWithSize(3))
                .body("message", is("Hola mundo Quarkus!"))
                .body("user", aMapWithSize(3))
                .body("user.name", is("Andres"))
                .body("user.age", is(40))
                .body("user.email", is("andres@correo.example"))
                .body("date", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?"))
                .extract().jsonPath().getMap("$");

        assertThat(json.keySet(), containsInAnyOrder("message", "user", "date"));
        assertThat(((Map<?, ?>) json.get("user")).keySet(), containsInAnyOrder("name", "age", "email"));
    }

    @Test
    @Order(10)
    void greetingXmlConCualquierAcceptDevuelveJsonPorDefecto() {
        // */* acepta todo: gana el primer tipo de @Produces (JSON)
        given()
                .accept(ContentType.ANY)
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("message", is("Hola mundo Quarkus!"));
    }

    @Test
    @Order(11)
    void greetingXmlRespetaLaPreferenciaDelAccept() {
        // con pesos q, gana el tipo que el cliente prefiere (XML), no el primero de @Produces
        given()
                .accept("application/xml;q=0.9, application/json;q=0.5")
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .contentType(containsString("application/xml"))
                .body("GreetingDto.message", is("Hola mundo Quarkus!"));
    }

    @Test
    @Order(12)
    void greetingXmlConAcceptNoSoportadoDevuelve406() {
        given()
                .accept(ContentType.HTML)
                .when().get(GREETING_XML)
                .then()
                .statusCode(406);
    }

    @Test
    @Order(13)
    void greetingXmlConFormatXmlDevuelveXmlAunqueElAcceptPidaJson() {
        // el query param manda sobre el header Accept
        XmlPath xml = given()
                .accept(ContentType.JSON)
                .queryParam("format", "xml")
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .contentType(ContentType.XML)
                .body("GreetingDto.message", is("Hola mundo Quarkus!"))
                .body("GreetingDto.user.name", is("Andres"))
                .body("GreetingDto.user.age", is("40"))
                .body("GreetingDto.user.email", is("andres@correo.example"))
                .extract().xmlPath();

        assertThat(xml.getList("GreetingDto.children().collect { it.name() }", String.class),
                containsInAnyOrder("message", "user", "date"));
    }

    @Test
    @Order(14)
    void greetingXmlConFormatJsonDevuelveJsonAunqueElAcceptPidaXml() {
        given()
                .accept(ContentType.XML)
                .queryParam("format", "json")
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", aMapWithSize(3))
                .body("message", is("Hola mundo Quarkus!"))
                .body("user.name", is("Andres"))
                .body("user.age", is(40))
                .body("user.email", is("andres@correo.example"));
    }

    @Test
    @Order(15)
    void greetingXmlConFormatEnMayusculasFunciona() {
        given()
                .queryParam("format", "XML")
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .contentType(ContentType.XML)
                .body("GreetingDto.message", is("Hola mundo Quarkus!"));
    }

    @Test
    @Order(16)
    void greetingXmlConFormatDesconocidoDevuelve400() {
        given()
                .queryParam("format", "yaml")
                .when().get(GREETING_XML)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(17)
    void formatQueElEndpointNoProduceDevuelve406() {
        // /api/greeting solo produce JSON: ?format=xml se traduce a Accept: application/xml
        given()
                .queryParam("format", "xml")
                .when().get(GREETING)
                .then()
                .statusCode(406);
    }

    @Test
    @Order(18)
    void greetingXmlConAcceptPdfDevuelveElPdfConLaTablaDelUser() throws IOException {
        byte[] pdf = given()
                .accept(APPLICATION_PDF)
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .contentType(APPLICATION_PDF)
                .header("Content-Disposition", is("inline; filename=\"greeting.pdf\""))
                .extract().asByteArray();

        // un PDF real (no el toString del DTO), de una página
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII), is("%PDF-"));
        String text = pdfText(pdf);

        // cabecera con el mensaje y tabla con los atributos y valores del user
        assertThat(text, containsString("Hola mundo Quarkus!"));
        assertThat(text, containsString("Atributo"));
        assertThat(text, containsString("Valor"));
        assertThat(text, containsString("Nombre (name)"));
        assertThat(text, containsString("Andres"));
        assertThat(text, containsString("Edad (age)"));
        assertThat(text, containsString("40 años"));
        assertThat(text, containsString("Correo (email)"));
        assertThat(text, containsString("andres@correo.example"));
        // descripción de la clase y pie con la fecha
        assertThat(text, containsString("¿Quién es User?"));
        assertThat(text, containsString("Generado el"));
    }

    @Test
    @Order(19)
    void greetingXmlConFormatPdfDevuelvePdfAunqueElAcceptPidaJson() throws IOException {
        byte[] pdf = given()
                .accept(ContentType.JSON)
                .queryParam("format", "pdf")
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .contentType(APPLICATION_PDF)
                .extract().asByteArray();

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII), is("%PDF-"));
        assertThat(pdfText(pdf), containsString("andres@correo.example"));
    }

    @Test
    @Order(20)
    void formatPdfEnUnEndpointSinPdfDevuelve406() {
        // /api/greeting-map solo produce XML
        given()
                .queryParam("format", "pdf")
                .when().get(GREETING_MAP)
                .then()
                .statusCode(406);
    }

    @Test
    @Order(21)
    void greetingXmlConAcceptCsvDevuelveElCsvDelUser() {
        String csv = given()
                .accept(TEXT_CSV)
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .contentType(TEXT_CSV)
                .header("Content-Disposition", is("attachment; filename=\"users.csv\""))
                .extract().asString();

        // solo los campos del User: cabecera y una fila, cada línea terminada en CRLF
        assertThat(csv, is("name,age,email\r\n"
                + "Andres,40,andres@correo.example\r\n"));
    }

    @Test
    @Order(22)
    void greetingXmlConFormatCsvDevuelveCsvAunqueElAcceptPidaJson() {
        given()
                .accept(ContentType.JSON)
                .queryParam("format", "csv")
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .contentType(TEXT_CSV)
                .body(is("name,age,email\r\nAndres,40,andres@correo.example\r\n"));
    }

    @Test
    @Order(23)
    void formatCsvEnUnEndpointSinCsvDevuelve406() {
        // /api/greeting-map solo produce XML
        given()
                .queryParam("format", "csv")
                .when().get(GREETING_MAP)
                .then()
                .statusCode(406);
    }

    @Test
    @Order(24)
    void metodoNoPermitidoDevuelve405() {
        given()
                .when().post(GREETING)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(25)
    void metodoNoPermitidoEnUserDevuelve405() {
        given()
                .when().post(USER)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(26)
    void metodoNoPermitidoEnGreetingMapDevuelve405() {
        given()
                .when().post(GREETING_MAP)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(27)
    void metodoNoPermitidoEnGreetingXmlDevuelve405() {
        given()
                .when().post(GREETING_XML)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(28)
    void rutaInexistenteDevuelve404() {
        given()
                .when().get("/api/no-existe")
                .then()
                .statusCode(404);
    }

    /** Texto de la primera (y única) página del PDF, con el extractor de OpenPDF. */
    private static String pdfText(byte[] pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        try {
            assertThat(reader.getNumberOfPages(), is(1));
            return new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
    }
}
