package com.andres.courses.claude.quarkus.validators;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Lógica de {@link Rut}: revisa el formato y calcula el dígito verificador
 * con el algoritmo módulo 11.
 * <p>
 * Hibernate Validator crea la instancia y llama a {@link #isValid} con el
 * valor del campo; no hay que registrarlo en ningún lado.
 */
public class RutValidator implements ConstraintValidator<Rut, String> {

    /**
     * Cuerpo de 7 u 8 dígitos (con los puntos bien puestos o sin puntos),
     * guion opcional y dígito verificador (0-9 o K).
     */
    private static final Pattern FORMAT = Pattern.compile("(\\d{1,2}\\.\\d{3}\\.\\d{3}|\\d{7,8})-?[\\dkK]");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null y en blanco los resuelve @NotBlank: aquí solo se valida el valor si viene
        // (así un RUT vacío da un solo error, "requerido", y no dos)
        if (value == null || value.isBlank()) {
            return true;
        }
        String rut = value.trim();
        if (!FORMAT.matcher(rut).matches()) {
            return false;
        }
        // "11.111.111-1" → cuerpo "11111111" y dígito verificador '1'
        String clean = rut.replace(".", "").replace("-", "").toUpperCase();
        String body = clean.substring(0, clean.length() - 1);
        char checkDigit = clean.charAt(clean.length() - 1);
        return checkDigit(body) == checkDigit;
    }

    /**
     * Módulo 11: multiplica los dígitos del cuerpo, de derecha a izquierda,
     * por 2, 3, 4, 5, 6, 7, 2, 3...; suma los productos y resta a 11 el resto
     * de dividir por 11. 11 → '0' y 10 → 'K'.
     */
    static char checkDigit(String body) {
        int sum = 0;
        int factor = 2;
        for (int i = body.length() - 1; i >= 0; i--) {
            sum += Character.getNumericValue(body.charAt(i)) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int result = 11 - (sum % 11);
        return switch (result) {
            case 11 -> '0';
            case 10 -> 'K';
            default -> (char) ('0' + result);
        };
    }
}
