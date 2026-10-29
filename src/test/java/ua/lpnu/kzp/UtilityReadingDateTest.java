package ua.lpnu.kzp;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє календарну дату та сумісне текстове представлення. */
class UtilityReadingDateTest {
    private static final String DATE_ERROR =
            "Помилка: дата має бути коректною календарною датою у форматі YYYY-MM-DD";

    @Test
    void storesCalendarDateAndPreservesTextGetter() {
        UtilityReading reading = new UtilityReading("Газ", " 2026-09-01 ", 10, 4);
        assertEquals(LocalDate.of(2026, 9, 1), reading.getLocalDate());
        assertEquals("2026-09-01", reading.getDate());
        assertEquals("Газ (2026-09-01): споживання 10.00, тариф 4.00", reading.toString());
    }

    @Test
    void acceptsRealLeapDayThroughFactory() {
        UtilityReading reading = UtilityReading.fromCsv("Вода;2024-02-29;10;4");
        assertEquals(LocalDate.of(2024, 2, 29), reading.getLocalDate());
        assertEquals("2024-02-29", reading.getDate());
    }

    @Test
    void dateArithmeticDoesNotMutateReading() {
        UtilityReading reading = new UtilityReading("Газ", "2026-09-01", 10, 4);
        LocalDate nextDay = reading.getLocalDate().plusDays(1);
        assertEquals(LocalDate.of(2026, 9, 2), nextDay);
        assertEquals(LocalDate.of(2026, 9, 1), reading.getLocalDate());
    }

    static Stream<String> invalidDates() {
        return Stream.of("2026-02-29", "2026-02-30", "2026-13-01", "2026-00-10",
                "2026-04-31", "2026-09-00", "01.09.2026", "2026-9-1",
                "2026-09-01T00:00:00", "завтра");
    }

    @ParameterizedTest
    @MethodSource("invalidDates")
    void constructorRejectsInvalidDateAndPreservesCause(String date) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new UtilityReading("Газ", date, 10, 4));
        assertEquals(DATE_ERROR, error.getMessage());
        assertInstanceOf(DateTimeParseException.class, error.getCause());
    }

    @ParameterizedTest
    @MethodSource("invalidDates")
    void factoryReportsCalendarErrorsAsRecordValidation(String date) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> UtilityReading.fromCsv("Вода;" + date + ";10;4"));
        assertEquals(DATE_ERROR, error.getMessage());
        assertInstanceOf(DateTimeParseException.class, error.getCause());
    }

    @Test
    void subtypesInheritCalendarValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new WaterReading("Вода", "2026-02-30", 10, 4, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityReading("Електроенергія", "2026-02-30", 10, 4, 4, 0.5));
    }

    @Test
    void invalidDateDoesNotStopLaterRowsOrChangeIndicators() {
        String report = Main.buildReport(List.of(
                "Вода;2026-09-01;10;30",
                "Електроенергія;2026-02-30;100;4",
                "Газ;2026-09-01;5;8"));
        assertTrue(report.contains("Коректних записів: 2"));
        assertTrue(report.contains("Сумарне споживання: 15.00"));
        assertTrue(report.contains("Загальна вартість: 340.00"));
        assertTrue(report.contains("Найбільше споживання: 10.00"));
        assertTrue(report.contains("Помилок: 1"));
        assertTrue(report.contains("Рядок 2: " + DATE_ERROR));
    }

    @Test
    void equalityAndHashCodeUseCalendarValue() {
        UtilityReading first = new UtilityReading("Газ", " 2026-09-01 ", 10, 4);
        UtilityReading second = new UtilityReading("Газ", "2026-09-01", 10, 4);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
