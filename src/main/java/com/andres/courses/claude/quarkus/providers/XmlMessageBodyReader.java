package com.andres.courses.claude.quarkus.providers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Lee el cuerpo de un request en XML (Content-Type application/xml) y lo
 * convierte en el objeto del parámetro del método, con Jackson XML.
 * <p>
 * Es la contraparte de {@link XmlMessageBodyWriter}: Quarkus REST solo trae
 * el lector de JSON y, sin este provider, un request en XML responde 415
 * Unsupported Media Type. El nombre del elemento raíz no importa
 * ({@code <Product>}, {@code <product>}...): Jackson lee sus hijos como los
 * campos del objeto.
 */
@Provider
@Consumes(MediaType.APPLICATION_XML)
public class XmlMessageBodyReader implements MessageBodyReader<Object> {

    private final XmlMapper mapper = XmlMapper.builder()
            .addModule(new JavaTimeModule())
            // como el JSON de Quarkus: un elemento desconocido se ignora, no falla
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // el stream del request lo cierra Quarkus, no Jackson
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
            .build();

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // los String los lee tal cual el lector de Quarkus
        return type != String.class;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        try {
            return mapper.readValue(entityStream, mapper.constructType(genericType));
        } catch (JsonProcessingException e) {
            // XML mal formado o con un valor que no corresponde al tipo → 400, no 500
            throw new BadRequestException("XML inválido: " + e.getOriginalMessage(), e);
        }
    }
}
