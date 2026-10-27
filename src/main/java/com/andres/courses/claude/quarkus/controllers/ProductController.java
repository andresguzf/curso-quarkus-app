package com.andres.courses.claude.quarkus.controllers;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestResponse;

import com.andres.courses.claude.quarkus.models.Product;

/**
 * Controller de productos.
 */
@Path("/api/products")
public class ProductController {

    /**
     * POST /api/products con un producto en el cuerpo, en JSON:
     * <pre>
     * {"name": "Teclado", "price": 49.99, "quantity": 10}
     * </pre>
     * → 201 Created con el producto recibido en el cuerpo.
     * <p>
     * El parámetro sin anotación ({@code @RestQuery}, {@code @RestPath}...) es
     * el cuerpo del request: Jackson lo convierte en un {@link Product}. Sin
     * {@code @Consumes} ni {@code @Produces}, Quarkus REST recibe y responde
     * JSON. Todavía no hay validación ni persistencia: el producto no se
     * guarda, solo se devuelve.
     */
    @POST
    public RestResponse<Product> create(Product product) {
        return RestResponse.ResponseBuilder.<Product>create(RestResponse.Status.CREATED)
                .entity(product)
                .build();
    }
}
