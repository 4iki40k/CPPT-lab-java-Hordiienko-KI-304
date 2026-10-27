package ua.lpnu.kzp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє контракт рівності ієрархії та поведінку хеш-колекцій. */
class ReadingEqualityTest {
    static Stream<Arguments> equalReadings() {
        return Stream.of(
                Arguments.of("звичайний",
                        new UtilityReading("Газ", "2026-09-01", 10, 4),
                        new UtilityReading("Газ", "2026-09-01", 10, 4),
                        new UtilityReading("Газ", "2026-09-01", 10, 4)),
                Arguments.of("вода",
                        new WaterReading("Вода", "2026-09-01", 10, 4, 2),
                        new WaterReading("Вода", "2026-09-01", 10, 4, 2),
                        new WaterReading("Вода", "2026-09-01", 10, 4, 2)),
                Arguments.of("електроенергія",
                        new ElectricityReading("Електроенергія", "2026-09-01", 10, 4, 4, 0.5),
                        new ElectricityReading("Електроенергія", "2026-09-01", 10, 4, 4, 0.5),
                        new ElectricityReading("Електроенергія", "2026-09-01", 10, 4, 4, 0.5)));
    }

    @ParameterizedTest(name = "Контракт equals: {0}")
    @MethodSource("equalReadings")
    void equalityIsReflexiveSymmetricTransitiveAndConsistent(String kind,
            UtilityReading first, UtilityReading second, UtilityReading third) {
        assertNotSame(first, second);
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(second, third);
        assertEquals(first, third);
        assertEquals(first, second);
        assertNotEquals(first, null);
        assertNotEquals(first, "інший тип");
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(second.hashCode(), third.hashCode());
    }

    @ParameterizedTest(name = "HashSet: {0}")
    @MethodSource("equalReadings")
    void hashSetDeduplicatesEqualInstances(String kind,
            UtilityReading first, UtilityReading second, UtilityReading third) {
        Set<UtilityReading> readings = new HashSet<>();
        readings.add(first);
        readings.add(second);
        readings.add(third);
        assertEquals(1, readings.size());
        assertTrue(readings.contains(second));
    }

    @ParameterizedTest(name = "HashMap: {0}")
    @MethodSource("equalReadings")
    void hashMapFindsValueWithAnotherEqualKey(String kind,
            UtilityReading first, UtilityReading second, UtilityReading third) {
        Map<UtilityReading, String> readings = new HashMap<>();
        readings.put(first, "перший");
        assertEquals("перший", readings.get(second));
        readings.put(third, "оновлений");
        assertEquals(1, readings.size());
        assertEquals("оновлений", readings.get(first));
    }

    static Stream<Arguments> differentReadings() {
        UtilityReading ordinary = new UtilityReading("Лічильник", "2026-09-01", 10, 4);
        WaterReading water = new WaterReading("Лічильник", "2026-09-01", 10, 4);
        ElectricityReading electricity = new ElectricityReading("Лічильник", "2026-09-01", 10, 4);
        return Stream.of(
                Arguments.of(ordinary, new UtilityReading("Інший", "2026-09-01", 10, 4)),
                Arguments.of(ordinary, new UtilityReading("Лічильник", "2026-10-01", 10, 4)),
                Arguments.of(ordinary, new UtilityReading("Лічильник", "2026-09-01", 11, 4)),
                Arguments.of(ordinary, new UtilityReading("Лічильник", "2026-09-01", 10, 5)),
                Arguments.of(ordinary, water),
                Arguments.of(ordinary, electricity),
                Arguments.of(water, electricity),
                Arguments.of(water, new WaterReading("Лічильник", "2026-09-01", 10, 4, 2)),
                Arguments.of(electricity, new ElectricityReading("Лічильник", "2026-09-01", 10, 4, 4, 0.5)),
                Arguments.of(electricity, new ElectricityReading("Лічильник", "2026-09-01", 10, 4, 0, 0.7)));
    }

    @ParameterizedTest
    @MethodSource("differentReadings")
    void distinguishesDifferentStateInBothDirectionsAndInHashSet(UtilityReading first, UtilityReading second) {
        assertNotEquals(first, second);
        assertNotEquals(second, first);
        assertEquals(2, new HashSet<>(List.of(first, second)).size());
    }

    @Test
    void comparesNormalizedStoredText() {
        UtilityReading first = new UtilityReading(" Газ ", " 2026-09-01 ", 10, 4);
        UtilityReading second = new UtilityReading("Газ", "2026-09-01", 10, 4);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equalityDoesNotRemoveRepeatedRowsFromReport() {
        String line = "Вода;2026-09-01;10;4";
        String report = Main.buildReport(List.of(line, line));
        assertTrue(report.contains("Коректних записів: 2"));
        assertTrue(report.contains("Сумарне споживання: 20.00"));
        assertTrue(report.contains("Загальна вартість: 80.00"));
        assertTrue(report.contains("Помилок: 0"));
    }
}
