package com.andres.courses.claude.quarkus.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestResponse;

import com.andres.courses.claude.quarkus.models.Product;

/**
 * Controller de productos.
 */
@Path("/api/products")
public class ProductController {

    /**
     * POST /api/products con un producto en el cuerpo, en JSON o XML:
     * <pre>
     * {"name": "Teclado", "price": 49.99, "quantity": 10}
     * &lt;Product&gt;&lt;name&gt;Teclado&lt;/name&gt;&lt;price&gt;49.99&lt;/price&gt;&lt;quantity&gt;10&lt;/quantity&gt;&lt;/Product&gt;
     * </pre>
     * → 201 Created con el producto recibido en el cuerpo, también en JSON o
     * XML. Las dos direcciones son independientes:
     * <ul>
     * <li>el header {@code Content-Type} dice en qué formato viene el request
     * ({@code @Consumes}): JSON lo lee Jackson y XML
     * {@code XmlMessageBodyReader}; otro tipo → 415 Unsupported Media Type</li>
     * <li>el header {@code Accept} dice en qué formato se responde
     * ({@code @Produces}): JSON lo escribe Jackson y XML
     * {@code XmlMessageBodyWriter} (raíz {@code <Product>}); con {@code *}{@code /*}
     * o sin {@code Accept}, JSON (el primero); otro tipo → 406</li>
     * </ul>
     * Se puede enviar XML y recibir JSON, o al revés. El parámetro sin
     * anotación ({@code @RestQuery}, {@code @RestPath}...) es el cuerpo del
     * request.
     * <p>
     * Validación (Bean Validation, antes de ejecutar el método):
     * <ul>
     * <li>{@code @NotNull}: sin cuerpo → 400</li>
     * <li>{@code @Valid}: aplica las reglas de los campos de {@link Product}
     * ({@code name}, {@code price}, {@code quantity}); si alguna falla → 400
     * con el {@code ViolationReport} en JSON o XML según el {@code Accept}, y
     * el campo con su ruta: {@code create.product.price}</li>
     * <li>JSON o XML mal formado, o con un tipo incorrecto (p. ej.
     * {@code "price": "caro"}) → 400 antes de validar</li>
     * </ul>
     * Sin {@code @Valid}, las anotaciones de {@link Product} se ignoran.
     * Todavía no hay persistencia: el producto no se guarda, solo se devuelve.
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public RestResponse<Product> create(
            @NotNull(message = "El cuerpo del request es requerido: envía un producto")
            @Valid
            Product product) {
        return RestResponse.ResponseBuilder.<Product>create(RestResponse.Status.CREATED)
                .entity(product)
                .build();
    }
}
