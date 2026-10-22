package com.andres.courses.claude.quarkus.models;

import java.time.LocalDateTime;

/**
 * DTO de respuesta del saludo ({@code GET /api/greeting}).
 * Jackson lo serializa a JSON usando sus componentes, en el orden en que se
 * declaran: message, user, date.
 * <p>
 * Java no tiene parámetros opcionales, así que se simulan con constructores
 * sobrecargados: lo que no se pasa queda en {@code null}, salvo {@code date},
 * que toma la fecha y hora actual.
 */
public record GreetingDto(String message, User user, LocalDateTime date) {

    public GreetingDto() {
        this(null, null, LocalDateTime.now());
    }

    public GreetingDto(String message) {
        this(message, null, LocalDateTime.now());
    }

    public GreetingDto(String message, User user) {
        this(message, user, LocalDateTime.now());
    }

    public GreetingDto(String message, LocalDateTime date) {
        this(message, null, date);
    }

    /**
     * Equivalente a un {@code setUser} para un record: como un record es
     * inmutable, no modifica este objeto, sino que devuelve una copia con el
     * user indicado. Hay que usar el valor devuelto.
     */
    public GreetingDto withUser(User user) {
        return new GreetingDto(message, user, date);
    }
}
