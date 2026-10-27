package com.andres.courses.claude.quarkus.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.Separator;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.andres.courses.claude.quarkus.models.ClientHeadersDto;
import com.andres.courses.claude.quarkus.models.GreetingDto;
import com.andres.courses.claude.quarkus.models.User;
import com.andres.courses.claude.quarkus.providers.FormatQueryParamFilter;
import com.andres.courses.claude.quarkus.providers.GreetingPdfMessageBodyWriter;
import com.andres.courses.claude.quarkus.providers.UserCsvMessageBodyWriter;
import com.andres.courses.claude.quarkus.providers.UserExcelMessageBodyWriter;

/**
 * Controller de bienvenida: responde en la raíz de la API.
 */
@Path("/api")
public class IndexController {

    /** Header requerido de {@code GET /api/headers}. */
    public static final String CLIENT_ID_HEADER = "X-Client-Id";

    /** Header opcional de {@code GET /api/headers}. */
    public static final String CLIENT_VERSION_HEADER = "X-Client-Version";

    /** Largo máximo del parámetro {@code name} de {@code GET /api/hello}. */
    public static final int NAME_MAX_LENGTH = 8;

    /** Rango válido del parámetro {@code age} de {@code GET /api/hello}. */
    public static final int AGE_MIN = 0;
    public static final int AGE_MAX = 120;

    /** Máximo de categorías del parámetro {@code categories} de {@code GET /api/hello}. */
    public static final int CATEGORIES_MAX = 5;

    /** Largo permitido de cada categoría de {@code GET /api/categories}. */
    public static final int CATEGORY_MIN_LENGTH = 2;
    public static final int CATEGORY_MAX_LENGTH = 20;

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
     * GET /api/greeting-map → 200 OK con el mismo saludo que
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

        return RestResponse.ResponseBuilder.<Map<String, Object>>create(RestResponse.Status.OK)
                .header("X-send-message", "Hola que tal este es un mensaje o data oculto en el header del response")
                .entity(body)
                .build();
    }

    /**
     * GET /api/greeting-xml?format=json|xml|pdf|csv|xlsx → 200 OK con el mismo
     * saludo que /api/greeting (DTO {@link GreetingDto} con su {@link User}),
     * en JSON, XML, PDF, CSV o Excel según el query param {@code format}:
     * <ul>
     * <li>{@code ?format=json} → JSON (lo genera Jackson)</li>
     * <li>{@code ?format=xml} → XML con raíz {@code <GreetingDto>} (lo genera
     * {@code XmlMessageBodyWriter})</li>
     * <li>{@code ?format=pdf} → PDF con el usuario en una tabla (lo genera
     * {@code GreetingPdfMessageBodyWriter} con OpenPDF)</li>
     * <li>{@code ?format=csv} → archivo plano CSV solo con el usuario:
     * cabecera {@code name,age,email} y una fila (lo genera
     * {@code UserCsvMessageBodyWriter}, que también sirve para una lista de
     * usuarios)</li>
     * <li>{@code ?format=xlsx} (o {@code excel}) → Excel con el mismo diseño
     * que el CSV: cabecera y una fila por usuario (lo genera
     * {@code UserExcelMessageBodyWriter} con Apache POI)</li>
     * <li>otro valor (p. ej. {@code ?format=yaml}) → 400 Bad Request</li>
     * <li>sin {@code format} → content negotiation por el header
     * {@code Accept}: JSON, XML, PDF, CSV o Excel según lo pida el cliente,
     * JSON si acepta cualquiera ({@code *}{@code /*}) y 406 si pide otro
     * tipo</li>
     * </ul>
     * Si viene {@code format}, manda sobre el {@code Accept}: lo traduce
     * {@code FormatQueryParamFilter} antes de la negociación, porque un
     * {@code .type(...)} en el {@link RestResponse} solo cambiaría la cabecera
     * y no el serializador. Los tipos de {@code @Produces} son los que
     * Quarkus negocia.
     *
     * @param format {@code json}, {@code xml}, {@code pdf}, {@code csv} o
     *               {@code xlsx}; lo procesa el filtro y se declara aquí para
     *               que aparezca en Swagger/OpenAPI
     */
    @Path("/greeting-xml")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
            GreetingPdfMessageBodyWriter.APPLICATION_PDF, UserCsvMessageBodyWriter.TEXT_CSV,
            UserExcelMessageBodyWriter.APPLICATION_XLSX })
    public RestResponse<GreetingDto> indexXml(@QueryParam(FormatQueryParamFilter.FORMAT) String format) {
        User user = new User("Andres", 40, "andres@correo.example");
        GreetingDto greeting = new GreetingDto("Hola mundo Quarkus!", LocalDateTime.now()).withUser(user);

        return RestResponse.ResponseBuilder.<GreetingDto>create(RestResponse.Status.OK)
                .entity(greeting)
                .build();
    }

    /**
     * GET /api/headers → 200 OK con los valores de los headers del request,
     * en JSON o XML: {"clientId": "...", "clientVersion": "...",
     * "contentTypeHeader": "..."}
     * <ul>
     * <li>{@code X-Client-Id}: requerido ({@code @NotBlank}). Si falta o viene
     * vacío → 400 Bad Request con el detalle de la validación</li>
     * <li>{@code X-Client-Version}: opcional. Si no viene, {@code clientVersion}
     * sale como {@code null}; si viene, debe tener formato de versión
     * ({@code @Pattern}: 1.0, 2.1.0...)</li>
     * <li>{@code Accept}: el header que envió el cliente, en
     * {@code contentTypeHeader}</li>
     * </ul>
     * {@code @HeaderParam} solo inyecta el valor, o {@code null} si el header
     * no viene: las reglas de Bean Validation (Hibernate Validator) son las que
     * lo hacen requerido. Quarkus las valida antes de ejecutar el método.
     */
    @Path("/headers")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public RestResponse<ClientHeadersDto> headers(
            @HeaderParam(CLIENT_ID_HEADER)
            @NotBlank(message = "El header " + CLIENT_ID_HEADER + " es requerido") String clientId,
            // opcional: @Pattern solo se aplica si el header viene (null es válido)
            @HeaderParam(CLIENT_VERSION_HEADER)
            @Pattern(regexp = "\\d+(\\.\\d+){1,2}",
                    message = "El header " + CLIENT_VERSION_HEADER + " debe tener formato de versión, p. ej. 2.1.0")
            String clientVersion,
            @HeaderParam("Accept") String contentTypeHeader) {
        return RestResponse.ResponseBuilder.<ClientHeadersDto>create(RestResponse.Status.OK)
                .entity(new ClientHeadersDto(clientId, clientVersion, contentTypeHeader))
                .build();
    }

    /**
     * GET /api/hello?name=Ana&amp;age=40&amp;birthDate=1985-10-26&amp;categories=java,quarkus
     * → 200 OK con un saludo para ese nombre, su edad, su fecha de nacimiento
     * y sus categorías, en JSON o en XML según el {@code Accept} (o
     * {@code ?format=json|xml}):
     * <pre>
     * {"message": "Hola, Ana!", "name": "Ana", "age": 40, "birthDate": "1985-10-26",
     *  "categories": ["java", "quarkus"]}
     * &lt;response&gt;...&lt;categories&gt;java&lt;/categories&gt;&lt;categories&gt;quarkus&lt;/categories&gt;&lt;/response&gt;
     * </pre>
     * {@code name} saluda a "Mundo" si no viene ({@code @DefaultValue}); si
     * viene, no puede estar vacío ({@code @NotBlank}) y debe tener entre 3 y
     * {@value #NAME_MAX_LENGTH} caracteres ({@code @Size}). Si no cumple → 400
     * Bad Request con el detalle de la validación.
     * <p>
     * {@code age} es un número entero ({@link Integer}): requerido
     * ({@code @NotNull}) y entre {@value #AGE_MIN} y {@value #AGE_MAX}
     * ({@code @Min}/{@code @Max}). Si falta o está fuera de rango → 400 con el
     * detalle; si no es un número (p. ej. {@code age=abc}) Quarkus no puede
     * convertirlo y, como manda JAX-RS para un query param, responde 404.
     * <p>
     * {@code birthDate} es una fecha ({@link LocalDate}) en formato ISO-8601
     * ({@code yyyy-MM-dd}): requerida ({@code @NotNull}) y en el pasado
     * ({@code @Past}). Si falta o no es pasada → 400; si no es una fecha válida
     * (p. ej. {@code 2026-13-45} o {@code 26/10/1985}) → 404, igual que
     * {@code age}.
     * <p>
     * {@code categories} es una lista de textos ({@code List<String>}): se
     * envía repitiendo el parámetro ({@code ?categories=java&categories=quarkus})
     * o separada por comas gracias a {@code @Separator(",")}
     * ({@code ?categories=java,quarkus}). Es requerida ({@code @NotEmpty}),
     * admite hasta {@value #CATEGORIES_MAX} ({@code @Size}) y cada categoría
     * debe tener texto ({@code List<@NotBlank String>}); si una falla, el
     * reporte indica su posición ({@code categories[1]}).
     * <p>
     * Sin DTO ni {@code Map}: la respuesta es un {@link ObjectNode} de
     * Jackson, un nodo JSON que se arma con {@code put}. Jackson lo serializa
     * a JSON y {@code XmlMessageBodyWriter} a XML (con raíz {@code <response>}).
     */
    @Path("/hello")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public RestResponse<ObjectNode> handler(
        //     @QueryParam("name")
        @RestQuery
        @DefaultValue("Mundo")
        @NotBlank(message = "El parámetro name es requerido")
        @Size(min=3, max = NAME_MAX_LENGTH, message = "El parámetro name debe tener entre {min} y {max} caracteres")
        String name,
        // Integer y no int: un int sin valor llega como 0 y @NotNull no podría detectarlo
        @RestQuery
        @NotNull(message = "El parámetro age es requerido")
        @Min(value = AGE_MIN, message = "El parámetro age debe ser mayor o igual a {value}")
        @Max(value = AGE_MAX, message = "El parámetro age debe ser menor o igual a {value}")
        Integer age,
        // fecha en formato ISO-8601 (yyyy-MM-dd): Quarkus la convierte a LocalDate
        @RestQuery
        @NotNull(message = "El parámetro birthDate es requerido")
        @Past(message = "El parámetro birthDate debe ser una fecha pasada")
        LocalDate birthDate,
        // lista: ?categories=java&categories=quarkus o, con @Separator, ?categories=java,quarkus
        @RestQuery
        @Separator(",")
        @NotEmpty(message = "El parámetro categories es requerido: envía al menos una categoría")
        @Size(max = CATEGORIES_MAX, message = "El parámetro categories admite hasta {max} categorías")
        // la regla dentro del tipo genérico se aplica a cada elemento de la lista
        List<@NotBlank(message = "Las categorías de categories no pueden estar vacías") String> categories) {
        ObjectNode body = JsonNodeFactory.instance.objectNode()
                .put("message", "Hola, " + name + "!")
                .put("name", name)
                // put(String, Integer) lo escribe como número, no como texto
                .put("age", age)
                // un ObjectNode no sabe de java.time: la fecha va como texto ISO-8601
                .put("birthDate", birthDate.toString());
        // la lista va como un arreglo JSON de textos
        ArrayNode categoryArray = body.putArray("categories");
        categories.forEach(categoryArray::add);

        return RestResponse.ResponseBuilder.<ObjectNode>create(RestResponse.Status.OK)
                .entity(body)
                .build();
    }

    /**
     * GET /api/categories?category=java&amp;category=quarkus → 200 OK con la
     * lista de categorías recibida, en JSON o XML:
     * <pre>
     * {"categories": ["java", "quarkus"], "total": 2}
     * &lt;response&gt;&lt;categories&gt;java&lt;/categories&gt;&lt;categories&gt;quarkus&lt;/categories&gt;&lt;total&gt;2&lt;/total&gt;&lt;/response&gt;
     * </pre>
     * Cada categoría es un query param independiente con el mismo nombre,
     * {@code category} (en singular, porque cada uno lleva una sola), y JAX-RS
     * junta todos sus valores, en orden, en el argumento {@code categories}
     * (en plural, porque es la lista): {@code @RestQuery("category")} separa
     * el nombre del parámetro en la URL del nombre de la variable. Sin
     * {@code @Separator}, una coma no separa: {@code ?category=java,quarkus}
     * es una sola categoría, {@code "java,quarkus"}.
     * <p>
     * Validación de la lista completa:
     * <ul>
     * <li>{@code @NotEmpty}: al menos una categoría. Sin el parámetro llega una
     * lista vacía (no {@code null}), por eso {@code @NotEmpty} y no
     * {@code @NotNull}. Los valores vacíos ({@code ?category=}) Quarkus los
     * descarta antes de validar</li>
     * <li>{@code @Size}: hasta {@value #CATEGORIES_MAX} categorías</li>
     * </ul>
     * Validación de cada elemento, con las reglas dentro del tipo genérico
     * ({@code List<@NotBlank @Size String>}):
     * <ul>
     * <li>{@code @NotBlank}: sin categorías en blanco ({@code ?category=%20})</li>
     * <li>{@code @Size}: entre {@value #CATEGORY_MIN_LENGTH} y
     * {@value #CATEGORY_MAX_LENGTH} caracteres</li>
     * </ul>
     * Si algo falla → 400 con el {@code ViolationReport}; para un elemento, el
     * campo indica su posición: {@code categories.categories[1].<list element>}.
     * Los valores repetidos se mantienen.
     */
    @Path("/categories")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public RestResponse<ObjectNode> categories(
            @RestQuery("category")
            // reglas de la lista completa
            @NotEmpty(message = "El parámetro category es requerido: envía al menos una categoría")
            @Size(max = CATEGORIES_MAX, message = "El parámetro category admite hasta {max} categorías")
            // reglas de cada elemento, dentro del tipo genérico
            List<@NotBlank(message = "Las categorías no pueden estar en blanco")
                 @Size(min = CATEGORY_MIN_LENGTH, max = CATEGORY_MAX_LENGTH,
                       message = "Cada categoría debe tener entre {min} y {max} caracteres")
                 String> categories) {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        ArrayNode categoryArray = body.putArray("categories");
        categories.forEach(categoryArray::add);
        body.put("total", categories.size());

        return RestResponse.ResponseBuilder.<ObjectNode>create(RestResponse.Status.OK)
                .entity(body)
                .build();
    }
}
