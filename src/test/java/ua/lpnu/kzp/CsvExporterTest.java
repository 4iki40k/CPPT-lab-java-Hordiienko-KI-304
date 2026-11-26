package ua.lpnu.kzp;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє анотацію та універсальність рефлексивного експортера на класах іншої предметної області. */
class CsvExporterTest
{
    private static final String NL = System.lineSeparator();

    @TempDir
    Path directory;

    /** Категорія тестової книги. */
    private enum Genre
    {
        NOVEL,
        POETRY
    }

    /** Тестовий клас іншої предметної області з int, enum і полем без анотації. */
    private static final class Book
    {
        @CsvColumn("назва")
        private final String title;

        @CsvColumn("рік")
        private final int year;

        @CsvColumn("жанр")
        private final Genre genre;

        /** Не експортується, бо не має анотації. */
        private final String note = "внутрішнє";

        private Book(String title, int year, Genre genre)
        {
            this.title = title;
            this.year = year;
            this.genre = genre;
        }
    }

    /** Базовий клас для перевірки обходу суперкласів. */
    private static class Base
    {
        @CsvColumn("код")
        private final String code;

        private Base(String code)
        {
            this.code = code;
        }
    }

    /** Підклас з власним полем. */
    private static final class Derived extends Base
    {
        @CsvColumn("кількість")
        private final int amount;

        private Derived(String code, int amount)
        {
            super(code);
            this.amount = amount;
        }
    }

    /** Клас без жодного анотованого поля. */
    private static final class Plain
    {
        private final String name = "x";
    }

    /** Тестова анотація без RUNTIME: недоступна рефлексії під час виконання. */
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    private @interface CompileTimeOnly
    {
    }

    @Test
    void annotationIsRuntimeFieldAnnotationWithValue() throws NoSuchMethodException
    {
        Retention retention = CsvColumn.class.getAnnotation(Retention.class);
        Target target = CsvColumn.class.getAnnotation(Target.class);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertArrayEquals(new ElementType[] {ElementType.FIELD}, target.value());
        assertEquals(String.class, CsvColumn.class.getMethod("value").getReturnType());
        assertEquals(1, CsvColumn.class.getDeclaredMethods().length);
    }

    @Test
    void annotationOfDomainFieldIsReadAtRuntime() throws NoSuchFieldException
    {
        CsvColumn column = UtilityReadingRecord.class.getDeclaredField("meter").getAnnotation(CsvColumn.class);
        assertNotNull(column);
        assertEquals("лічильник", column.value());
    }

    @Test
    void annotationWithoutRuntimeRetentionIsInvisible()
    {
        assertTrue(Arrays.stream(Book.class.getDeclaredFields()).noneMatch(field -> field.isAnnotationPresent(CompileTimeOnly.class)));
    }

    @Test
    void headerIsSortedByJavaFieldName() throws DataStorageException
    {
        // genre, title, year — порядок імен полів, а не порядок оголошення.
        assertEquals(List.of("жанр", "назва", "рік"), CsvExporter.header(Book.class));
        assertEquals(List.of("дата", "лічильник", "тариф", "значення"), CsvExporter.header(UtilityReadingRecord.class));
    }

    @Test
    void exportsOtherDomainClassWithIntAndEnum() throws DataStorageException
    {
        String csv = CsvExporter.toCsv(Book.class, List.of(new Book("Кобзар", 1840, Genre.POETRY), new Book("Місто", 1928, Genre.NOVEL)));
        assertEquals("жанр,назва,рік" + NL + "POETRY,Кобзар,1840" + NL + "NOVEL,Місто,1928" + NL, csv);
    }

    @Test
    void fieldWithoutAnnotationIsNotExported() throws DataStorageException
    {
        String csv = CsvExporter.toCsv(Book.class, List.of(new Book("Кобзар", 1840, Genre.POETRY)));
        assertTrue(!csv.contains("внутрішнє"), csv);
    }

    @Test
    void inheritedAnnotatedFieldsAreExported() throws DataStorageException
    {
        // amount < code: поле підкласу стоїть перед полем суперкласу, бо сортування за іменем.
        assertEquals("кількість,код" + NL + "3,A-1" + NL, CsvExporter.toCsv(Derived.class, List.of(new Derived("A-1", 3))));
    }

    @Test
    void emptyListStillHasHeader() throws DataStorageException
    {
        assertEquals("жанр,назва,рік" + NL, CsvExporter.toCsv(Book.class, List.of()));
    }

    @Test
    void nullValueBecomesEmptyField() throws DataStorageException
    {
        assertEquals("жанр,назва,рік" + NL + ",Без жанру,2000" + NL, CsvExporter.toCsv(Book.class, List.of(new Book("Без жанру", 2000, null))));
    }

    @Test
    void escapesCommaQuoteAndLineBreaks()
    {
        assertEquals("просто", CsvExporter.escape("просто"));
        assertEquals("\"Вода, гаряча\"", CsvExporter.escape("Вода, гаряча"));
        assertEquals("\"Газ \"\"Схід\"\"\"", CsvExporter.escape("Газ \"Схід\""));
        assertEquals("\"a\nb\"", CsvExporter.escape("a\nb"));
        assertEquals("\"a\rb\"", CsvExporter.escape("a\rb"));
        assertEquals("", CsvExporter.escape(null));
        assertEquals("1.5", CsvExporter.escape(1.5));
    }

    @Test
    void escapedHeaderAndValuesInExportedText() throws DataStorageException
    {
        String csv = CsvExporter.toCsv(Book.class, List.of(new Book("Карти, що \"говорять\"", 1988, Genre.NOVEL)));
        assertTrue(csv.contains("NOVEL,\"Карти, що \"\"говорять\"\"\",1988"));
    }

    @Test
    void classWithoutAnnotatedFieldsIsStorageError()
    {
        DataStorageException exception = assertThrows(DataStorageException.class, () -> CsvExporter.toCsv(Plain.class, List.of(new Plain())));
        assertTrue(exception.getMessage().contains("@CsvColumn"));
        assertThrows(DataStorageException.class, () -> CsvExporter.header(Plain.class));
    }

    @Test
    void nullRecordIsStorageErrorWithPosition()
    {
        List<Book> books = Arrays.asList(new Book("a", 1, Genre.NOVEL), null);
        DataStorageException exception = assertThrows(DataStorageException.class, () -> CsvExporter.toCsv(Book.class, books));
        assertTrue(exception.getMessage().contains("2"));
    }

    @Test
    void nullArgumentsAreRejected()
    {
        assertThrows(NullPointerException.class, () -> CsvExporter.toCsv(null, List.of()));
        assertThrows(NullPointerException.class, () -> CsvExporter.toCsv(Book.class, null));
        assertThrows(NullPointerException.class, () -> CsvExporter.write(null, Book.class, List.of()));
    }

    @Test
    void writesUtf8AndCreatesParentDirectories() throws DataStorageException, IOException
    {
        Path path = directory.resolve("nested").resolve("deeper").resolve("books.csv");
        CsvExporter.write(path, Book.class, List.of(new Book("Їжак і ґудзик", 2001, Genre.NOVEL)));
        byte[] bytes = Files.readAllBytes(path);
        assertEquals("жанр,назва,рік" + NL + "NOVEL,Їжак і ґудзик,2001" + NL, new String(bytes, StandardCharsets.UTF_8));
        assertTrue(bytes.length > 0 && bytes[0] != (byte) 0xEF, "UTF-8 без BOM");
    }

    @Test
    void shorterExportTruncatesExistingFile() throws DataStorageException, IOException
    {
        Path path = directory.resolve("books.csv");
        Files.writeString(path, "старий довгий вміст\n".repeat(20), StandardCharsets.UTF_8);
        CsvExporter.write(path, Book.class, List.of());
        assertEquals("жанр,назва,рік" + NL, Files.readString(path, StandardCharsets.UTF_8));
    }

    @Test
    void directoryAsTargetIsWrappedWithIoCause() throws IOException
    {
        Path target = Files.createDirectory(directory.resolve("not-a-file"));
        DataStorageException exception = assertThrows(DataStorageException.class, () -> CsvExporter.write(target, Book.class, List.of()));
        assertInstanceOf(IOException.class, exception.getCause());
        assertTrue(exception.getMessage().contains(target.toString()));
    }

    @Test
    void fileAsParentDirectoryIsWrappedWithIoCause() throws IOException
    {
        Path blocker = Files.writeString(directory.resolve("blocker"), "x", StandardCharsets.UTF_8);
        DataStorageException exception = assertThrows(DataStorageException.class, () -> CsvExporter.write(blocker.resolve("books.csv"), Book.class, List.of()));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void exportDoesNotModifyRecordsOrList() throws DataStorageException
    {
        List<UtilityReadingRecord> records = List.of(new UtilityReadingRecord("Вода", java.time.LocalDate.of(2026, 9, 1), 5, 30));
        List<UtilityReadingRecord> copy = List.copyOf(records);
        CsvExporter.toCsv(UtilityReadingRecord.class, records);
        assertEquals(copy, records);
    }

    @Test
    void storageExceptionKeepsMessageAndCause()
    {
        IOException cause = new IOException("диск");
        DataStorageException withCause = new DataStorageException("Не вдалося записати", cause);
        assertEquals("Не вдалося записати", withCause.getMessage());
        assertEquals(cause, withCause.getCause());
        assertEquals(null, new DataStorageException("Без причини").getCause());
        assertInstanceOf(Exception.class, withCause);
        assertTrue(!RuntimeException.class.isAssignableFrom(DataStorageException.class), "виняток має бути checked");
    }
}
