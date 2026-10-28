package com.andres.courses.claude.quarkus.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;

import org.jboss.resteasy.reactive.RestResponse;

/**
 * Respuesta simple para los errores de Bean Validation: un JSON con cada
 * campo y la lista de sus mensajes (uno o varios, si falló más de una regla):
 * <pre>
 * {
 *   "email": ["El email no tiene un formato válido"],
 *   "name": ["El nombre debe tener entre 3 y 20 caracteres"],
 *   "rut": ["El RUT no es válido"]
 * }
 * </pre>
 * Reemplaza al {@code ViolationReport} de Quarkus ({@code title},
 * {@code status}, {@code violations[].field}/{@code message}). Es reutilizable:
 * cada controller que lo quiera declara un método con
 * {@code @ServerExceptionMapper} que delega aquí:
 * <pre>
 * &#64;ServerExceptionMapper
 * public RestResponse&lt;Map&lt;String, List&lt;String&gt;&gt;&gt; validationErrors(ConstraintViolationException e) {
 *     return ValidationErrors.badRequest(e);
 * }
 * </pre>
 * Un {@code @ServerExceptionMapper} dentro de un controller solo aplica a
 * ese controller; los que no lo declaran siguen con el {@code ViolationReport}.
 */
public final class ValidationErrors {

    private ValidationErrors() {
    }

    /**
     * 400 Bad Request con los errores agrupados por campo. Los campos van en
     * orden alfabético y los mensajes de cada campo también, para que la
     * respuesta sea siempre la misma (Bean Validation no garantiza el orden).
     */
    public static RestResponse<Map<String, List<String>>> badRequest(ConstraintViolationException e) {
        Map<String, List<String>> errors = new TreeMap<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            errors.computeIfAbsent(fieldName(violation.getPropertyPath()), field -> new ArrayList<>())
                    .add(violation.getMessage());
        }
        errors.values().forEach(messages -> messages.sort(null));
        return RestResponse.ResponseBuilder.<Map<String, List<String>>>create(RestResponse.Status.BAD_REQUEST)
                .entity(errors)
                .build();
    }

    /**
     * Nombre simple del campo a partir de la ruta de Bean Validation:
     * <ul>
     * <li>{@code create.user.name} → {@code name}: se quitan el método y el
     * parámetro del cuerpo</li>
     * <li>{@code create.user} (sin cuerpo) → {@code user}: el parámetro</li>
     * <li>{@code handler.categories[1].<list element>} →
     * {@code categories[1]}: un elemento de una lista lleva su posición</li>
     * </ul>
     */
    static String fieldName(Path path) {
        List<String> segments = new ArrayList<>();
        boolean startsWithParameter = false;
        for (Path.Node node : path) {
            ElementKind kind = node.getKind();
            if (kind == ElementKind.METHOD || kind == ElementKind.CONSTRUCTOR) {
                continue;
            }
            // la posición en una lista va pegada al segmento anterior: categories[1]
            if (node.getIndex() != null && !segments.isEmpty()) {
                int last = segments.size() - 1;
                segments.set(last, segments.get(last) + "[" + node.getIndex() + "]");
            }
            if (kind == ElementKind.CONTAINER_ELEMENT) {
                continue;
            }
            if (segments.isEmpty()) {
                startsWithParameter = kind == ElementKind.PARAMETER;
            }
            segments.add(node.getName());
        }
        // user.name → name: el nombre del parámetro del cuerpo no aporta
        if (startsWithParameter && segments.size() > 1) {
            segments.remove(0);
        }
        return String.join(".", segments);
    }
}
