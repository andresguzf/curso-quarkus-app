package com.andres.courses.claude.quarkus.controllers;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestResponse;

import com.andres.courses.claude.quarkus.models.GreetingDto;
import com.andres.courses.claude.quarkus.models.User;

/**
 * Controller de bienvenida: responde en la raíz de la API.
 */
@Path("/api")
public class IndexController {

    /**
     * GET /api/index → {"message": "Hola mundo de Quarkus"}
     */
    @Path("/greeting")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GreetingDto index() {
        User user = new User("Andres", 40, "andres@correo.example");
        return new GreetingDto("Hola mundo Quarkus!", LocalDateTime.now()).withUser(user);

        // return Map.of("message", "Hola mundo Quarkus!",
        //         "user", Map.of("name", "Andres",
        //                 "age", 40,
        //                 "email", "andres@correo.example"),
        //         "date", LocalDateTime.now());
    }

    /**
     * GET /api/user → 200 OK con el usuario en el cuerpo:
     * {"name": "Andres", "age": 40, "email": "andres@correo.example"}
     * <p>
     * Usa {@link RestResponse.ResponseBuilder} para indicar de forma explícita
     * el status y el content type de la respuesta.
     */
    @Path("/user")
    @GET
    public RestResponse<User> user() {
        User user = new User("Andres", 40, "andres@correo.example");
        return RestResponse.ResponseBuilder.<User>create(RestResponse.Status.OK)
                .type(MediaType.APPLICATION_JSON)
                .entity(user)
                .build();
    }

    /**
     * GET /api/greeting-map → 202 Accepted con el mismo saludo que
     * /api/greeting, pero armado con un {@code Map} y en XML:
     * {@code <response><message>Hola mundo Quarkus!</message><user>...</user><date>...</date></response>}
     * <p>
     * Como en /api/user, usa {@link RestResponse.ResponseBuilder} para indicar
     * el status, las cabeceras y el content type. {@code @Produces} es
     * necesario: sin él, Quarkus REST trata el método como JSON. El XML lo
     * genera {@code XmlMessageBodyWriter}. {@code Map.of} no garantiza el
     * orden de los campos.
     */
    @Path("/greeting-map")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public RestResponse<Map<String, Object>> indexMap() {
        Map<String, Object> body = Map.of("message", "Hola mundo Quarkus!",
                "user", Map.of("name", "Andres",
                        "age", 40,
                        "email", "andres@correo.example"),
                "date", LocalDateTime.now());

        return RestResponse.ResponseBuilder.<Map<String, Object>>create(RestResponse.Status.ACCEPTED)
                .header("X-send-message", "Hola que tal este es un mensaje o data oculto en el header del response")
                .header("Content-Type", "application/xml")
                .entity(body)
                .build();
    }
}
