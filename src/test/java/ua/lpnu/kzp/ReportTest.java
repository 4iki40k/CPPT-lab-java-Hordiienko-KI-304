package ua.lpnu.kzp;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє показники та діагностику незалежно від файлів і CLI. */
class ReportTest {
    @Test
    void calculatesAllFourIndicators() {
        String report = Main.buildReport(List.of(
                "Електроенергія;2026-09-01;100;4",
                "Вода;2026-09-01;10;30",
                "Газ;2026-09-01;5;8"));
        assertEquals(String.format(Locale.ROOT,
                "Коректних записів: 3%nСумарне споживання: 115.00%nЗагальна вартість: 740.00%n"
                        + "Найбільше споживання: 100.00%nПомилок: 0%n"), report);
    }

    @Test
    void skipsInvalidRowsWithoutChangingIndicatorsAndKeepsLineNumbers() {
        String report = Main.buildReport(List.of(
                "Вода;2026-09-01;10;30",
                "Газ;2026-09-01;помилка;8",
                "Газ;2026-09-01;5;8",
                "Вода;2026-09-01;100;-1"));
        assertTrue(report.contains("Коректних записів: 2"));
        assertTrue(report.contains("Сумарне споживання: 15.00"));
        assertTrue(report.contains("Загальна вартість: 340.00"));
        assertTrue(report.contains("Найбільше споживання: 10.00"));
        assertTrue(report.contains("Помилок: 2"));
        assertTrue(report.contains("Рядок 2: Помилка: споживання або тариф має неправильний числовий формат"));
        assertTrue(report.contains("Рядок 4: Помилка: споживання та тариф не можуть бути від’ємними"));
    }

    @Test
    void handlesEmptyInput() {
        String report = Main.buildReport(List.of());
        assertEquals(String.format(Locale.ROOT,
                "Коректних записів: 0%nСумарне споживання: 0.00%nЗагальна вартість: 0.00%n"
                        + "Найбільше споживання: немає даних%nПомилок: 0%n"), report);
    }

    @Test
    void handlesInputWithNoValidRows() {
        String report = Main.buildReport(List.of("", "Вода;;10;30"));
        assertTrue(report.contains("Коректних записів: 0"));
        assertTrue(report.contains("Найбільше споживання: немає даних"));
        assertTrue(report.contains("Помилок: 2"));
        assertTrue(report.contains("Рядок 1:"));
        assertTrue(report.contains("Рядок 2:"));
    }

    @Test
    void distinguishesZeroConsumptionFromNoData() {
        String report = Main.buildReport(List.of("Вода;2026-09-01;0;0"));
        assertTrue(report.contains("Коректних записів: 1"));
        assertTrue(report.contains("Загальна вартість: 0.00"));
        assertTrue(report.contains("Найбільше споживання: 0.00"));
        assertFalse(report.contains("немає даних"));
    }

    @Test
    void formatsDecimalsIndependentlyOfDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            String report = Main.buildReport(List.of("Вода;2026-09-01;1.5;2.2"));
            assertTrue(report.contains("Сумарне споживання: 1.50"));
            assertTrue(report.contains("Загальна вартість: 3.30"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void rejectsOverflowingRecordCost() {
        assertEquals("Помилка: вартість запису перевищує допустимий діапазон",
                Main.validateLine("Вода;2026-09-01;1e308;10"));
    }

    @Test
    void rejectsOverflowingConsumptionSumWithoutPartialUpdate() {
        String report = Main.buildReport(List.of(
                "Вода;2026-09-01;1e308;0", "Вода;2026-09-01;1e308;1"));
        assertTrue(report.contains("Коректних записів: 1"));
        assertTrue(report.contains("Загальна вартість: 0.00"));
        assertTrue(report.contains("Рядок 2: Помилка: сума перевищує допустимий діапазон"));
        assertFalse(report.contains("Infinity"));
    }

    @Test
    void rejectsOverflowingCostSumWithoutPartialUpdate() {
        String report = Main.buildReport(List.of(
                "Вода;2026-09-01;1;1e308", "Вода;2026-09-01;1;1e308"));
        assertTrue(report.contains("Коректних записів: 1"));
        assertTrue(report.contains("Сумарне споживання: 1.00"));
        assertTrue(report.contains("Рядок 2: Помилка: сума перевищує допустимий діапазон"));
        assertFalse(report.contains("Infinity"));
    }
}
