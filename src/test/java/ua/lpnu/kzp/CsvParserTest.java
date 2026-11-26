package ua.lpnu.kzp;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє документний CSV-парсер. */
class CsvParserTest
{
    @Test
    void parsesSimpleRecords() throws DataStorageException
    {
        assertEquals(List.of(List.of("a", "b"), List.of("c", "d")), CsvParser.parseDocument("a,b\nc,d\n"));
    }

    @Test
    void emptyTextHasNoRecords() throws DataStorageException
    {
        assertEquals(List.of(), CsvParser.parseDocument(""));
    }

    @Test
    void nullTextIsStorageError()
    {
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument(null));
    }

    @Test
    void lastRecordWithoutLineBreakIsKept() throws DataStorageException
    {
        assertEquals(List.of(List.of("a"), List.of("b")), CsvParser.parseDocument("a\nb"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a,b\r\nc,d\r\n", "a,b\nc,d\n", "a,b\rc,d\r"})
    void acceptsCrLfLfAndCr(String text) throws DataStorageException
    {
        assertEquals(List.of(List.of("a", "b"), List.of("c", "d")), CsvParser.parseDocument(text));
    }

    @Test
    void commaInsideQuotesIsData() throws DataStorageException
    {
        assertEquals(List.of("2026-09-01", "Вода, гаряча", "98.5"), CsvParser.parse("2026-09-01,\"Вода, гаряча\",98.5"));
    }

    @Test
    void doubledQuoteBecomesOneQuote() throws DataStorageException
    {
        assertEquals(List.of("Газ \"Схід\"", "x"), CsvParser.parse("\"Газ \"\"Схід\"\"\",x"));
    }

    @Test
    void lineBreakInsideQuotesDoesNotEndRecord() throws DataStorageException
    {
        List<List<String>> records = CsvParser.parseDocument("h1,h2\n\"перший\nдругий\",2\r\n\"a\r\nb\",3\n");
        assertEquals(3, records.size());
        assertEquals(List.of("перший\nдругий", "2"), records.get(1));
        assertEquals(List.of("a\r\nb", "3"), records.get(2));
    }

    @Test
    void emptyFieldsArePreserved() throws DataStorageException
    {
        assertEquals(List.of("", "", ""), CsvParser.parse(",,"));
        assertEquals(List.of("", "x"), CsvParser.parse("\"\",x"));
        assertEquals(List.of(List.of("")), CsvParser.parseDocument("\"\""));
    }

    @Test
    void blankLineIsOneEmptyField() throws DataStorageException
    {
        assertEquals(List.of(List.of("a"), List.of(""), List.of("b")), CsvParser.parseDocument("a\n\nb\n"));
    }

    @Test
    void parseRejectsSeveralRecords()
    {
        DataStorageException exception = assertThrows(DataStorageException.class, () -> CsvParser.parse("a\nb"));
        assertTrue(exception.getMessage().contains("2"));
    }

    @Test
    void parseRejectsEmptyText()
    {
        assertThrows(DataStorageException.class, () -> CsvParser.parse(""));
    }

    @Test
    void unclosedQuoteIsStorageError()
    {
        DataStorageException exception = assertThrows(DataStorageException.class, () -> CsvParser.parseDocument("a,b\n\"Вода, гаряча,3\n"));
        assertTrue(exception.getMessage().contains("незакриті лапки"));
        assertTrue(exception.getMessage().contains("CSV-запис 2"));
    }

    @Test
    void symbolAfterClosingQuoteIsStorageError()
    {
        assertThrows(DataStorageException.class, () -> CsvParser.parse("\"Вода\"x,1"));
    }

    @Test
    void bareQuoteInsideUnquotedFieldIsStorageError()
    {
        assertThrows(DataStorageException.class, () -> CsvParser.parse("Газ \"Схід\",1"));
    }

    @Test
    void resultIsUnmodifiable() throws DataStorageException
    {
        List<List<String>> records = CsvParser.parseDocument("a,b\n");
        assertThrows(UnsupportedOperationException.class, () -> records.add(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> records.get(0).add("c"));
    }

    @Test
    void escapedValuesRoundTripThroughExporterEscape() throws DataStorageException
    {
        List<String> values = List.of("просто", "з, комою", "з \"лапками\"", "рядок\nдругий", "", " пробіли ");
        String line = String.join(",", values.stream().map(CsvExporter::escape).toList());
        assertEquals(values, CsvParser.parse(line));
    }
}
