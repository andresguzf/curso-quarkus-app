package com.andres.courses.claude.quarkus.providers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import com.andres.courses.claude.quarkus.models.GreetingDto;
import com.andres.courses.claude.quarkus.models.User;

/**
 * Serializa usuarios a CSV (text/csv): un archivo plano con una cabecera con
 * los campos de {@link User} y una fila por usuario.
 *
 * <pre>
 * name,age,email
 * Andres,40,andres@correo.example
 * </pre>
 *
 * Acepta tres tipos de respuesta:
 * <ul>
 * <li>{@code Collection<User>}: una fila por usuario (una tabla o lista de
 * usuarios)</li>
 * <li>{@link User}: una sola fila</li>
 * <li>{@link GreetingDto}: la fila de su {@code user}; el mensaje y la fecha
 * no van en el CSV. Sin usuario, solo la cabecera</li>
 * </ul>
 * El separador de columnas es configurable: por defecto es la coma
 * ({@value #DEFAULT_SEPARATOR}); para cambiarlo en toda la API basta con
 * modificar {@link #DEFAULT_SEPARATOR} (p. ej. a {@code ';'}, el que espera
 * Excel en configuraciones regionales en español), y el constructor
 * {@link #UserCsvMessageBodyWriter(char)} permite usar otro en una instancia.
 * <p>
 * Los valores se escapan según RFC 4180 y las líneas terminan en CRLF. No
 * necesita dependencias: es texto plano.
 * <p>
 * Igual que el writer de PDF, va tipado como {@code MessageBodyWriter<Object>}
 * y filtra en {@link #isWriteable}, para que Quarkus REST lo elija con un
 * {@code RestResponse<...>}.
 */
@Provider
@Produces(UserCsvMessageBodyWriter.TEXT_CSV)
public class UserCsvMessageBodyWriter implements MessageBodyWriter<Object> {

    /** jakarta.ws.rs.core.MediaType no trae una constante para CSV. */
    public static final String TEXT_CSV = "text/csv";

    /** Separador de columnas que usa Quarkus al crear el provider. */
    public static final char DEFAULT_SEPARATOR = ';';

    /** Columnas del CSV: los campos de {@link User}, en este orden. */
    static final List<String> COLUMNS = List.of("name", "age", "email");

    private static final String CRLF = "\r\n";

    private final String separator;

    /** Con el separador por defecto ({@value #DEFAULT_SEPARATOR}). Es el que usa Quarkus. */
    public UserCsvMessageBodyWriter() {
        this(DEFAULT_SEPARATOR);
    }

    /**
     * @param separator separador de columnas; no puede ser una comilla doble
     *                  ni un salto de línea, que el CSV reserva para escapar
     *                  valores y separar filas
     */
    public UserCsvMessageBodyWriter(char separator) {
        if (separator == '"' || separator == '\n' || separator == '\r') {
            throw new IllegalArgumentException("Separador de CSV no válido: " + separator);
        }
        this.separator = String.valueOf(separator);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (GreetingDto.class.isAssignableFrom(type) || User.class.isAssignableFrom(type)) {
            return true;
        }
        // Collection<User> (List, Set...): se comprueba el tipo de sus elementos
        return Collection.class.isAssignableFrom(type)
                && genericType instanceof ParameterizedType parameterized
                && parameterized.getActualTypeArguments()[0] instanceof Class<?> element
                && User.class.isAssignableFrom(element);
    }

    @Override
    public void writeTo(Object entity, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        // attachment: el navegador lo descarga como archivo
        httpHeaders.putSingle(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.csv\"");
        entityStream.write(toCsv(users(entity)).getBytes(StandardCharsets.UTF_8));
    }

    /** Los usuarios que contiene la respuesta, sea una colección, un User o un GreetingDto. */
    private static List<User> users(Object entity) {
        return switch (entity) {
            case Collection<?> collection -> collection.stream().map(User.class::cast).toList();
            case User user -> List.of(user);
            case GreetingDto greeting when greeting.user() != null -> List.of(greeting.user());
            default -> List.of();
        };
    }

    /** CSV completo: cabecera + una fila por usuario. Una lista vacía deja solo la cabecera. */
    String toCsv(List<User> users) {
        return Stream.concat(Stream.of(String.join(separator, COLUMNS)), users.stream().map(this::row))
                .map(line -> line + CRLF)
                .collect(Collectors.joining());
    }

    private String row(User user) {
        return String.join(separator,
                escape(user.getName()),
                String.valueOf(user.getAge()),
                escape(user.getEmail()));
    }

    /**
     * RFC 4180: si el valor tiene el separador, comillas o saltos de línea, va
     * entre comillas dobles y cada comilla se duplica. Un valor nulo queda vacío.
     */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(separator) || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
