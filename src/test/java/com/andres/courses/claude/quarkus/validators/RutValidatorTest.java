package com.andres.courses.claude.quarkus.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas unitarias de {@link RutValidator}, sin arrancar Quarkus: se llama
 * directo a {@code isValid} (el {@code ConstraintValidatorContext} no se usa,
 * por eso va {@code null}).
 * <p>
 * Todos los RUT son sintéticos (cuerpos con dígitos repetidos o redondos),
 * no de personas reales.
 */
class RutValidatorTest {

    private final RutValidator validator = new RutValidator();

    @ParameterizedTest
    @ValueSource(strings = { "11.111.111-1", "11111111-1", "111111111", "22.222.222-2", "1.000.000-9",
            "1000000-9", "10.000.004-0", "10.000.013-K", "10.000.013-k", "10000013K", "  11.111.111-1  " })
    void aceptaRutValidosEnCualquierFormato(String rut) {
        assertTrue(validator.isValid(rut, null), rut);
    }

    @ParameterizedTest
    @ValueSource(strings = { "11.111.111-2", "10.000.013-0", "10.000.004-K", "22222222-1" })
    void rechazaRutConDigitoVerificadorIncorrecto(String rut) {
        assertFalse(validator.isValid(rut, null), rut);
    }

    @ParameterizedTest
    @ValueSource(strings = { "1.1111.111-1", "11.111111-1", "111.111.111-1", "11.111.111--1", "11.111.111-X",
            "123456-0", "123456789-0", "abc", "-1", "11 111 111-1" })
    void rechazaRutConFormatoInvalido(String rut) {
        assertFalse(validator.isValid(rut, null), rut);
    }

    @Test
    void nullYEnBlancoSonValidosPorqueLosResuelveNotBlank() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
        assertTrue(validator.isValid("   ", null));
    }

    @ParameterizedTest
    @CsvSource({ "11111111,1", "22222222,2", "1000000,9", "10000004,0", "10000013,K" })
    void calculaElDigitoVerificadorConModulo11(String body, char expected) {
        assertEquals(expected, RutValidator.checkDigit(body));
    }
}
