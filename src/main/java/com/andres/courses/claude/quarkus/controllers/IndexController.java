package com.andres.courses.claude.quarkus.controllers;

import java.time.LocalDateTime;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
}
