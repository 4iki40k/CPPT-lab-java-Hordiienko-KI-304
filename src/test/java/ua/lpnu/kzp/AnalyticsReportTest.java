package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє текст аналітики незалежно від файлового вводу та CLI. */
class AnalyticsReportTest
{
    private static List<UtilityReading> sampleReadings()
    {
        return List.of(UtilityReading.fromCsv("Електроенергія;2026-09-03;100;4"), UtilityReading.fromCsv("Вода;2026-09-02;60;30"), UtilityReading.fromCsv("Електроенергія;2026-09-01;60;4"), UtilityReading.fromCsv("Газ;2026-09-04;50;8"), UtilityReading.fromCsv("Вода;2026-09-05;30;30"), UtilityReading.fromCsv("Газ;2026-09-06;10;8"));
    }

    @Test
    void formatsAllQueriesAndBothSearchResults()
    {
        String expected = """
                Поріг споживання: 50.00
                Понад поріг: Електроенергія (2026-09-03): 100.00, Вода (2026-09-02): 60.00, Електроенергія (2026-09-01): 60.00
                Споживання: [100.00, 60.00, 60.00, 50.00, 30.00, 10.00]
                Суми за лічильниками: {Вода=90.00, Газ=60.00, Електроенергія=160.00}
                Кількість: 6
                Сума: 310.00
                Середнє: 51.67
                Мінімум: 10.00
                Максимум: 100.00
                Топ-5: Електроенергія (2026-09-03): 100.00, Електроенергія (2026-09-01): 60.00, Вода (2026-09-02): 60.00, Газ (2026-09-04): 50.00, Вода (2026-09-05): 30.00
                Пошук Вода: Вода (2026-09-02): 60.00
                Пошук Тепло: Не знайдено
                """;
        assertEquals(expected.replace("\n", System.lineSeparator()), AnalyticsReport.build(sampleReadings()));
    }

    @Test
    void formatsEmptyInputWithoutInfinityOrNaN()
    {
        String expected = """
                Поріг споживання: 50.00
                Понад поріг: немає даних
                Споживання: []
                Суми за лічильниками: {}
                Кількість: 0
                Сума: 0.00
                Середнє: 0.00
                Мінімум: немає даних
                Максимум: немає даних
                Топ-5: немає даних
                Пошук Вода: Не знайдено
                Пошук Тепло: Не знайдено
                """;
        String actual = AnalyticsReport.build(List.of());
        assertEquals(expected.replace("\n", System.lineSeparator()), actual);
        assertFalse(actual.contains("Infinity"));
        assertFalse(actual.contains("NaN"));
    }

    @Test
    void formatsUsingDecimalPointRegardlessOfDefaultLocale()
    {
        Locale original = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.GERMANY);
            String report = AnalyticsReport.build(sampleReadings());
            assertTrue(report.contains("Сума: 310.00"));
            assertTrue(report.contains("Середнє: 51.67"));
            assertTrue(report.contains("Вода=90.00"));
        }
        finally
        {
            Locale.setDefault(original);
        }
    }

    @Test
    void preservesSourceAndProducesDeterministicText()
    {
        List<UtilityReading> source = new ArrayList<>(sampleReadings());
        List<UtilityReading> before = List.copyOf(source);
        String first = AnalyticsReport.build(source);
        String second = AnalyticsReport.build(source);
        assertEquals(first, second);
        assertEquals(before, source);
    }

    @Test
    void rejectsNullSource()
    {
        assertThrows(NullPointerException.class, () -> AnalyticsReport.build(null));
    }
}
