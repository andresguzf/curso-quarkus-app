package com.andres.courses.claude.quarkus.providers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.andres.courses.claude.quarkus.models.GreetingDto;
import com.andres.courses.claude.quarkus.models.User;

/**
 * Pruebas unitarias de {@link UserExcelMessageBodyWriter}: JUnit puro, sin
 * arrancar Quarkus. Cubren el contenido de la hoja (también con varios
 * usuarios), los tipos de celda, el formato de la cabecera, el nombre de hoja
 * configurable y qué tipos acepta el writer.
 */
class UserExcelMessageBodyWriterTest {

    private final UserExcelMessageBodyWriter writer = new UserExcelMessageBodyWriter();

    // tipos genéricos reales para probar isWriteable con colecciones
    @SuppressWarnings("unused")
    private List<User> users;
    @SuppressWarnings("unused")
    private Set<User> userSet;
    @SuppressWarnings("unused")
    private List<String> strings;

    @Test
    void unUsuarioEsLaCabeceraYUnaFila() throws IOException {
        try (Workbook workbook = writer.toWorkbook(List.of(new User("Andres", 40, "andres@correo.example")))) {
            Sheet sheet = workbook.getSheetAt(0);

            assertThat(sheet.getLastRowNum(), is(1));
            assertRow(sheet.getRow(0), "name", "age", "email");
            assertUser(sheet.getRow(1), "Andres", 40, "andres@correo.example");
        }
    }

    @Test
    void unaListaDeUsuariosEsUnaFilaPorUsuarioEnOrden() throws IOException {
        try (Workbook workbook = writer.toWorkbook(List.of(
                new User("Ana", 31, "ana@correo.example"),
                new User("Bruno", 25, "bruno@correo.example"),
                new User("Carla", 47, "carla@correo.example")))) {
            Sheet sheet = workbook.getSheetAt(0);

            assertThat(sheet.getLastRowNum(), is(3));
            assertUser(sheet.getRow(1), "Ana", 31, "ana@correo.example");
            assertUser(sheet.getRow(2), "Bruno", 25, "bruno@correo.example");
            assertUser(sheet.getRow(3), "Carla", 47, "carla@correo.example");
        }
    }

    @Test
    void unaListaVaciaEsSoloLaCabecera() throws IOException {
        try (Workbook workbook = writer.toWorkbook(List.of())) {
            Sheet sheet = workbook.getSheetAt(0);

            assertThat(sheet.getLastRowNum(), is(0));
            assertRow(sheet.getRow(0), "name", "age", "email");
        }
    }

    @Test
    void losValoresConComasYComillasNoNecesitanEscaparse() throws IOException {
        // a diferencia del CSV, cada valor va en su propia celda
        try (Workbook workbook = writer.toWorkbook(List.of(new User("Pérez, Ana \"la dev\"", 31, "ana@correo.example")))) {
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue(), is("Pérez, Ana \"la dev\""));
        }
    }

    @Test
    void unValorNuloDejaLaCeldaEnBlanco() throws IOException {
        try (Workbook workbook = writer.toWorkbook(List.of(new User("Ana", 31, null)))) {
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(2).getCellType(), is(CellType.BLANK));
        }
    }

    @Test
    void laCabeceraVaEnNegritaConFondoInmovilizadaYConAutofiltro() throws IOException {
        try (XSSFWorkbook workbook = (XSSFWorkbook) writer.toWorkbook(List.of(
                new User("Ana", 31, "ana@correo.example"),
                new User("Bruno", 25, "bruno@correo.example")))) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            XSSFCellStyle style = sheet.getRow(0).getCell(0).getCellStyle();

            assertThat(style.getFont().getBold(), is(true));
            assertThat(style.getFillPattern(), is(FillPatternType.SOLID_FOREGROUND));
            assertThat(style.getFillForegroundColorColor().getARGBHex(), is("FF4695EB"));

            PaneInformation pane = sheet.getPaneInformation();
            assertThat(pane, notNullValue());
            assertThat(pane.isFreezePane(), is(true));
            assertThat(pane.getHorizontalSplitPosition(), is((short) 1));

            // el autofiltro cubre la cabecera y todas las filas de usuarios
            assertThat(sheet.getCTWorksheet().getAutoFilter().getRef(), is("A1:C3"));
        }
    }

    @Test
    void laHojaPorDefectoSeLlamaUsuarios() throws IOException {
        assertThat(UserExcelMessageBodyWriter.DEFAULT_SHEET_NAME, is("Usuarios"));
        try (Workbook workbook = writer.toWorkbook(List.of())) {
            assertThat(workbook.getSheetName(0), is("Usuarios"));
        }
    }

    @Test
    void elNombreDeLaHojaEsConfigurable() throws IOException {
        try (Workbook workbook = new UserExcelMessageBodyWriter("Equipo").toWorkbook(List.of())) {
            assertThat(workbook.getSheetName(0), is("Equipo"));
        }
    }

    @Test
    void rechazaNombresDeHojaQueExcelNoAdmite() {
        for (String invalid : List.of("", "a/b", "[usuarios]", "un nombre de hoja de más de 31 caracteres")) {
            assertThrows(IllegalArgumentException.class, () -> new UserExcelMessageBodyWriter(invalid));
        }
    }

    @Test
    void writeToEscribeUnXlsxConElUserDelSaludoYPideDescargarlo() throws IOException {
        GreetingDto greeting = new GreetingDto("Hola mundo Quarkus!", LocalDateTime.now())
                .withUser(new User("Andrés", 40, "andres@correo.example"));
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        writer.writeTo(greeting, GreetingDto.class, GreetingDto.class, null, null, headers, body);

        assertThat(headers.getFirst("Content-Disposition"), is("attachment; filename=\"users.xlsx\""));
        // el mensaje y la fecha del saludo no van en el Excel, solo el user
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(body.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum(), is(1));
            assertUser(sheet.getRow(1), "Andrés", 40, "andres@correo.example");
        }
    }

    @Test
    void unSaludoSinUsuarioEsSoloLaCabecera() throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        writer.writeTo(new GreetingDto("Hola"), GreetingDto.class, GreetingDto.class, null, null,
                new MultivaluedHashMap<>(), body);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(body.toByteArray()))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum(), is(0));
        }
    }

    @Test
    void aceptaGreetingDtoUserYColeccionesDeUser() throws NoSuchFieldException {
        assertThat(writer.isWriteable(GreetingDto.class, GreetingDto.class, null, null), is(true));
        assertThat(writer.isWriteable(User.class, User.class, null, null), is(true));
        assertThat(writer.isWriteable(List.class, genericType("users"), null, null), is(true));
        assertThat(writer.isWriteable(Set.class, genericType("userSet"), null, null), is(true));
    }

    @Test
    void rechazaOtrosTipos() throws NoSuchFieldException {
        assertThat(writer.isWriteable(String.class, String.class, null, null), is(false));
        assertThat(writer.isWriteable(List.class, genericType("strings"), null, null), is(false));
        // una lista sin tipo genérico no se sabe si es de usuarios
        assertThat(writer.isWriteable(List.class, List.class, null, null), is(false));
    }

    private static void assertRow(Row row, String... values) {
        assertThat(row.getLastCellNum(), is((short) values.length));
        for (int i = 0; i < values.length; i++) {
            assertThat(row.getCell(i).getStringCellValue(), is(values[i]));
        }
    }

    /** name y email como texto; age como número, para que Excel pueda sumarla u ordenarla. */
    private static void assertUser(Row row, String name, int age, String email) {
        assertThat(row.getCell(0).getStringCellValue(), is(name));
        assertThat(row.getCell(1).getCellType(), is(CellType.NUMERIC));
        assertThat(row.getCell(1).getNumericCellValue(), is((double) age));
        assertThat(row.getCell(2).getStringCellValue(), is(email));
    }

    private Type genericType(String field) throws NoSuchFieldException {
        return getClass().getDeclaredField(field).getGenericType();
    }
}
