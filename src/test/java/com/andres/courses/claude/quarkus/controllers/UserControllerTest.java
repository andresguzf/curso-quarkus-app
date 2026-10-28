package com.andres.courses.claude.quarkus.controllers;

import static com.andres.courses.claude.quarkus.models.UserDto.NAME_MAX_LENGTH;
import static com.andres.courses.claude.quarkus.models.UserDto.NAME_MIN_LENGTH;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.xml.XmlPath;
import io.restassured.specification.RequestSpecification;

/**
 * Pruebas del recurso REST {@link UserController}.
 * <p>
 * Primero los casos felices (JSON, XML y valores límite), luego las
 * validaciones de cada campo ({@code name}, {@code lastName}, {@code email},
 * {@code age}) y, al final, los errores del request.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerTest {

    private static final String USERS = "/api/users";

    /** Usuario válido: cada prueba cambia solo el campo que necesita. */
    private static Map<String, Object> user(Object name, Object lastName, Object email, Object age) {
        // HashMap y no Map.of: algunas pruebas envían valores null
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("lastName", lastName);
        user.put("email", email);
        user.put("age", age);
        return user;
    }

    private static RequestSpecification givenJson() {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    @Test
    @Order(1)
    void crearUsuarioDevuelve201ConElUsuario() {
        givenJson()
                .body(user("Ana", "Pérez", "ana@correo.example", 30))
                .when().post(USERS)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                // exactamente name, lastName, email y age
                .body("$", aMapWithSize(4))
                .body("name", is("Ana"))
                .body("lastName", is("Pérez"))
                .body("email", is("ana@correo.example"))
                .body("age", is(30));
    }

    @Test
    @Order(2)
    void crearUsuarioEnviandoXmlYRecibiendoXml() {
        XmlPath xml = given()
                .contentType(ContentType.XML)
                .accept(ContentType.XML)
                .body("""
                        <user>
                            <name>Ana</name>
                            <lastName>Pérez</lastName>
                            <email>ana@correo.example</email>
                            <age>30</age>
                        </user>
                        """)
                .when().post(USERS)
                .then()
                .statusCode(201)
                .contentType(ContentType.XML)
                // un record se serializa con el nombre de su clase como raíz
                .body("UserDto.name", is("Ana"))
                .body("UserDto.lastName", is("Pérez"))
                .body("UserDto.email", is("ana@correo.example"))
                .body("UserDto.age", is("30"))
                .extract().xmlPath();

        assertThat(xml.getList("UserDto.children().collect { it.name() }", String.class),
                containsInAnyOrder("name", "lastName", "email", "age"));
    }

    @Test
    @Order(3)
    void crearUsuarioEnXmlRespetaElCharsetDelContentType() {
        // el mismo XML con acentos en UTF-8 y en ISO-8859-1: el reader usa el charset declarado
        for (String charset : new String[] { "UTF-8", "ISO-8859-1" }) {
            given()
                    .contentType("application/xml; charset=" + charset)
                    .accept(ContentType.JSON)
                    .body("<user><name>José</name><lastName>Núñez</lastName><email>jose@correo.example</email><age>30</age></user>")
                    .when().post(USERS)
                    .then()
                    .statusCode(201)
                    .body("name", is("José"))
                    .body("lastName", is("Núñez"));
        }
    }

    @Test
    @Order(4)
    void crearUsuarioConLosValoresLimiteDevuelve201() {
        for (int length : new int[] { NAME_MIN_LENGTH, NAME_MAX_LENGTH }) {
            String text = "a".repeat(length);
            givenJson()
                    .body(user(text, text, "ana@correo.example", 0))
                    .when().post(USERS)
                    .then()
                    .statusCode(201)
                    .body("name", is(text))
                    .body("lastName", is(text))
                    .body("age", is(0));
        }
    }

    @Test
    @Order(5)
    void crearUsuarioSinCuerpoDevuelve400() {
        givenJson()
                .when().post(USERS)
                .then()
                .statusCode(400)
                .body("violations", hasSize(1))
                .body("violations[0].field", is("create.user"))
                .body("violations[0].message", is("El cuerpo del request es requerido: envía un usuario"));
    }

    @Test
    @Order(6)
    void crearUsuarioSinNameDevuelve400() {
        for (String name : new String[] { null, "   " }) {
            givenJson()
                    .body(user(name, "Pérez", "ana@correo.example", 30))
                    .when().post(USERS)
                    .then()
                    .statusCode(400)
                    .body("violations", hasSize(1))
                    .body("violations[0].field", is("create.user.name"))
                    .body("violations[0].message", is("El nombre es requerido"));
        }
    }

    @Test
    @Order(7)
    void crearUsuarioSoloConNameDevuelve400ConLosCamposRequeridos() {
        // lastName, email y age en null: @Size, @Email y @PositiveOrZero no lo detectan,
        // @NotBlank y @NotNull sí
        givenJson()
                .body(user("Andres", null, null, null))
                .when().post(USERS)
                .then()
                .statusCode(400)
                .body("violations", hasSize(3))
                .body("violations.field", containsInAnyOrder(
                        "create.user.lastName", "create.user.email", "create.user.age"))
                .body("violations.message", containsInAnyOrder(
                        "El apellido es requerido", "El email es requerido", "La edad es requerida"));
        // lo mismo si los campos ni siquiera vienen en el JSON
        givenJson()
                .body("""
                        {"name": "Andres"}
                        """)
                .when().post(USERS)
                .then()
                .statusCode(400)
                .body("violations", hasSize(3));
    }

    @Test
    @Order(8)
    void crearUsuarioConLastNameOEmailEnBlancoDevuelve400() {
        givenJson()
                .body(user("Ana", "   ", "ana@correo.example", 30))
                .when().post(USERS)
                .then()
                .statusCode(400)
                .body("violations", hasSize(1))
                .body("violations[0].field", is("create.user.lastName"))
                .body("violations[0].message", is("El apellido es requerido"));
        // @Email acepta "" como válido: lo rechaza @NotBlank
        givenJson()
                .body(user("Ana", "Pérez", "", 30))
                .when().post(USERS)
                .then()
                .statusCode(400)
                .body("violations", hasSize(1))
                .body("violations[0].field", is("create.user.email"))
                .body("violations[0].message", is("El email es requerido"));
    }

    @Test
    @Order(9)
    void crearUsuarioConNameDemasiadoCortoODemasiadoLargoDevuelve400() {
        for (String name : new String[] { "Al", "a".repeat(NAME_MAX_LENGTH + 1) }) {
            givenJson()
                    .body(user(name, "Pérez", "ana@correo.example", 30))
                    .when().post(USERS)
                    .then()
                    .statusCode(400)
                    .body("violations", hasSize(1))
                    .body("violations[0].field", is("create.user.name"))
                    .body("violations[0].message",
                            is("El nombre debe tener entre " + NAME_MIN_LENGTH + " y " + NAME_MAX_LENGTH + " caracteres"));
        }
    }

    @Test
    @Order(10)
    void crearUsuarioConLastNameDemasiadoCortoODemasiadoLargoDevuelve400() {
        for (String lastName : new String[] { "Li", "a".repeat(NAME_MAX_LENGTH + 1) }) {
            givenJson()
                    .body(user("Ana", lastName, "ana@correo.example", 30))
                    .when().post(USERS)
                    .then()
                    .statusCode(400)
                    .body("violations", hasSize(1))
                    .body("violations[0].field", is("create.user.lastName"))
                    .body("violations[0].message",
                            is("El apellido debe tener entre " + NAME_MIN_LENGTH + " y " + NAME_MAX_LENGTH + " caracteres"));
        }
    }

    @Test
    @Order(11)
    void crearUsuarioConEmailInvalidoDevuelve400() {
        for (String email : new String[] { "ana", "ana@", "@correo.example", "ana perez@correo.example" }) {
            givenJson()
                    .body(user("Ana", "Pérez", email, 30))
                    .when().post(USERS)
                    .then()
                    .statusCode(400)
                    .body("violations", hasSize(1))
                    .body("violations[0].field", is("create.user.email"))
                    .body("violations[0].message", is("El email no tiene un formato válido"));
        }
    }

    @Test
    @Order(12)
    void crearUsuarioConEmailSinDominioDeNivelSuperiorEsValido() {
        // @Email sigue la RFC: "ana@localhost" es un correo válido aunque no tenga ".com"
        givenJson()
                .body(user("Ana", "Pérez", "ana@localhost", 30))
                .when().post(USERS)
                .then()
                .statusCode(201)
                .body("email", is("ana@localhost"));
    }

    @Test
    @Order(13)
    void crearUsuarioConAgeNegativaDevuelve400() {
        givenJson()
                .body(user("Ana", "Pérez", "ana@correo.example", -1))
                .when().post(USERS)
                .then()
                .statusCode(400)
                .body("violations", hasSize(1))
                .body("violations[0].field", is("create.user.age"))
                .body("violations[0].message", is("La edad debe ser mayor o igual a 0"));
    }

    @Test
    @Order(14)
    void crearUsuarioConTodoInvalidoDevuelveTodasLasViolaciones() {
        givenJson()
                .body(user("Al", "Li", "ana", -1))
                .when().post(USERS)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("title", is("Constraint Violation"))
                .body("status", is(400))
                .body("violations", hasSize(4))
                .body("violations.field", containsInAnyOrder(
                        "create.user.name", "create.user.lastName", "create.user.email", "create.user.age"));
    }

    @Test
    @Order(15)
    void crearUsuarioEnXmlInvalidoDevuelveLasViolacionesEnXml() {
        String body = given()
                .contentType(ContentType.XML)
                .accept(ContentType.XML)
                .body("<user><name>Ana</name><lastName>Pérez</lastName><email>ana</email><age>-1</age></user>")
                .when().post(USERS)
                .then()
                .statusCode(400)
                .contentType(ContentType.XML)
                .extract().asString();

        assertThat(body, containsString("create.user.email"));
        assertThat(body, containsString("El email no tiene un formato válido"));
        assertThat(body, containsString("create.user.age"));
        assertThat(body, containsString("La edad debe ser mayor o igual a 0"));
    }

    @Test
    @Order(16)
    void crearUsuarioConAgeQueNoEsUnNumeroDevuelve400() {
        givenJson()
                .body(user("Ana", "Pérez", "ana@correo.example", "treinta"))
                .when().post(USERS)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(17)
    void metodoNoPermitidoEnUsersDevuelve405() {
        given()
                .when().get(USERS)
                .then()
                .statusCode(405);
    }
}
