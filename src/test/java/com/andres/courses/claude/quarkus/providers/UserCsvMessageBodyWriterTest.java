package com.andres.courses.claude.quarkus.providers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;

import com.andres.courses.claude.quarkus.models.GreetingDto;
import com.andres.courses.claude.quarkus.models.User;

/**
 * Pruebas unitarias de {@link UserCsvMessageBodyWriter}: JUnit puro, sin
 * arrancar Quarkus. Cubren el formato del CSV (también con varios usuarios),
 * el escapado RFC 4180 y qué tipos acepta el writer.
 */
class UserCsvMessageBodyWriterTest {

    private final UserCsvMessageBodyWriter writer = new UserCsvMessageBodyWriter();

    // tipos genéricos reales para probar isWriteable con colecciones
    @SuppressWarnings("unused")
    private List<User> users;
    @SuppressWarnings("unused")
    private Set<User> userSet;
    @SuppressWarnings("unused")
    private List<String> strings;

    @Test
    void unUsuarioEsLaCabeceraYUnaFila() {
        String csv = UserCsvMessageBodyWriter.toCsv(List.of(new User("Andres", 40, "andres@correo.example")));

        assertThat(csv, is("name,age,email\r\n"
                + "Andres,40,andres@correo.example\r\n"));
    }

    @Test
    void unaListaDeUsuariosEsUnaFilaPorUsuarioEnOrden() {
        String csv = UserCsvMessageBodyWriter.toCsv(List.of(
                new User("Ana", 31, "ana@correo.example"),
                new User("Bruno", 25, "bruno@correo.example"),
                new User("Carla", 47, "carla@correo.example")));

        assertThat(csv, is("name,age,email\r\n"
                + "Ana,31,ana@correo.example\r\n"
                + "Bruno,25,bruno@correo.example\r\n"
                + "Carla,47,carla@correo.example\r\n"));
    }

    @Test
    void unaListaVaciaEsSoloLaCabecera() {
        assertThat(UserCsvMessageBodyWriter.toCsv(List.of()), is("name,age,email\r\n"));
    }

    @Test
    void escapaComasComillasYSaltosDeLinea() {
        String csv = UserCsvMessageBodyWriter.toCsv(List.of(
                new User("Pérez, Ana", 31, "ana@correo.example"),
                new User("Ana \"la dev\"", 31, "ana@correo.example"),
                new User("Ana\nPérez", 31, "ana@correo.example")));

        assertThat(csv, is("name,age,email\r\n"
                + "\"Pérez, Ana\",31,ana@correo.example\r\n"
                + "\"Ana \"\"la dev\"\"\",31,ana@correo.example\r\n"
                + "\"Ana\nPérez\",31,ana@correo.example\r\n"));
    }

    @Test
    void unValorNuloQuedaVacio() {
        assertThat(UserCsvMessageBodyWriter.toCsv(List.of(new User("Ana", 31, null))),
                is("name,age,email\r\nAna,31,\r\n"));
    }

    @Test
    void writeToEscribeElUserDelSaludoEnUtf8YPideDescargarlo() throws IOException {
        GreetingDto greeting = new GreetingDto("Hola mundo Quarkus!", LocalDateTime.now())
                .withUser(new User("Andrés", 40, "andres@correo.example"));
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        writer.writeTo(greeting, GreetingDto.class, GreetingDto.class, null, null, headers, body);

        // el mensaje y la fecha del saludo no van en el CSV, solo el user
        assertThat(body.toString(StandardCharsets.UTF_8), is("name,age,email\r\n"
                + "Andrés,40,andres@correo.example\r\n"));
        assertThat(headers.getFirst("Content-Disposition"), is("attachment; filename=\"users.csv\""));
    }

    @Test
    void unSaludoSinUsuarioEsSoloLaCabecera() throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        writer.writeTo(new GreetingDto("Hola"), GreetingDto.class, GreetingDto.class, null, null,
                new MultivaluedHashMap<>(), body);

        assertThat(body.toString(StandardCharsets.UTF_8), is("name,age,email\r\n"));
    }

    @Test
    void aceptaGreetingDtoUserYColeccionesDeUser() throws NoSuchFieldException {
        assertThat(writer.isWriteable(GreetingDto.class, GreetingDto.class, null, null), is(true));
        assertThat(writer.isWriteable(User.class, User.class, null, null), is(true));
        assertThat(writer.isWriteable(List.class, genericType("users"), null, null), is(true));
        assertThat(writer.isWriteable(Set.class, genericType("userSet"), null, null), is(true));
    }

    @Test
    void rechazaOtrosTipos() throws NoSuchFieldException {
        assertThat(writer.isWriteable(String.class, String.class, null, null), is(false));
        assertThat(writer.isWriteable(List.class, genericType("strings"), null, null), is(false));
        // una lista sin tipo genérico no se sabe si es de usuarios
        assertThat(writer.isWriteable(List.class, List.class, null, null), is(false));
    }

    private Type genericType(String field) throws NoSuchFieldException {
        return getClass().getDeclaredField(field).getGenericType();
    }
}
