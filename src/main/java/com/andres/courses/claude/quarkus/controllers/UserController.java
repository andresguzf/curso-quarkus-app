package com.andres.courses.claude.quarkus.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestResponse;

import com.andres.courses.claude.quarkus.models.UserDto;

/**
 * Controller de usuarios.
 */
@Path("/api/users")
public class UserController {

    /**
     * POST /api/users con un usuario en el cuerpo, en JSON o XML:
     * <pre>
     * {"name": "Ana", "lastName": "Pérez", "email": "ana@correo.example", "age": 30}
     * </pre>
     * → 201 Created con el usuario recibido en el cuerpo, en JSON o XML según
     * el {@code Accept}. Igual que {@code POST /api/products}, el
     * {@code Content-Type} elige el formato de entrada y el {@code Accept} el
     * de salida.
     * <p>
     * Validación (Bean Validation, antes de ejecutar el método):
     * <ul>
     * <li>{@code @NotNull}: sin cuerpo → 400</li>
     * <li>{@code @Valid}: aplica las reglas de {@link UserDto}: {@code name}
     * y {@code lastName} requeridos y entre 3 y 20 caracteres; {@code email}
     * requerido y con formato válido; {@code age} requerida y mayor o igual
     * a 0. Si alguna falla → 400 con el {@code ViolationReport}
     * ({@code create.user.email}...)</li>
     * </ul>
     * Todavía no hay persistencia: el usuario no se guarda, solo se devuelve.
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public RestResponse<UserDto> create(
            @NotNull(message = "El cuerpo del request es requerido: envía un usuario")
            @Valid
            UserDto user) {
        return RestResponse.ResponseBuilder.<UserDto>create(RestResponse.Status.CREATED)
                .entity(user)
                .build();
    }
}
