package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє спільне зарахування CSV-рядків і діагностику обох звітів. */
class AnalyticsProcessingTest
{
    @Test
    void analyticsDatasetHasSixAcceptedRowsAndTwoExplainedErrors() throws IOException
    {
        List<String> lines = Files.readAllLines(Path.of("data", "analytics.csv"), StandardCharsets.UTF_8);
        assertEquals(8, lines.size());
        String report = Main.buildAnalyticsReport(lines);
        assertTrue(report.contains("Кількість: 6"));
        assertTrue(report.contains("Сума: 310.00"));
        assertTrue(report.contains("Середнє: 51.67"));
        assertTrue(report.contains("Суми за лічильниками: {Вода=90.00, Газ=60.00, Електроенергія=160.00}"));
        assertTrue(report.contains("Помилок: 2"));
        assertTrue(report.contains("Рядок 7: Помилка: дата має бути коректною календарною датою у форматі YYYY-MM-DD"));
        assertTrue(report.contains("Рядок 8: Помилка: споживання та тариф не можуть бути від’ємними"));
    }

    @Test
    void ordinaryReportUsesSameAcceptedRowsAndPreservesCost() throws IOException
    {
        List<String> lines = Files.readAllLines(Path.of("data", "analytics.csv"), StandardCharsets.UTF_8);
        String ordinary = Main.buildReport(lines);
        String analytics = Main.buildAnalyticsReport(lines);
        assertTrue(ordinary.contains("Коректних записів: 6"));
        assertTrue(ordinary.contains("Сумарне споживання: 310.00"));
        assertTrue(ordinary.contains("Загальна вартість: 3820.00"));
        assertTrue(ordinary.contains("Найбільше споживання: 100.00"));
        assertEquals(ordinary.substring(ordinary.indexOf("Помилок: ")), analytics.substring(analytics.indexOf("Помилок: ")));
    }

    @Test
    void originalInputStillProducesOriginalControlReport() throws IOException
    {
        List<String> lines = Files.readAllLines(Path.of("data", "input.csv"), StandardCharsets.UTF_8);
        String expected = Files.readString(Path.of("src", "test", "resources", "lab01", "report.txt"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertEquals(expected, Main.buildReport(lines).replace("\r\n", "\n"));
    }

    @Test
    void emptyInputProducesEmptyAnalyticsAndNoErrors()
    {
        String report = Main.buildAnalyticsReport(List.of());
        assertTrue(report.contains("Кількість: 0"));
        assertTrue(report.contains("Сума: 0.00"));
        assertTrue(report.contains("Мінімум: немає даних"));
        assertTrue(report.endsWith("Помилок: 0" + System.lineSeparator()));
        assertFalse(report.contains("Infinity"));
        assertFalse(report.contains("NaN"));
    }

    @Test
    void allInvalidRowsAreExplainedWithoutBecomingAnalyticsRecords()
    {
        String report = Main.buildAnalyticsReport(List.of("", "Вода;2026-02-30;10;4", "Газ;2026-09-01;-5;8"));
        assertTrue(report.contains("Кількість: 0"));
        assertTrue(report.contains("Помилок: 3"));
        assertTrue(report.contains("Рядок 1:"));
        assertTrue(report.contains("Рядок 2:"));
        assertTrue(report.contains("Рядок 3:"));
        assertFalse(report.contains("Infinity"));
    }

    @Test
    void overflowingRowDoesNotPartiallyChangeTotalsOrStopLaterRows()
    {
        List<String> lines = List.of("Газ;2026-09-01;1e308;0", "Газ;2026-09-02;1e308;1", "Газ;2026-09-03;5;2");
        String ordinary = Main.buildReport(lines);
        String analytics = Main.buildAnalyticsReport(lines);
        assertTrue(ordinary.contains("Коректних записів: 2"));
        assertTrue(ordinary.contains("Загальна вартість: 10.00"));
        assertTrue(analytics.contains("Кількість: 2"));
        assertTrue(analytics.contains("Мінімум: 5.00"));
        assertTrue(analytics.contains("Рядок 2: Помилка: сума перевищує допустимий діапазон"));
        assertFalse(analytics.contains("Infinity"));
        assertEquals(ordinary.substring(ordinary.indexOf("Помилок: ")), analytics.substring(analytics.indexOf("Помилок: ")));
    }

    @Test
    void reportGenerationDoesNotMutateInputLines()
    {
        List<String> lines = new ArrayList<>(List.of("Вода;2026-09-01;10;4", "неправильний рядок"));
        List<String> before = List.copyOf(lines);
        Main.buildReport(lines);
        Main.buildAnalyticsReport(lines);
        assertEquals(before, lines);
    }
}
