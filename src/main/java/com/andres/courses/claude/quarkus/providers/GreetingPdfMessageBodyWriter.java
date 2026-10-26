package com.andres.courses.claude.quarkus.providers;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import com.andres.courses.claude.quarkus.models.GreetingDto;
import com.andres.courses.claude.quarkus.models.User;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Serializa un {@link GreetingDto} a PDF (application/pdf) con OpenPDF.
 * <p>
 * El documento tiene una cabecera con el mensaje del saludo, una tabla con
 * los datos del {@link User}, una descripción de la clase y un pie con la
 * fecha. Igual que con XML, el método del controller debe declarar
 * {@value #APPLICATION_PDF} en su {@code @Produces} para que Quarkus use este
 * writer.
 * <p>
 * Va tipado como {@code MessageBodyWriter<Object>} y filtra en
 * {@link #isWriteable}: tipado como {@code MessageBodyWriter<GreetingDto>},
 * Quarkus REST no lo elige para un {@code RestResponse<GreetingDto>} y el PDF
 * sale con el {@code toString()} del DTO.
 */
@Provider
@Produces(GreetingPdfMessageBodyWriter.APPLICATION_PDF)
public class GreetingPdfMessageBodyWriter implements MessageBodyWriter<Object> {

    /** jakarta.ws.rs.core.MediaType no trae una constante para PDF. */
    public static final String APPLICATION_PDF = "application/pdf";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm:ss");

    // Paleta inspirada en los colores de Quarkus
    private static final Color QUARKUS_BLUE = new Color(0x46, 0x95, 0xEB);
    private static final Color QUARKUS_DARK = new Color(0x0D, 0x1C, 0x2C);
    private static final Color QUARKUS_RED = new Color(0xFF, 0x00, 0x4A);
    private static final Color ROW_LIGHT = new Color(0xEE, 0xF5, 0xFD);
    private static final Color GRAY = new Color(0x6B, 0x72, 0x80);

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Font.NORMAL, Color.WHITE);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, Color.WHITE);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Font.NORMAL, QUARKUS_DARK);
    private static final Font TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.NORMAL, Color.WHITE);
    private static final Font TABLE_KEY = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.NORMAL, QUARKUS_DARK);
    private static final Font TABLE_VALUE = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, QUARKUS_DARK);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, QUARKUS_DARK);
    private static final Font BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.NORMAL, QUARKUS_BLUE);
    private static final Font FOOTER = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Font.NORMAL, GRAY);

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return GreetingDto.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(Object entity, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        GreetingDto greeting = (GreetingDto) entity;
        // inline: el navegador lo muestra; con "attachment" lo descargaría
        httpHeaders.putSingle(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"greeting.pdf\"");

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, entityStream);
            // el stream de la respuesta lo cierra Quarkus, no OpenPDF
            writer.setCloseStream(false);

            document.addTitle("Saludo de Quarkus");
            document.addSubject("GreetingDto renderizado en PDF");
            document.addCreator("Quarkus + OpenPDF");
            document.open();

            document.add(header(greeting.message()));
            document.add(section("El usuario"));
            document.add(userTable(greeting.user()));
            document.add(section("¿Quién es User?"));
            document.add(userDescription());
            document.add(footer(greeting));
        } catch (DocumentException e) {
            throw new IOException("No se pudo generar el PDF del saludo", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    /** Banda azul con el mensaje del saludo como título. */
    private PdfPTable header(String message) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(QUARKUS_BLUE);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(20);
        cell.addElement(new Paragraph(message != null ? message : "¡Hola!", TITLE));
        Paragraph subtitle = new Paragraph("Un saludo servido por GET /api/greeting-xml en formato PDF", SUBTITLE);
        subtitle.setSpacingBefore(6);
        cell.addElement(subtitle);

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.addCell(cell);
        return table;
    }

    private Paragraph section(String title) {
        Paragraph paragraph = new Paragraph(title, SECTION);
        paragraph.setSpacingBefore(24);
        paragraph.setSpacingAfter(10);
        return paragraph;
    }

    /** Tabla de dos columnas (atributo y valor) con los datos del usuario. */
    private PdfPTable userTable(User user) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1, 2 });
        table.setHeaderRows(1);

        table.addCell(headerCell("Atributo"));
        table.addCell(headerCell("Valor"));

        if (user == null) {
            PdfPCell empty = valueCell("Este saludo no trae usuario", false);
            empty.setColspan(2);
            table.addCell(empty);
            return table;
        }

        addRow(table, "Nombre (name)", user.getName(), false);
        addRow(table, "Edad (age)", user.getAge() + " años", true);
        addRow(table, "Correo (email)", user.getEmail(), false);
        return table;
    }

    private void addRow(PdfPTable table, String key, String value, boolean shaded) {
        PdfPCell keyCell = new PdfPCell(new Phrase(key, TABLE_KEY));
        style(keyCell, shaded);
        table.addCell(keyCell);
        table.addCell(valueCell(value != null ? value : "-", shaded));
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER));
        cell.setBackgroundColor(QUARKUS_DARK);
        cell.setBorderColor(QUARKUS_DARK);
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell valueCell(String text, boolean shaded) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_VALUE));
        style(cell, shaded);
        return cell;
    }

    /** Filas alternas sombreadas y solo un borde inferior suave. */
    private void style(PdfPCell cell, boolean shaded) {
        cell.setBackgroundColor(shaded ? ROW_LIGHT : Color.WHITE);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(ROW_LIGHT);
        cell.setPadding(8);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    }

    /** Descripción simpática de la clase User. */
    private Paragraph userDescription() {
        Paragraph paragraph = new Paragraph();
        paragraph.setLeading(16);
        paragraph.add(new Phrase("User", BODY_BOLD));
        paragraph.add(new Phrase(" es el modelo más tranquilo del proyecto: vive en el package ", BODY));
        paragraph.add(new Phrase("models", BODY_BOLD));
        paragraph.add(new Phrase(" y solo necesita tres atributos para presentarse: su nombre, su edad "
                + "y su correo. No guarda secretos ni lógica de negocio; lo suyo es viajar de un lado a otro "
                + "dentro del ", BODY));
        paragraph.add(new Phrase("GreetingDto", BODY_BOLD));
        paragraph.add(new Phrase(", un record de Java que lo acompaña junto al mensaje y la fecha.", BODY));

        paragraph.add(new Phrase("\n\nLo mejor es que nunca cambia de forma, solo de ropa: el mismo objeto sale "
                + "en JSON, en XML o, como estás viendo, en PDF. Eso es content negotiation: el cliente pide el "
                + "formato con el header Accept o con ", BODY));
        paragraph.add(new Phrase("?format=pdf", BODY_BOLD));
        paragraph.add(new Phrase(", y Quarkus elige el serializador que mejor le queda.", BODY));
        return paragraph;
    }

    private Paragraph footer(GreetingDto greeting) {
        String date = greeting.date() != null ? greeting.date().format(DATE_FORMAT) : "en algún momento";
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(36);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.add(new Phrase("Generado el " + date + "  ·  Hecho con ", FOOTER));
        footer.add(new Phrase("Quarkus", FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 9, Font.NORMAL, QUARKUS_RED)));
        footer.add(new Phrase(" y OpenPDF", FOOTER));
        return footer;
    }
}
