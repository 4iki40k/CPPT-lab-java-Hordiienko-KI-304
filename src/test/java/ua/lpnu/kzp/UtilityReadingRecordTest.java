package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє запис варіанта 9, його розбір і round-trip через CSV-файл. */
class UtilityReadingRecordTest
{
    private static final LocalDate SEPTEMBER = LocalDate.of(2026, 9, 1);

    @TempDir
    Path directory;

    private static List<UtilityReadingRecord> sample()
    {
        return List.of(
                new UtilityReadingRecord("Електроенергія", SEPTEMBER, 120.5, 4.32),
                new UtilityReadingRecord("Вода, гаряча", SEPTEMBER, 3.2, 98.5),
                new UtilityReadingRecord("Газ \"Схід\"", SEPTEMBER, 25, 7.96),
                new UtilityReadingRecord("Лічильник\nв підвалі", LocalDate.of(2024, 2, 29), 0, 0),
                new UtilityReadingRecord("Електроенергія", LocalDate.of(2026, 10, 1), 0.1, 1e-3),
                new UtilityReadingRecord("Вода", LocalDate.of(2026, 10, 1), 1.0 / 3, 30));
    }

    @Test
    void constructorStoresValuesAndStripsMeter()
    {
        UtilityReadingRecord record = new UtilityReadingRecord("  Вода  ", SEPTEMBER, 5, 30);
        assertEquals("Вода", record.getMeter());
        assertEquals(SEPTEMBER, record.getDate());
        assertEquals(5.0, record.getValue());
        assertEquals(30.0, record.getTariff());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, -0.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void constructorRejectsBadNumbers(double bad)
    {
        assertThrows(IllegalArgumentException.class, () -> new UtilityReadingRecord("Вода", SEPTEMBER, bad, 1));
        assertThrows(IllegalArgumentException.class, () -> new UtilityReadingRecord("Вода", SEPTEMBER, 1, bad));
    }

    @Test
    void constructorRejectsMissingText()
    {
        assertThrows(IllegalArgumentException.class, () -> new UtilityReadingRecord(null, SEPTEMBER, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new UtilityReadingRecord("  ", SEPTEMBER, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new UtilityReadingRecord("Вода", null, 1, 1));
    }

    @Test
    void zeroIsBoundaryValue()
    {
        UtilityReadingRecord record = new UtilityReadingRecord("Вода", SEPTEMBER, 0, 0);
        assertEquals(0.0, record.getValue());
    }

    @Test
    void fromPolymorphicReadingKeepsCommonFields()
    {
        UtilityReading electricity = new ElectricityReading("Електроенергія", "2026-09-01", 100, 4, 30, 0.5);
        UtilityReadingRecord record = UtilityReadingRecord.from(electricity);
        assertEquals(new UtilityReadingRecord("Електроенергія", SEPTEMBER, 100, 4), record);
        assertThrows(NullPointerException.class, () -> UtilityReadingRecord.from(null));
    }

    @Test
    void equalsAndHashCodeUseAllFields()
    {
        UtilityReadingRecord base = new UtilityReadingRecord("Вода", SEPTEMBER, 5, 30);
        assertEquals(base, new UtilityReadingRecord("Вода", SEPTEMBER, 5, 30));
        assertEquals(base.hashCode(), new UtilityReadingRecord("Вода", SEPTEMBER, 5, 30).hashCode());
        assertNotEquals(base, new UtilityReadingRecord("вода", SEPTEMBER, 5, 30));
        assertNotEquals(base, new UtilityReadingRecord("Вода", SEPTEMBER.plusDays(1), 5, 30));
        assertNotEquals(base, new UtilityReadingRecord("Вода", SEPTEMBER, 5.000001, 30));
        assertNotEquals(base, new UtilityReadingRecord("Вода", SEPTEMBER, 5, 31));
        assertNotEquals(base, null);
        assertNotEquals(base, "Вода");
        Set<UtilityReadingRecord> set = new HashSet<>(List.of(base, new UtilityReadingRecord("Вода", SEPTEMBER, 5, 30)));
        assertEquals(1, set.size());
    }

    @Test
    void toStringShowsKey()
    {
        assertEquals("Вода (2026-09-01): значення 5.0, тариф 30.0", new UtilityReadingRecord("Вода", SEPTEMBER, 5, 30).toString());
    }

    @Test
    void fromCsvParsesExportOrder() throws DataStorageException
    {
        assertEquals(new UtilityReadingRecord("Вода, гаряча", SEPTEMBER, 3.2, 98.5), UtilityReadingRecord.fromCsv("2026-09-01,\"Вода, гаряча\",98.5,3.2"));
    }

    @Test
    void wrongFieldCountIsStorageError()
    {
        DataStorageException exception = assertThrows(DataStorageException.class, () -> UtilityReadingRecord.fromCsv("2026-09-01,Вода,98.5"));
        assertTrue(exception.getMessage().contains("отримано 3"));
        assertThrows(DataStorageException.class, () -> UtilityReadingRecord.fromCsv("2026-09-01,Вода, гаряча,98.5,3.2"));
    }

    @Test
    void badNumberKeepsNumberFormatCause()
    {
        DataStorageException exception = assertThrows(DataStorageException.class, () -> UtilityReadingRecord.fromCsv("2026-09-01,Вода,98.5,три"));
        assertInstanceOf(NumberFormatException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("значення"));
        DataStorageException tariff = assertThrows(DataStorageException.class, () -> UtilityReadingRecord.fromCsv("2026-09-01,Вода,\"98,5\",3"));
        assertInstanceOf(NumberFormatException.class, tariff.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-30", "01.09.2026", "", "вчора"})
    void badDateKeepsParseCause(String date)
    {
        DataStorageException exception = assertThrows(DataStorageException.class, () -> UtilityReadingRecord.fromFields(List.of(date, "Вода", "1", "1")));
        assertInstanceOf(DateTimeParseException.class, exception.getCause());
    }

    @Test
    void invariantViolationKeepsIllegalArgumentCause()
    {
        DataStorageException negative = assertThrows(DataStorageException.class, () -> UtilityReadingRecord.fromCsv("2026-09-01,Вода,1,-5"));
        assertInstanceOf(IllegalArgumentException.class, negative.getCause());
        DataStorageException blank = assertThrows(DataStorageException.class, () -> UtilityReadingRecord.fromCsv("2026-09-01,,1,5"));
        assertInstanceOf(IllegalArgumentException.class, blank.getCause());
        assertThrows(DataStorageException.class, () -> UtilityReadingRecord.fromCsv("2026-09-01,Вода,NaN,5"));
    }

    @Test
    void roundTripThroughFileRestoresEqualRecordsInOrder() throws DataStorageException, IOException
    {
        List<UtilityReadingRecord> original = sample();
        Path path = directory.resolve("readings.csv");
        CsvExporter.write(path, UtilityReadingRecord.class, original);

        String csv = Files.readString(path, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("дата,лічильник,тариф,значення" + System.lineSeparator()));
        assertTrue(csv.contains("2026-09-01,\"Вода, гаряча\",98.5,3.2"));
        assertTrue(csv.contains("\"Газ \"\"Схід\"\"\""));
        assertTrue(csv.contains("\"Лічильник\nв підвалі\""));

        List<UtilityReadingRecord> restored = UtilityReadingRecord.readCsv(path);
        assertEquals(original, restored);
        // Багаторядкове поле: записів 7 (з заголовком), а фізичних рядків 8.
        assertEquals(7, CsvParser.parseDocument(csv).size());
        assertEquals(8, csv.lines().count());
    }

    @Test
    void roundTripPreservesExactDoubles() throws DataStorageException
    {
        Path path = directory.resolve("doubles.csv");
        List<UtilityReadingRecord> original = List.of(new UtilityReadingRecord("Вода", SEPTEMBER, 0.1 + 0.2, Double.MIN_VALUE), new UtilityReadingRecord("Газ", SEPTEMBER, 1e15, 123456.789));
        CsvExporter.write(path, UtilityReadingRecord.class, original);
        List<UtilityReadingRecord> restored = UtilityReadingRecord.readCsv(path);
        assertEquals(0.1 + 0.2, restored.get(0).getValue());
        assertEquals(original, restored);
    }

    @Test
    void emptyRepositoryRoundTripsToHeaderOnly() throws DataStorageException, IOException
    {
        Path path = directory.resolve("empty.csv");
        CsvExporter.write(path, UtilityReadingRecord.class, new Repository<UtilityReadingRecord>().all());
        assertEquals("дата,лічильник,тариф,значення" + System.lineSeparator(), Files.readString(path, StandardCharsets.UTF_8));
        assertEquals(List.of(), UtilityReadingRecord.readCsv(path));
    }

    @Test
    void controlEmptyCsvHasNoRecords() throws DataStorageException
    {
        assertEquals(List.of(), UtilityReadingRecord.readCsv(Path.of("src", "test", "resources", "lab05", "empty-readings.csv")));
    }

    @Test
    void controlBrokenCsvReportsRecordNumberAndCause()
    {
        DataStorageException exception = assertThrows(DataStorageException.class, () -> UtilityReadingRecord.readCsv(Path.of("src", "test", "resources", "lab05", "broken-readings.csv")));
        assertTrue(exception.getMessage().startsWith("CSV-запис 3:"), exception.getMessage());
        assertInstanceOf(DataStorageException.class, exception.getCause());
        assertInstanceOf(NumberFormatException.class, exception.getCause().getCause());
    }

    @Test
    void zeroByteFileIsStorageError() throws IOException
    {
        Path path = Files.writeString(directory.resolve("zero.csv"), "", StandardCharsets.UTF_8);
        DataStorageException exception = assertThrows(DataStorageException.class, () -> UtilityReadingRecord.readCsv(path));
        assertTrue(exception.getMessage().contains("порожній"));
    }

    @Test
    void wrongHeaderIsStorageError() throws IOException
    {
        Path path = Files.writeString(directory.resolve("header.csv"), "лічильник,дата,значення,тариф\nВода,2026-09-01,5,30\n", StandardCharsets.UTF_8);
        DataStorageException exception = assertThrows(DataStorageException.class, () -> UtilityReadingRecord.readCsv(path));
        assertTrue(exception.getMessage().contains("заголовок"));
    }

    @Test
    void oldSemicolonFormatIsRejected() throws IOException
    {
        Path path = Files.writeString(directory.resolve("old.csv"), "дата,лічильник,тариф,значення\nВода;2026-09-01;5;30\n", StandardCharsets.UTF_8);
        assertThrows(DataStorageException.class, () -> UtilityReadingRecord.readCsv(path));
    }

    @Test
    void unclosedQuoteInFileIsStorageError() throws IOException
    {
        Path path = Files.writeString(directory.resolve("quote.csv"), "дата,лічильник,тариф,значення\n2026-09-01,\"Вода,30,5\n", StandardCharsets.UTF_8);
        assertThrows(DataStorageException.class, () -> UtilityReadingRecord.readCsv(path));
    }

    @Test
    void missingFileKeepsIoCause()
    {
        Path missing = directory.resolve("missing.csv");
        DataStorageException exception = assertThrows(DataStorageException.class, () -> UtilityReadingRecord.readCsv(missing));
        assertInstanceOf(IOException.class, exception.getCause());
        assertTrue(exception.getMessage().contains(missing.toString()));
    }

    @Test
    void invalidUtf8KeepsIoCause() throws IOException
    {
        Path path = Files.write(directory.resolve("cp1251.csv"), "дата,лічильник,тариф,значення\n".getBytes(java.nio.charset.Charset.forName("windows-1251")));
        DataStorageException exception = assertThrows(DataStorageException.class, () -> UtilityReadingRecord.readCsv(path));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void crLfFileFromWindowsIsReadOnAnyOs() throws IOException, DataStorageException
    {
        Path path = Files.writeString(directory.resolve("crlf.csv"), "дата,лічильник,тариф,значення\r\n2026-09-01,\"Вода, гаряча\",98.5,3.2\r\n", StandardCharsets.UTF_8);
        assertEquals(List.of(new UtilityReadingRecord("Вода, гаряча", SEPTEMBER, 3.2, 98.5)), UtilityReadingRecord.readCsv(path));
    }

    @Test
    void lfFileFromUnixIsReadOnAnyOs() throws IOException, DataStorageException
    {
        Path path = Files.writeString(directory.resolve("lf.csv"), "дата,лічильник,тариф,значення\n2026-09-01,Вода,30,5\n", StandardCharsets.UTF_8);
        assertEquals(List.of(new UtilityReadingRecord("Вода", SEPTEMBER, 5, 30)), UtilityReadingRecord.readCsv(path));
    }

    @Test
    void restoredListIsUnmodifiable() throws DataStorageException
    {
        Path path = directory.resolve("r.csv");
        CsvExporter.write(path, UtilityReadingRecord.class, sample());
        List<UtilityReadingRecord> restored = UtilityReadingRecord.readCsv(path);
        assertThrows(UnsupportedOperationException.class, restored::clear);
    }
}
