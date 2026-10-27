package com.andres.courses.claude.quarkus.providers;

import java.util.Map;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Content negotiation por query param: traduce
 * {@code ?format=json|xml|pdf|csv|xlsx} ({@code excel} es un alias de
 * {@code xlsx}) al header {@code Accept} de la petición.
 * <p>
 * Quarkus REST elige el serializador al negociar el {@code Accept} contra el
 * {@code @Produces} del método, antes de ejecutarlo; por eso fijar el tipo
 * con {@code RestResponse.type(...)} solo cambia la cabecera y no el cuerpo.
 * Al ser {@code @PreMatching}, este filtro corre antes de esa negociación.
 * <ul>
 * <li>sin {@code format}: no hace nada y decide el {@code Accept} del cliente</li>
 * <li>{@code format} válido (sin distinguir mayúsculas): reemplaza el {@code Accept}</li>
 * <li>{@code format} desconocido: corta la petición con 400 Bad Request</li>
 * </ul>
 * Aplica a todos los endpoints: si uno no produce el formato pedido, responde 406.
 */
@Provider
@PreMatching
public class FormatQueryParamFilter implements ContainerRequestFilter {

    public static final String FORMAT = "format";

    private static final Map<String, String> FORMATS = Map.of(
            "json", MediaType.APPLICATION_JSON,
            "xml", MediaType.APPLICATION_XML,
            "pdf", GreetingPdfMessageBodyWriter.APPLICATION_PDF,
            "csv", UserCsvMessageBodyWriter.TEXT_CSV,
            "xlsx", UserExcelMessageBodyWriter.APPLICATION_XLSX,
            "excel", UserExcelMessageBodyWriter.APPLICATION_XLSX);

    @Override
    public void filter(ContainerRequestContext request) {
        String format = request.getUriInfo().getQueryParameters().getFirst(FORMAT);
        if (format == null) {
            return;
        }

        String mediaType = FORMATS.get(format.toLowerCase());
        if (mediaType == null) {
            request.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
            return;
        }

        request.getHeaders().putSingle(HttpHeaders.ACCEPT, mediaType);
    }
}
