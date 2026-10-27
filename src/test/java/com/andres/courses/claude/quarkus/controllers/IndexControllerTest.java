package com.andres.courses.claude.quarkus.controllers;

import static com.andres.courses.claude.quarkus.providers.UserCsvMessageBodyWriter.TEXT_CSV;
import static com.andres.courses.claude.quarkus.providers.UserExcelMessageBodyWriter.APPLICATION_XLSX;
import static com.andres.courses.claude.quarkus.providers.GreetingPdfMessageBodyWriter.APPLICATION_PDF;
import static com.andres.courses.claude.quarkus.controllers.IndexController.CLIENT_ID_HEADER;
import static com.andres.courses.claude.quarkus.controllers.IndexController.CLIENT_VERSION_HEADER;
import static com.andres.courses.claude.quarkus.controllers.IndexController.AGE_MAX;
import static com.andres.courses.claude.quarkus.controllers.IndexController.AGE_MIN;
import static com.andres.courses.claude.quarkus.controllers.IndexController.NAME_MAX_LENGTH;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import com.andres.courses.claude.quarkus.providers.UserCsvMessageBodyWriter;

/**
 * Pruebas del recurso REST {@link IndexController}.
 * <p>
 * {@code @QuarkusTest} arranca la aplicación en el puerto de test (8081) y
 * RestAssured hace peticiones HTTP reales contra ella.
 * <p>
 * Las pruebas se ejecutan en orden: primero {@code /api/greeting}, en el
 * orden de sus campos (estructura, message, user, date); luego
 * {@code /api/user}, {@code /api/greeting-map}, {@code /api/greeting-xml}
 * (JSON, XML, PDF, CSV y Excel, por header Accept y por query param
 * format), {@code /api/headers}, {@code /api/hello} y, al final, los casos
 * de error. Ninguna prueba depende del orden de los campos en el JSON o el XML.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IndexControllerTest {

    private static final String GREETING = "/api/greeting";
    private static final String USER = "/api/user";
    private static final String GREETING_MAP = "/api/greeting-map";
    private static final String GREETING_XML = "/api/greeting-xml";
    private static final String HEADERS = "/api/headers";
    private static final String HELLO = "/api/hello";

    /** birthDate válida (pasada, ISO-8601) para las pruebas de /api/hello. */
    private static final String BIRTH_DATE = "1985-10-26";

    /** CSV del user del saludo, con el separador configurado en UserCsvMessageBodyWriter. */
    private static final String EXPECTED_CSV = csvLine("name", "age", "email")
            + csvLine("Andres", "40", "andres@correo.example");

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
        assertThat(csv, is(EXPECTED_CSV));
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
                .body(is(EXPECTED_CSV));
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
    void greetingXmlConAcceptXlsxDevuelveElExcelDelUser() throws IOException {
        byte[] xlsx = given()
                .accept(APPLICATION_XLSX)
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .contentType(containsString(APPLICATION_XLSX))
                .header("Content-Disposition", is("attachment; filename=\"users.xlsx\""))
                .extract().asByteArray();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            // una hoja, solo con los campos del User: cabecera y una fila
            assertThat(workbook.getNumberOfSheets(), is(1));
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName(), is("Usuarios"));
            assertThat(sheet.getLastRowNum(), is(1));

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue(), is("name"));
            assertThat(header.getCell(1).getStringCellValue(), is("age"));
            assertThat(header.getCell(2).getStringCellValue(), is("email"));
            assertThat(header.getLastCellNum(), is((short) 3));

            Row user = sheet.getRow(1);
            assertThat(user.getCell(0).getStringCellValue(), is("Andres"));
            // la edad es numérica, no texto
            assertThat(user.getCell(1).getCellType(), is(CellType.NUMERIC));
            assertThat(user.getCell(1).getNumericCellValue(), is(40.0));
            assertThat(user.getCell(2).getStringCellValue(), is("andres@correo.example"));
        }
    }

    @Test
    @Order(25)
    void greetingXmlConFormatExcelDevuelveXlsxAunqueElAcceptPidaJson() throws IOException {
        byte[] xlsx = given()
                .accept(ContentType.JSON)
                .queryParam("format", "excel")
                .when().get(GREETING_XML)
                .then()
                .statusCode(200)
                .contentType(containsString(APPLICATION_XLSX))
                .extract().asByteArray();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue(),
                    is("andres@correo.example"));
        }
    }

    @Test
    @Order(26)
    void formatXlsxEnUnEndpointSinExcelDevuelve406() {
        // /api/greeting-map solo produce XML
        given()
                .queryParam("format", "xlsx")
                .when().get(GREETING_MAP)
                .then()
                .statusCode(406);
    }

    @Test
    @Order(27)
    void headersConAmbosHeadersDevuelveSusValoresEnElJson() {
        given()
                .accept("application/json")
                .header(CLIENT_ID_HEADER, "app-movil")
                .header(CLIENT_VERSION_HEADER, "2.1.0")
                .when().get(HEADERS)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", aMapWithSize(3))
                .body("clientId", is("app-movil"))
                .body("clientVersion", is("2.1.0"))
                // el header Accept que envió el cliente
                .body("contentTypeHeader", is("application/json"));
    }

    @Test
    @Order(28)
    void headersSinElOpcionalDevuelveClientVersionNull() {
        given()
                .header(CLIENT_ID_HEADER, "app-movil")
                .when().get(HEADERS)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                // el campo sale en el JSON, con valor null
                .body("$", aMapWithSize(3))
                .body("$", hasKey("clientVersion"))
                .body("clientId", is("app-movil"))
                .body("clientVersion", nullValue());
    }

    @Test
    @Order(29)
    void headersSinElRequeridoDevuelve400ConLaViolacion() {
        given()
                .accept("application/json")
                .header(CLIENT_VERSION_HEADER, "2.1.0")
                .when().get(HEADERS)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                // reporte de Hibernate Validator: qué parámetro falló y por qué
                .body("title", is("Constraint Violation"))
                .body("status", is(400))
                .body("violations", hasSize(1))
                .body("violations[0].field", is("headers.clientId"))
                .body("violations[0].message", is("El header X-Client-Id es requerido"));
    }

    @Test
    @Order(30)
    void headersConElRequeridoVacioDevuelve400() {
        // @NotBlank: tampoco vale un valor con solo espacios
        given()
                .header(CLIENT_ID_HEADER, "   ")
                .when().get(HEADERS)
                .then()
                .statusCode(400)
                .body("violations[0].field", is("headers.clientId"))
                .body("violations[0].message", is("El header X-Client-Id es requerido"));
    }

    @Test
    @Order(31)
    void headersConVersionInvalidaDevuelve400() {
        // el opcional es válido si no viene, pero si viene debe cumplir @Pattern
        given()
                .header(CLIENT_ID_HEADER, "app-movil")
                .header(CLIENT_VERSION_HEADER, "beta")
                .when().get(HEADERS)
                .then()
                .statusCode(400)
                .body("violations", hasSize(1))
                .body("violations[0].field", is("headers.clientVersion"))
                .body("violations[0].message",
                        is("El header X-Client-Version debe tener formato de versión, p. ej. 2.1.0"));
    }

    @Test
    @Order(32)
    void headersConVariasViolacionesLasDevuelveTodas() {
        given()
                .header(CLIENT_VERSION_HEADER, "beta")
                .when().get(HEADERS)
                .then()
                .statusCode(400)
                .body("violations", hasSize(2))
                .body("violations.field", containsInAnyOrder("headers.clientId", "headers.clientVersion"));
    }

    @Test
    @Order(33)
    void headersNoDistingueMayusculasEnElNombreDelHeader() {
        // en HTTP los nombres de los headers no distinguen mayúsculas
        given()
                .header("x-client-id", "web")
                .header("X-CLIENT-VERSION", "1.0")
                .when().get(HEADERS)
                .then()
                .statusCode(200)
                .body("clientId", is("web"))
                .body("clientVersion", is("1.0"));
    }

    @Test
    @Order(34)
    void headersConAcceptXmlDevuelveLosValoresEnXml() {
        given()
                .accept("application/xml")
                .header(CLIENT_ID_HEADER, "app-movil")
                .header(CLIENT_VERSION_HEADER, "2.1.0")
                .when().get(HEADERS)
                .then()
                .statusCode(200)
                .contentType(ContentType.XML)
                // el elemento raíz toma el nombre del DTO
                .body("ClientHeadersDto.clientId", is("app-movil"))
                .body("ClientHeadersDto.clientVersion", is("2.1.0"))
                .body("ClientHeadersDto.contentTypeHeader", is("application/xml"));
    }

    @Test
    @Order(35)
    void helloConNameYAgeSaludaEnJson() {
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .accept("application/json")
                .queryParam("name", "Ana")
                .queryParam("age", 40)
                .when().get(HELLO)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                // exactamente message, name, age y birthDate
                .body("$", aMapWithSize(4))
                .body("message", is("Hola, Ana!"))
                .body("name", is("Ana"))
                // age es un número en el JSON, no un texto
                .body("age", is(40))
                // la fecha va como texto ISO-8601
                .body("birthDate", is(BIRTH_DATE));
    }

    @Test
    @Order(36)
    void helloConAcceptXmlSaludaEnXml() {
        XmlPath xml = given()
                .queryParam("birthDate", BIRTH_DATE)
                .accept("application/xml")
                .queryParam("name", "Ana")
                .queryParam("age", 40)
                .when().get(HELLO)
                .then()
                .statusCode(200)
                .contentType(ContentType.XML)
                // un ObjectNode no tiene nombre de clase útil: la raíz es <response>
                .body("response.message", is("Hola, Ana!"))
                .body("response.name", is("Ana"))
                .body("response.age", is("40"))
                .body("response.birthDate", is(BIRTH_DATE))
                .extract().xmlPath();

        assertThat(xml.getList("response.children().collect { it.name() }", String.class),
                containsInAnyOrder("message", "name", "age", "birthDate"));
    }

    @Test
    @Order(37)
    void helloConFormatXmlSaludaEnXml() {
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .queryParam("name", "Ana")
                .queryParam("age", 40)
                .queryParam("format", "xml")
                .when().get(HELLO)
                .then()
                .statusCode(200)
                .contentType(ContentType.XML)
                .body("response.message", is("Hola, Ana!"));
    }

    @Test
    @Order(38)
    void helloSinNameSaludaAlMundo() {
        // @DefaultValue("Mundo"): sin name no falla @NotBlank
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .queryParam("age", 40)
                .when().get(HELLO)
                .then()
                .statusCode(200)
                .body("message", is("Hola, Mundo!"))
                .body("name", is("Mundo"));
    }

    @Test
    @Order(39)
    void helloConNameVacioDevuelve400() {
        // @DefaultValue solo aplica si name no viene; si viene vacío, falla @NotBlank
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .queryParam("name", "   ")
                .queryParam("age", 40)
                .when().get(HELLO)
                .then()
                .statusCode(400)
                .body("violations.field", hasItem("handler.name"))
                .body("violations.message", hasItem("El parámetro name es requerido"));
    }

    @Test
    @Order(40)
    void helloConNameDemasiadoLargoDevuelve400() {
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .queryParam("name", "a".repeat(NAME_MAX_LENGTH + 1))
                .queryParam("age", 40)
                .when().get(HELLO)
                .then()
                .statusCode(400)
                .body("violations", hasSize(1))
                .body("violations[0].field", is("handler.name"))
                .body("violations[0].message", is("El parámetro name debe tener entre 3 y " + NAME_MAX_LENGTH + " caracteres"));
    }

    @Test
    @Order(41)
    void helloConNameDemasiadoCortoDevuelve400() {
        // @Size(min = 3)
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .queryParam("name", "Al")
                .queryParam("age", 40)
                .when().get(HELLO)
                .then()
                .statusCode(400)
                .body("violations", hasSize(1))
                .body("violations[0].field", is("handler.name"))
                .body("violations[0].message", is("El parámetro name debe tener entre 3 y " + NAME_MAX_LENGTH + " caracteres"));
    }

    @Test
    @Order(42)
    void helloConNameDelLargoMaximoSaluda() {
        String name = "a".repeat(NAME_MAX_LENGTH);
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .queryParam("name", name)
                .queryParam("age", 40)
                .when().get(HELLO)
                .then()
                .statusCode(200)
                .body("name", is(name));
    }

    @Test
    @Order(43)
    void helloSinAgeDevuelve400() {
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .accept("application/json")
                .queryParam("name", "Ana")
                .when().get(HELLO)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("violations", hasSize(1))
                .body("violations[0].field", is("handler.age"))
                .body("violations[0].message", is("El parámetro age es requerido"));
    }

    @Test
    @Order(44)
    void helloConAgeVacioDevuelve400() {
        // ?age= sin valor llega como null al Integer
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .queryParam("name", "Ana")
                .queryParam("age", "")
                .when().get(HELLO)
                .then()
                .statusCode(400)
                .body("violations[0].message", is("El parámetro age es requerido"));
    }

    @Test
    @Order(45)
    void helloConAgeMenorAlMinimoDevuelve400() {
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .queryParam("name", "Ana")
                .queryParam("age", AGE_MIN - 1)
                .when().get(HELLO)
                .then()
                .statusCode(400)
                .body("violations[0].field", is("handler.age"))
                .body("violations[0].message", is("El parámetro age debe ser mayor o igual a " + AGE_MIN));
    }

    @Test
    @Order(46)
    void helloConAgeMayorAlMaximoDevuelve400() {
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .queryParam("name", "Ana")
                .queryParam("age", AGE_MAX + 1)
                .when().get(HELLO)
                .then()
                .statusCode(400)
                .body("violations[0].field", is("handler.age"))
                .body("violations[0].message", is("El parámetro age debe ser menor o igual a " + AGE_MAX));
    }

    @Test
    @Order(47)
    void helloConAgeEnLosLimitesSaluda() {
        for (int age : new int[] { AGE_MIN, AGE_MAX }) {
            given()
                    .queryParam("birthDate", BIRTH_DATE)
                .queryParam("birthDate", BIRTH_DATE)
                    .queryParam("name", "Ana")
                    .queryParam("age", age)
                    .when().get(HELLO)
                    .then()
                    .statusCode(200)
                    .body("age", is(age));
        }
    }

    @Test
    @Order(48)
    void helloConAgeQueNoEsUnNumeroEnteroDevuelve404() {
        // JAX-RS: si un query param no se puede convertir a su tipo, responde 404
        for (String age : new String[] { "abc", "40.5" }) {
            given()
                    .queryParam("birthDate", BIRTH_DATE)
                .queryParam("birthDate", BIRTH_DATE)
                    .queryParam("name", "Ana")
                    .queryParam("age", age)
                    .when().get(HELLO)
                    .then()
                    .statusCode(404);
        }
    }

    @Test
    @Order(49)
    void helloSinNameNiAgeConAcceptXmlDevuelveLaViolacionEnXml() {
        // el reporte de validación también respeta la content negotiation
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .accept("application/xml")
                .when().get(HELLO)
                .then()
                .statusCode(400)
                .contentType(ContentType.XML)
                .body("ViolationReport.status", is("400"))
                .body("ViolationReport.violations.violations.field", is("handler.age"))
                .body("ViolationReport.violations.violations.message", is("El parámetro age es requerido"));
    }

    @Test
    @Order(50)
    void helloSinBirthDateDevuelve400() {
        given()
                .queryParam("name", "Ana")
                .queryParam("age", 40)
                .when().get(HELLO)
                .then()
                .statusCode(400)
                .body("violations", hasSize(1))
                .body("violations[0].field", is("handler.birthDate"))
                .body("violations[0].message", is("El parámetro birthDate es requerido"));
    }

    @Test
    @Order(51)
    void helloConBirthDateVacioDevuelve400() {
        // ?birthDate= sin valor llega como null al LocalDate
        given()
                .queryParam("name", "Ana")
                .queryParam("age", 40)
                .queryParam("birthDate", "")
                .when().get(HELLO)
                .then()
                .statusCode(400)
                .body("violations[0].message", is("El parámetro birthDate es requerido"));
    }

    @Test
    @Order(52)
    void helloConBirthDateDeHoyOFuturaDevuelve400() {
        // @Past exige una fecha estrictamente anterior a hoy
        LocalDate today = LocalDate.now();
        for (LocalDate date : new LocalDate[] { today, today.plusDays(1), today.plusYears(1) }) {
            given()
                    .queryParam("name", "Ana")
                    .queryParam("age", 40)
                    .queryParam("birthDate", date.toString())
                    .when().get(HELLO)
                    .then()
                    .statusCode(400)
                    .body("violations[0].field", is("handler.birthDate"))
                    .body("violations[0].message", is("El parámetro birthDate debe ser una fecha pasada"));
        }
    }

    @Test
    @Order(53)
    void helloConBirthDateDeAyerSaluda() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        given()
                .queryParam("name", "Ana")
                .queryParam("age", 0)
                .queryParam("birthDate", yesterday)
                .when().get(HELLO)
                .then()
                .statusCode(200)
                .body("birthDate", is(yesterday));
    }

    @Test
    @Order(54)
    void helloAceptaUn29DeFebreroDeAnioBisiesto() {
        given()
                .queryParam("name", "Ana")
                .queryParam("age", 40)
                .queryParam("birthDate", "2024-02-29")
                .when().get(HELLO)
                .then()
                .statusCode(200)
                .body("birthDate", is("2024-02-29"));
    }

    @Test
    @Order(55)
    void helloConBirthDateQueNoEsUnaFechaIsoValidaDevuelve404() {
        // igual que age: si el texto no se puede convertir a LocalDate, JAX-RS responde 404
        for (String date : new String[] { "2026-13-45", "2025-02-29", "26/10/1985", "ayer" }) {
            given()
                    .queryParam("name", "Ana")
                    .queryParam("age", 40)
                    .queryParam("birthDate", date)
                    .when().get(HELLO)
                    .then()
                    .statusCode(404);
        }
    }

    @Test
    @Order(56)
    void helloRespetaLosAcentos() {
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .queryParam("name", "José")
                .queryParam("age", 40)
                .when().get(HELLO)
                .then()
                .statusCode(200)
                .body("message", is("Hola, José!"))
                .body("name", is("José"));
    }

    @Test
    @Order(57)
    void helloConAcceptNoSoportadoDevuelve406() {
        given()
                .queryParam("birthDate", BIRTH_DATE)
                .accept(ContentType.HTML)
                .queryParam("name", "Ana")
                .queryParam("age", 40)
                .when().get(HELLO)
                .then()
                .statusCode(406);
    }

    @Test
    @Order(58)
    void metodoNoPermitidoDevuelve405() {
        given()
                .when().post(GREETING)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(59)
    void metodoNoPermitidoEnUserDevuelve405() {
        given()
                .when().post(USER)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(60)
    void metodoNoPermitidoEnGreetingMapDevuelve405() {
        given()
                .when().post(GREETING_MAP)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(61)
    void metodoNoPermitidoEnGreetingXmlDevuelve405() {
        given()
                .when().post(GREETING_XML)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(62)
    void metodoNoPermitidoEnHeadersDevuelve405() {
        given()
                .header(CLIENT_ID_HEADER, "app-movil")
                .when().post(HEADERS)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(63)
    void metodoNoPermitidoEnHelloDevuelve405() {
        given()
                .when().post(HELLO)
                .then()
                .statusCode(405);
    }

    @Test
    @Order(64)
    void rutaInexistenteDevuelve404() {
        given()
                .when().get("/api/no-existe")
                .then()
                .statusCode(404);
    }

    private static String csvLine(String... values) {
        return String.join(String.valueOf(UserCsvMessageBodyWriter.DEFAULT_SEPARATOR), values) + "\r\n";
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
