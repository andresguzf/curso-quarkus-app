package com.andres.courses.claude.quarkus.validators;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Restricción personalizada de Bean Validation: el texto debe ser un RUT
 * chileno válido (Rol Único Tributario), con su dígito verificador correcto.
 * <p>
 * Se usa igual que las anotaciones estándar ({@code @NotBlank},
 * {@code @Email}...): en un campo, componente de record, parámetro o tipo
 * genérico. La lógica está en {@link RutValidator}, que se enlaza con
 * {@code @Constraint(validatedBy = ...)}.
 * <p>
 * Formatos aceptados: con o sin puntos y con o sin guion
 * ({@code 11.111.111-1}, {@code 11111111-1}, {@code 111111111}), y la
 * {@code K} en mayúscula o minúscula. Como {@code @Email}, un {@code null} o
 * un texto en blanco es válido: para que sea requerido se combina con
 * {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = RutValidator.class)
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE })
@Retention(RUNTIME)
public @interface Rut {

    /** Mensaje si el RUT no es válido. */
    String message() default "El RUT no es válido";

    /** Grupos de validación (atributo obligatorio de toda restricción). */
    Class<?>[] groups() default {};

    /** Metadatos para el cliente de la validación (atributo obligatorio). */
    Class<? extends Payload>[] payload() default {};
}
