package com.andres.courses.claude.quarkus.controllers;

import static com.andres.courses.claude.quarkus.models.UserDto.NAME_MAX_LENGTH;
import static com.andres.courses.claude.quarkus.models.UserDto.NAME_MIN_LENGTH;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
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
 * {@code age}, {@code rut}) y, al final, los errores del request.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerTest {

    private static final String USERS = "/api/users";

    /** RUT válido de prueba (sintético, con dígito verificador correcto). */
    private static final String RUT = "11.111.111-1";

    /** Usuario válido con {@link #RUT}: cada prueba cambia solo el campo que necesita. */
    private static Map<String, Object> user(Object name, Object lastName, Object email, Object age) {
        return user(name, lastName, email, age, RUT);
    }

    private static Map<String, Object> user(Object name, Object lastName, Object email, Object age, Object rut) {
        // HashMap y no Map.of: algunas pruebas envían valores null
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("lastName", lastName);
        user.put("email", email);
        user.put("age", age);
        user.put("rut", rut);
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
                // exactamente name, lastName, email, age y rut
                .body("$", aMapWithSize(5))
                .body("name", is("Ana"))
                .body("lastName", is("Pérez"))
                .body("email", is("ana@correo.example"))
                .body("age", is(30))
                .body("rut", is(RUT));
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
                            <rut>11.111.111-1</rut>
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
                containsInAnyOrder("name", "lastName", "email", "age", "rut"));
    }

    @Test
    @Order(3)
    void crearUsuarioEnXmlRespetaElCharsetDelContentType() {
        // el mismo XML con acentos en UTF-8 y en ISO-8859-1: el reader usa el charset declarado
        for (String charset : new String[] { "UTF-8", "ISO-8859-1" }) {
            given()
                    .contentType("application/xml; charset=" + charset)
                    .accept(ContentType.JSON)
                    .body("<user><name>José</name><lastName>Núñez</lastName><email>jose@correo.example</email><age>30</age><rut>11.111.111-1</rut></user>")
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
                .body("$", aMapWithSize(1))
                .body("user", contains("El cuerpo del request es requerido: envía un usuario"));
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
                    .body("$", aMapWithSize(1))
                    .body("name", contains("El nombre es requerido"));
        }
    }

    @Test
    @Order(7)
    void crearUsuarioSoloConNameDevuelve400ConLosCamposRequeridos() {
        // lastName, email y age en null: @Size, @Email y @PositiveOrZero no lo detectan,
        // @NotBlank y @NotNull sí
        givenJson()
                .body(user("Andres", null, null, null, null))
                .when().post(USERS)
                .then()
                .statusCode(400)
                .body("$", aMapWithSize(4))
                .body("lastName", contains("El apellido es requerido"))
                .body("email", contains("El email es requerido"))
                .body("age", contains("La edad es requerida"))
                .body("rut", contains("El RUT es requerido"));
        // lo mismo si los campos ni siquiera vienen en el JSON
        givenJson()
                .body("""
                        {"name": "Andres"}
                        """)
                .when().post(USERS)
                .then()
                .statusCode(400)
                .body("$", aMapWithSize(4));
    }

    @Test
    @Order(8)
    void crearUsuarioConLastNameOEmailEnBlancoDevuelve400() {
        givenJson()
                .body(user("Ana", "   ", "ana@correo.example", 30))
                .when().post(USERS)
                .then()
                .statusCode(400)
                .body("$", aMapWithSize(1))
                .body("lastName", contains("El apellido es requerido"));
        // @Email acepta "" como válido: lo rechaza @NotBlank
        givenJson()
                .body(user("Ana", "Pérez", "", 30))
                .when().post(USERS)
                .then()
                .statusCode(400)
                .body("$", aMapWithSize(1))
                .body("email", contains("El email es requerido"));
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
                    .body("$", aMapWithSize(1))
                    .body("name", contains("El nombre debe tener entre " + NAME_MIN_LENGTH + " y " + NAME_MAX_LENGTH + " caracteres"));
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
                    .body("$", aMapWithSize(1))
                    .body("lastName", contains("El apellido debe tener entre " + NAME_MIN_LENGTH + " y " + NAME_MAX_LENGTH + " caracteres"));
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
                    .body("$", aMapWithSize(1))
                    .body("email", contains("El email no tiene un formato válido"));
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
                .body("$", aMapWithSize(1))
                .body("age", contains("La edad debe ser mayor o igual a 0"));
    }

    @Test
    @Order(14)
    void crearUsuarioConRutEnDistintosFormatosDevuelve201() {
        // RUT sintéticos: con y sin puntos, con y sin guion, dígito 0 y K (mayúscula y minúscula)
        for (String rut : new String[] { "11.111.111-1", "22222222-2", "111111111", "1.000.000-9",
                "10.000.004-0", "10.000.013-K", "10000013-k" }) {
            givenJson()
                    .body(user("Ana", "Pérez", "ana@correo.example", 30, rut))
                    .when().post(USERS)
                    .then()
                    .statusCode(201)
                    // se devuelve tal como llegó, sin normalizar
                    .body("rut", is(rut));
        }
    }

    @Test
    @Order(15)
    void crearUsuarioConRutInvalidoDevuelve400() {
        // dígito verificador incorrecto, puntos mal puestos, letras, muy corto o vacío de cuerpo
        for (String rut : new String[] { "11.111.111-2", "10.000.013-0", "1.1111.111-1", "11.111.111-X",
                "abc", "123-4", "-1" }) {
            givenJson()
                    .body(user("Ana", "Pérez", "ana@correo.example", 30, rut))
                    .when().post(USERS)
                    .then()
                    .statusCode(400)
                    .body("$", aMapWithSize(1))
                    .body("rut", contains("El RUT no es válido"));
        }
    }

    @Test
    @Order(16)
    void crearUsuarioConRutEnBlancoDevuelveSoloRequerido() {
        // @Rut deja pasar el texto en blanco: el único error es el de @NotBlank
        givenJson()
                .body(user("Ana", "Pérez", "ana@correo.example", 30, "   "))
                .when().post(USERS)
                .then()
                .statusCode(400)
                .body("$", aMapWithSize(1))
                .body("rut", contains("El RUT es requerido"));
    }

    @Test
    @Order(17)
    void crearUsuarioConVariasReglasFallidasEnUnCampoDevuelveTodosSusMensajes() {
        // name vacío: fallan @NotBlank y @Size(min = 3), los dos mensajes van en la lista de name
        givenJson()
                .body(user("", "Pérez", "ana@correo.example", 30))
                .when().post(USERS)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("$", aMapWithSize(1))
                // en orden alfabético
                .body("name", contains(
                        "El nombre debe tener entre " + NAME_MIN_LENGTH + " y " + NAME_MAX_LENGTH + " caracteres",
                        "El nombre es requerido"));
    }

    @Test
    @Order(18)
    void crearUsuarioConTodoInvalidoDevuelveTodasLasViolaciones() {
        givenJson()
                .body(user("Al", "Li", "ana", -1, "11.111.111-2"))
                .when().post(USERS)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                // formato simple: cada campo con la lista de sus mensajes
                .body("$", aMapWithSize(5))
                .body("name", contains("El nombre debe tener entre " + NAME_MIN_LENGTH + " y " + NAME_MAX_LENGTH + " caracteres"))
                .body("lastName", contains("El apellido debe tener entre " + NAME_MIN_LENGTH + " y " + NAME_MAX_LENGTH + " caracteres"))
                .body("email", contains("El email no tiene un formato válido"))
                .body("age", contains("La edad debe ser mayor o igual a 0"))
                .body("rut", contains("El RUT no es válido"));
    }

    @Test
    @Order(19)
    void crearUsuarioEnXmlInvalidoDevuelveLasViolacionesEnXml() {
        String body = given()
                .contentType(ContentType.XML)
                .accept(ContentType.XML)
                .body("<user><name>Ana</name><lastName>Pérez</lastName><email>ana</email><age>-1</age><rut>11.111.111-2</rut></user>")
                .when().post(USERS)
                .then()
                .statusCode(400)
                .contentType(ContentType.XML)
                .extract().asString();

        assertThat(body, containsString("<email>"));
        assertThat(body, containsString("El email no tiene un formato válido"));
        assertThat(body, containsString("<age>"));
        assertThat(body, containsString("La edad debe ser mayor o igual a 0"));
        assertThat(body, containsString("<rut>"));
        assertThat(body, containsString("El RUT no es válido"));
    }

    @Test
    @Order(20)
    void crearUsuarioConAgeQueNoEsUnNumeroDevuelve400() {
        givenJson()
                .body(user("Ana", "Pérez", "ana@correo.example", "treinta"))
                .when().post(USERS)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(21)
    void metodoNoPermitidoEnUsersDevuelve405() {
        given()
                .when().get(USERS)
                .then()
                .statusCode(405);
    }
}
