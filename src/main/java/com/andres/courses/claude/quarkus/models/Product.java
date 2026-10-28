package com.andres.courses.claude.quarkus.models;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Producto que recibe y devuelve {@code POST /api/products}:
 * {"name": "Teclado", "price": 49.99, "quantity": 10}.
 * <p>
 * Jackson lo arma desde el JSON (o el XML) del request usando los nombres de
 * los componentes del record. Las reglas de Bean Validation de cada
 * componente se aplican cuando el parámetro del método lleva {@code @Valid}.
 *
 * @param name     nombre del producto: requerido, hasta {@value #NAME_MAX_LENGTH} caracteres
 * @param price    precio: requerido, mayor que 0 y con hasta 4 decimales;
 *                 {@code BigDecimal} y no {@code double} porque un precio
 *                 necesita decimales exactos
 * @param quantity cantidad en stock: requerida y mayor o igual a 0
 */
public record Product(
        @NotBlank(message = "El nombre es requerido")
        @Size(max = Product.NAME_MAX_LENGTH, message = "El nombre del producto admite hasta {max} caracteres")
        String name,
        @NotNull(message = "El precio es requerido")
        @Positive(message = "El precio debe ser mayor que 0")
        @Digits(integer = 12, fraction = 4, message = "El precio admite hasta {fraction} decimales")
        BigDecimal price,
        // Integer y no int: si falta llega null y @NotNull lo detecta (con int llegaría 0)
        @NotNull(message = "La cantidad es requerida")
        @PositiveOrZero(message = "La cantidad debe ser mayor o igual a 0")
        Integer quantity) {

    /** Largo máximo del nombre del producto. */
    public static final int NAME_MAX_LENGTH = 50;
}
