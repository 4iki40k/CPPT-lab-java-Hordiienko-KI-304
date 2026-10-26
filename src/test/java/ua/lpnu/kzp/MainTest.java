package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Перевіряє структуру рядка, текстові поля та перетворення чисел. */
class MainTest {
    @Test
    void acceptsFourFieldsWithUkrainianText() {
        assertEquals("", Main.validateLine("Гаряча вода;2026-09-01;10;30"));
    }

    @Test
    void acceptsSpacesAroundNonemptyTextFields() {
        assertEquals("", Main.validateLine("  Вода  ; 2026-09-01 ;10;30"));
    }

    @Test
    void rejectsThreeFields() {
        assertEquals("Помилка: потрібно рівно 4 поля",
                Main.validateLine("Вода;2026-09-01;10"));
    }

    @Test
    void rejectsFiveFields() {
        assertEquals("Помилка: потрібно рівно 4 поля",
                Main.validateLine("Вода;2026-09-01;10;30;зайве"));
    }

    @Test
    void rejectsEmptyMeter() {
        assertEquals("Помилка: назва лічильника або дата порожня",
                Main.validateLine(";2026-09-01;10;30"));
    }

    @Test
    void rejectsWhitespaceOnlyMeter() {
        assertEquals("Помилка: назва лічильника або дата порожня",
                Main.validateLine("   ;2026-09-01;10;30"));
    }

    @Test
    void rejectsEmptyDate() {
        assertEquals("Помилка: назва лічильника або дата порожня",
                Main.validateLine("Вода;;10;30"));
    }

    @Test
    void rejectsWhitespaceOnlyDate() {
        assertEquals("Помилка: назва лічильника або дата порожня",
                Main.validateLine("Вода;   ;10;30"));
    }

    @Test
    void rejectsEmptyLine() {
        assertEquals("Помилка: потрібно рівно 4 поля", Main.validateLine(""));
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
    void rejectsInvalidNumericFormat(String line) {
        assertEquals("Помилка: споживання або тариф має неправильний числовий формат",
                Main.validateLine(line));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Вода;2026-09-01;0;0",
            "Вода;2026-09-01;10.5;4.32",
            "Вода;2026-09-01; 10.5 ; 4.32 "
    })
    void acceptsValidNumericFormat(String line) {
        assertEquals("", Main.validateLine(line));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Вода;2026-09-01;-5;30",
            "Вода;2026-09-01;10;-30",
            "Вода;2026-09-01;-5;-30"
    })
    void rejectsNegativeNumbers(String line) {
        assertEquals("Помилка: споживання та тариф не можуть бути від’ємними",
                Main.validateLine(line));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Вода;2026-09-01;NaN;30",
            "Вода;2026-09-01;10;NaN",
            "Вода;2026-09-01;Infinity;30",
            "Вода;2026-09-01;10;Infinity",
            "Вода;2026-09-01;-Infinity;30",
            "Вода;2026-09-01;10;-Infinity",
            "Вода;2026-09-01;1e309;30",
            "Вода;2026-09-01;10;1e309"
    })
    void rejectsNonFiniteNumbers(String line) {
        assertEquals("Помилка: споживання та тариф мають бути скінченними числами",
                Main.validateLine(line));
    }
}
