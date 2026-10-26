package com.andres.courses.claude.quarkus.providers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Serializa a XML (application/xml) las respuestas de la API con Jackson XML.
 * <p>
 * Quarkus REST solo trae el serializador de JSON; sin este provider, una
 * respuesta con Content-Type application/xml sale con el cuerpo en JSON.
 * A diferencia de JAXB, Jackson XML convierte también un {@code Map} y las
 * fechas de {@code java.time}.
 */
@Provider
@Produces(MediaType.APPLICATION_XML)
public class XmlMessageBodyWriter implements MessageBodyWriter<Object> {

    /** Elemento raíz del XML cuando la respuesta es un Map (no tiene nombre de clase útil). */
    private static final String MAP_ROOT = "response";

    private final XmlMapper mapper = XmlMapper.builder()
            .addModule(new JavaTimeModule())
            // fechas en ISO-8601 ("2026-10-26T10:15:30"), no como números
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // el stream de la respuesta lo cierra Quarkus, no Jackson
            .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
            .build();

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // los String ya son el cuerpo final: los escribe el serializador de Quarkus
        return type != String.class;
    }

    @Override
    public void writeTo(Object entity, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        ObjectWriter writer = entity instanceof Map ? mapper.writer().withRootName(MAP_ROOT) : mapper.writer();
        writer.writeValue(entityStream, entity);
    }
}
