package com.andres.courses.claude.quarkus.models;

/**
 * DTO de respuesta de {@code GET /api/headers}: los valores de los headers
 * que envió el cliente.
 *
 * @param clientId      valor del header requerido {@code X-Client-Id}
 * @param clientVersion valor del header opcional {@code X-Client-Version};
 *                      {@code null} si el cliente no lo envió
 */
public record ClientHeadersDto(String clientId, String clientVersion, String contentTypeHeader) {

}
