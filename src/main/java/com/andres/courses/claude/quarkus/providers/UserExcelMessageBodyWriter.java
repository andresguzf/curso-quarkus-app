package com.andres.courses.claude.quarkus.providers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.andres.courses.claude.quarkus.models.GreetingDto;
import com.andres.courses.claude.quarkus.models.User;

/**
 * Serializa usuarios a Excel (.xlsx) con Apache POI: una hoja con una fila de
 * cabecera con los campos de {@link User} y una fila por usuario.
 *
 * <pre>
 * | name   | age | email                 |
 * | Andres | 40  | andres@correo.example |
 * </pre>
 *
 * Igual que el CSV, acepta tres tipos de respuesta:
 * <ul>
 * <li>{@code Collection<User>}: una fila por usuario (una tabla o lista de
 * usuarios)</li>
 * <li>{@link User}: una sola fila</li>
 * <li>{@link GreetingDto}: la fila de su {@code user}; el mensaje y la fecha
 * no van en el Excel. Sin usuario, solo la cabecera</li>
 * </ul>
 * La cabecera va en negrita con fondo, inmovilizada y con autofiltro; la edad
 * es una celda numérica (Excel puede sumarla u ordenarla), no texto.
 * <p>
 * El nombre de la hoja es configurable: por defecto es
 * {@value #DEFAULT_SHEET_NAME}; para cambiarlo en toda la API basta con
 * modificar {@link #DEFAULT_SHEET_NAME}, y el constructor
 * {@link #UserExcelMessageBodyWriter(String)} permite usar otro en una
 * instancia.
 * <p>
 * Igual que los writers de PDF y CSV, va tipado como
 * {@code MessageBodyWriter<Object>} y filtra en {@link #isWriteable}, para
 * que Quarkus REST lo elija con un {@code RestResponse<...>}.
 */
@Provider
@Produces(UserExcelMessageBodyWriter.APPLICATION_XLSX)
public class UserExcelMessageBodyWriter implements MessageBodyWriter<Object> {

    /** Media type oficial de los .xlsx; jakarta.ws.rs.core.MediaType no trae una constante. */
    public static final String APPLICATION_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** Nombre de la hoja que usa Quarkus al crear el provider. */
    public static final String DEFAULT_SHEET_NAME = "Usuarios";

    /** Columnas del Excel: los campos de {@link User}, en este orden. */
    static final List<String> COLUMNS = List.of("name", "age", "email");

    /** Fondo de la cabecera: el azul de Quarkus, el mismo que usa el PDF (#4695EB). */
    private static final byte[] QUARKUS_BLUE = { (byte) 0x46, (byte) 0x95, (byte) 0xEB };

    /** Ancho de cada columna, en caracteres (POI lo mide en 1/256 de carácter). */
    private static final int[] COLUMN_WIDTHS = { 24, 8, 32 };

    private final String sheetName;

    /** Con la hoja por defecto ({@value #DEFAULT_SHEET_NAME}). Es el que usa Quarkus. */
    public UserExcelMessageBodyWriter() {
        this(DEFAULT_SHEET_NAME);
    }

    /**
     * @param sheetName nombre de la hoja; debe cumplir las reglas de Excel
     *                  (1 a 31 caracteres, sin {@code \ / ? * [ ] :} ni
     *                  apóstrofo al principio o al final)
     */
    public UserExcelMessageBodyWriter(String sheetName) {
        // lanza IllegalArgumentException si Excel no lo admite
        WorkbookUtil.validateSheetName(sheetName);
        this.sheetName = sheetName;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (GreetingDto.class.isAssignableFrom(type) || User.class.isAssignableFrom(type)) {
            return true;
        }
        // Collection<User> (List, Set...): se comprueba el tipo de sus elementos
        return Collection.class.isAssignableFrom(type)
                && genericType instanceof ParameterizedType parameterized
                && parameterized.getActualTypeArguments()[0] instanceof Class<?> element
                && User.class.isAssignableFrom(element);
    }

    @Override
    public void writeTo(Object entity, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        // attachment: el navegador lo descarga como archivo
        httpHeaders.putSingle(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.xlsx\"");
        try (Workbook workbook = toWorkbook(users(entity))) {
            // write() no cierra el stream de la respuesta: lo cierra Quarkus
            workbook.write(entityStream);
        }
    }

    /** Los usuarios que contiene la respuesta, sea una colección, un User o un GreetingDto. */
    private static List<User> users(Object entity) {
        return switch (entity) {
            case Collection<?> collection -> collection.stream().map(User.class::cast).toList();
            case User user -> List.of(user);
            case GreetingDto greeting when greeting.user() != null -> List.of(greeting.user());
            default -> List.of();
        };
    }

    /** Libro con una hoja: cabecera + una fila por usuario. Una lista vacía deja solo la cabecera. */
    Workbook toWorkbook(List<User> users) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);

        XSSFCellStyle headerStyle = headerStyle(workbook);
        Row header = sheet.createRow(0);
        for (int column = 0; column < COLUMNS.size(); column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(COLUMNS.get(column));
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(column, COLUMN_WIDTHS[column] * 256);
        }

        int rowIndex = 1;
        for (User user : users) {
            Row row = sheet.createRow(rowIndex++);
            setText(row.createCell(0), user.getName());
            row.createCell(1).setCellValue(user.getAge());
            setText(row.createCell(2), user.getEmail());
        }

        // cabecera siempre visible al hacer scroll y con filtros por columna
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, rowIndex - 1), 0, COLUMNS.size() - 1));
        return workbook;
    }

    /** Un valor nulo deja la celda vacía (en blanco), como en el CSV. */
    private static void setText(Cell cell, String value) {
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private static XSSFCellStyle headerStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        // color RGB explícito: los colores indexados se ven distintos según el visor
        style.setFillForegroundColor(new XSSFColor(QUARKUS_BLUE, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }
}
