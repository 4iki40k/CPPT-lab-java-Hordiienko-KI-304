package ua.lpnu.kzp;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Перевіряє нічне споживання, успадковані правила й поліморфну поведінку. */
class ElectricityReadingTest {
    @Test
    void exposesCommonAndSpecificState() {
        ElectricityReading reading = new ElectricityReading(" Електроенергія ", " 2026-09-01 ", 10, 4, 4, 0.5);
        assertEquals("Електроенергія", reading.getMeter());
        assertEquals("2026-09-01", reading.getDate());
        assertEquals(10.0, reading.getValue(), 0.000001);
        assertEquals(4.0, reading.getTariff(), 0.000001);
        assertEquals(4.0, reading.getNightValue(), 0.000001);
        assertEquals(0.5, reading.getNightCoefficient(), 0.000001);
        assertEquals(UtilityKind.ELECTRICITY, reading.getKind());
    }

    @Test
    void defaultConstructorPreservesPreviousFormula() {
        ElectricityReading reading = new ElectricityReading("Електроенергія", "2026-09-01", 10, 4);
        assertEquals(0.0, reading.getNightValue(), 0.000001);
        assertEquals(0.5, reading.getNightCoefficient(), 0.000001);
        assertEquals(40.0, reading.calculateCost(), 0.000001);
    }

    @ParameterizedTest
    @CsvSource({"0, 0.5, 40", "4, 0.5, 32", "10, 0.5, 20", "10, 0, 0", "10, 1, 40"})
    void calculatesDayAndNightCostAtBoundaries(double nightValue, double coefficient, double expected) {
        CostCalculable reading = new ElectricityReading("Електроенергія", "2026-09-01", 10, 4,
                nightValue, coefficient);
        assertEquals(expected, reading.calculateCost(), 0.000001);
    }

    @Test
    void zeroConsumptionHasZeroCost() {
        ElectricityReading reading = new ElectricityReading("Електроенергія", "2026-09-01", 0, 4, 0, 0.5);
        assertEquals(0.0, reading.calculateCost(), 0.000001);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, 11.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidNightConsumption(double nightValue) {
        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityReading("Електроенергія", "2026-09-01", 10, 4, nightValue, 0.5));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidNightCoefficient(double coefficient) {
        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityReading("Електроенергія", "2026-09-01", 10, 4, 4, coefficient));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void inheritsMeterValidation(String meter) {
        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityReading(meter, "2026-09-01", 10, 4, 4, 0.5));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void inheritsDateValidation(String date) {
        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityReading("Електроенергія", date, 10, 4, 4, 0.5));
    }

    @ParameterizedTest
    @CsvSource({"-1, 4", "10, -1"})
    void inheritsNumericValidation(double value, double tariff) {
        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityReading("Електроенергія", "2026-09-01", value, tariff));
    }

    @Test
    void inheritsBaseCostOverflowCheck() {
        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityReading("Електроенергія", "2026-09-01", 1e308, 10));
    }

    @Test
    void inheritedRecordMethodUsesNightTariff() {
        UtilityReading reading = new ElectricityReading("Електроенергія", "2026-09-01", 10, 4, 4, 0.5);
        assertEquals(10.0, reading.consumptionCost().consumption(), 0.000001);
        assertEquals(32.0, reading.consumptionCost().cost(), 0.000001);
    }

    @Test
    void differentSubtypesUseTheirOwnFormulasInOneBaseTypeList() {
        UtilityReading electricity = new ElectricityReading("Електроенергія", "2026-09-01", 10, 4, 4, 0.5);
        UtilityReading water = new WaterReading("Вода", "2026-09-01", 10, 4, 2);
        assertNotEquals(electricity.calculateCost(), water.calculateCost());

        List<UtilityReading> readings = List.of(electricity, water,
                new UtilityReading("Газ", "2026-09-01", 10, 4));
        double totalCost = 0.0;
        for (UtilityReading reading : readings) {
            totalCost += reading.calculateCost();
        }
        assertEquals(132.0, totalCost, 0.000001);
    }
}
