package ua.lpnu.kzp;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Перевіряє конструктор, CSV-фабрику, методи читання та опис запису. */
class UtilityReadingTest {
    @Test
    void constructorStoresAllFields() {
        UtilityReading reading = new UtilityReading("Вода", "2026-09-01", 10.5, 30.25);
        assertEquals("Вода", reading.getMeter());
        assertEquals("2026-09-01", reading.getDate());
        assertEquals(10.5, reading.getValue(), 0.000001);
        assertEquals(30.25, reading.getTariff(), 0.000001);
    }

    @Test
    void constructorStripsSurroundingSpaces() {
        UtilityReading reading = new UtilityReading("  Гаряча вода  ", " 2026-09-01 ", 10, 30);
        assertEquals("Гаряча вода", reading.getMeter());
        assertEquals("2026-09-01", reading.getDate());
    }

    @Test
    void constructorAllowsZeroValues() {
        UtilityReading reading = new UtilityReading("Вода", "2026-09-01", 0, 0);
        assertEquals(0.0, reading.getValue(), 0.000001);
        assertEquals(0.0, reading.getTariff(), 0.000001);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void constructorRejectsMissingMeter(String meter) {
        assertThrows(IllegalArgumentException.class,
                () -> new UtilityReading(meter, "2026-09-01", 10, 30));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void constructorRejectsMissingDate(String date) {
        assertThrows(IllegalArgumentException.class,
                () -> new UtilityReading("Вода", date, 10, 30));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void constructorRejectsInvalidConsumption(double value) {
        assertThrows(IllegalArgumentException.class,
                () -> new UtilityReading("Вода", "2026-09-01", value, 30));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void constructorRejectsInvalidTariff(double tariff) {
        assertThrows(IllegalArgumentException.class,
                () -> new UtilityReading("Вода", "2026-09-01", 10, tariff));
    }

    @Test
    void constructorRejectsOverflowingCost() {
        assertThrows(IllegalArgumentException.class,
                () -> new UtilityReading("Вода", "2026-09-01", 1e308, 10));
    }

    @Test
    void toStringDescribesReadingWithTwoDecimalPlaces() {
        UtilityReading reading = new UtilityReading("Вода", "2026-09-01", 10, 30);
        assertEquals("Вода (2026-09-01): споживання 10.00, тариф 30.00", reading.toString());
    }

    @Test
    void toStringUsesDecimalPointRegardlessOfDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            UtilityReading reading = new UtilityReading("Вода", "2026-09-01", 1.5, 4.32);
            assertEquals("Вода (2026-09-01): споживання 1.50, тариф 4.32", reading.toString());
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void fromCsvCreatesReadingAndStripsSpaces() {
        UtilityReading reading = UtilityReading.fromCsv(" Гаряча вода ; 2026-09-01 ; 10.5 ; 4.32 ");
        assertEquals("Гаряча вода", reading.getMeter());
        assertEquals("2026-09-01", reading.getDate());
        assertEquals(10.5, reading.getValue(), 0.000001);
        assertEquals(4.32, reading.getTariff(), 0.000001);
    }

    @Test
    void fromCsvAllowsZeroValues() {
        UtilityReading reading = UtilityReading.fromCsv("Вода;2026-09-01;0;0");
        assertEquals(0.0, reading.getValue(), 0.000001);
        assertEquals(0.0, reading.getTariff(), 0.000001);
    }

    @Test
    void fromCsvRejectsNull() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> UtilityReading.fromCsv(null));
        assertEquals("Помилка: CSV-рядок не може бути null", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Вода;2026-09-01;10", "Вода;2026-09-01;10;30;зайве"})
    void fromCsvRejectsWrongFieldCount(String line) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> UtilityReading.fromCsv(line));
        assertEquals("Помилка: потрібно рівно 4 поля", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Вода;2026-09-01;помилка;30",
            "Вода;2026-09-01;10;помилка",
            "Вода;2026-09-01;;30",
            "Вода;2026-09-01;10;",
            "Вода;2026-09-01;   ;30",
            "Вода;2026-09-01;10;   ",
            "Вода;2026-09-01;10,5;30",
            "Вода;2026-09-01;10;4,32"
    })
    void fromCsvExplainsInvalidNumberAndPreservesCause(String line) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> UtilityReading.fromCsv(line));
        assertEquals("Помилка: споживання або тариф має неправильний числовий формат", error.getMessage());
        assertInstanceOf(NumberFormatException.class, error.getCause());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            ";2026-09-01;10;30|Помилка: назва лічильника або дата порожня",
            "Вода;;10;30|Помилка: назва лічильника або дата порожня",
            "Вода;2026-09-01;-5;30|Помилка: споживання та тариф не можуть бути від’ємними",
            "Вода;2026-09-01;10;-30|Помилка: споживання та тариф не можуть бути від’ємними",
            "Вода;2026-09-01;NaN;30|Помилка: споживання та тариф мають бути скінченними числами",
            "Вода;2026-09-01;10;Infinity|Помилка: споживання та тариф мають бути скінченними числами",
            "Вода;2026-09-01;1e308;10|Помилка: вартість запису перевищує допустимий діапазон"
    })
    void fromCsvPropagatesConstructorValidation(String line, String message) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> UtilityReading.fromCsv(line));
        assertEquals(message, error.getMessage());
    }

    @Test
    void fromCsvChecksTextBeforeNumericFormatLikeLab01() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> UtilityReading.fromCsv(";2026-09-01;помилка;30"));
        assertEquals("Помилка: назва лічильника або дата порожня", error.getMessage());
    }

    @Test
    void consumptionCostReturnsRecordWithCalculatedCost() {
        UtilityReading reading = new UtilityReading("Вода", "2026-09-01", 10.5, 4.32);
        ConsumptionCost amounts = reading.consumptionCost();
        assertEquals(10.5, amounts.consumption(), 0.000001);
        assertEquals(45.36, amounts.cost(), 0.000001);
    }
}
