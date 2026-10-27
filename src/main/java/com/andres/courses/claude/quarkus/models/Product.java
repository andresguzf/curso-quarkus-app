package com.andres.courses.claude.quarkus.models;

import java.math.BigDecimal;

/**
 * Producto que recibe y devuelve {@code POST /api/products}:
 * {"name": "Teclado", "price": 49.99, "quantity": 10}.
 * <p>
 * Jackson lo arma desde el JSON del request usando los nombres de los
 * componentes del record.
 *
 * @param name     nombre del producto
 * @param price    precio; {@code BigDecimal} y no {@code double} porque un
 *                 precio necesita decimales exactos
 * @param quantity cantidad en stock
 */
public record Product(String name, BigDecimal price, Integer quantity) {

}
