package com.andres.courses.claude.quarkus.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.andres.courses.claude.quarkus.validators.Rut;

/**
 * Usuario que recibe y devuelve {@code POST /api/users}:
 * {"name": "Ana", "lastName": "Pérez", "email": "ana@correo.example", "age": 30,
 * "rut": "11.111.111-1"}.
 * <p>
 * Todos los campos son requeridos. {@code @Size}, {@code @Email} y
 * {@code @PositiveOrZero} consideran válido un {@code null} (solo revisan el
 * valor si viene), por eso cada campo lleva además {@code @NotBlank} (textos)
 * o {@code @NotNull} (la edad). Lo mismo con {@code @Rut}, la restricción
 * personalizada del RUT.
 *
 * @param name     nombre: requerido, entre {@value #NAME_MIN_LENGTH} y {@value #NAME_MAX_LENGTH} caracteres
 * @param lastName apellido: requerido, entre {@value #NAME_MIN_LENGTH} y {@value #NAME_MAX_LENGTH} caracteres
 * @param email    correo: requerido, con formato de email
 * @param age      edad: requerida, mayor o igual a 0
 * @param rut      identificador tributario chileno (RUT): requerido y con
 *                 dígito verificador válido ({@link Rut})
 */
public record UserDto(
        @NotBlank(message = "El nombre es requerido")
        @Size(min = UserDto.NAME_MIN_LENGTH, max = UserDto.NAME_MAX_LENGTH,
                message = "El nombre debe tener entre {min} y {max} caracteres")
        String name,
        @NotBlank(message = "El apellido es requerido")
        @Size(min = UserDto.NAME_MIN_LENGTH, max = UserDto.NAME_MAX_LENGTH,
                message = "El apellido debe tener entre {min} y {max} caracteres")
        String lastName,
        @NotBlank(message = "El email es requerido")
        @Email(message = "El email no tiene un formato válido")
        String email,
        // Integer y no int: si no viene llega null y @NotNull lo detecta (con int llegaría 0)
        @NotNull(message = "La edad es requerida")
        @PositiveOrZero(message = "La edad debe ser mayor o igual a 0")
        Integer age,
        @NotBlank(message = "El RUT es requerido")
        // restricción personalizada: se usa igual que las estándar de Jakarta
        @Rut
        String rut) {

    /** Largo permitido del nombre y del apellido. */
    public static final int NAME_MIN_LENGTH = 3;
    public static final int NAME_MAX_LENGTH = 20;
}
