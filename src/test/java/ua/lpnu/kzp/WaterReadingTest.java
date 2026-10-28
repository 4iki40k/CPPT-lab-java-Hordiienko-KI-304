package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Перевіряє тариф водовідведення, успадковані правила та поліморфну вартість. */
class WaterReadingTest {
    @Test
    void preservesCommonFieldsAndExposesWaterCategory() {
        WaterReading reading = new WaterReading(" Вода ", " 2026-09-01 ", 10, 4, 2);
        assertEquals("Вода", reading.getMeter());
        assertEquals("2026-09-01", reading.getDate());
        assertEquals(10.0, reading.getValue(), 0.000001);
        assertEquals(4.0, reading.getTariff(), 0.000001);
        assertEquals(2.0, reading.getWastewaterTariff(), 0.000001);
        assertEquals(UtilityKind.WATER, reading.getKind());
        assertEquals(60.0, reading.calculateCost(), 0.000001);
    }

    @Test
    void defaultWastewaterTariffPreservesPreviousFormula() {
        WaterReading reading = new WaterReading("Вода", "2026-09-01", 10, 4);
        assertEquals(0.0, reading.getWastewaterTariff(), 0.000001);
        assertEquals(40.0, reading.calculateCost(), 0.000001);
    }

    @Test
    void zeroConsumptionHasZeroCost() {
        WaterReading reading = new WaterReading("Вода", "2026-09-01", 0, 4, 2);
        assertEquals(0.0, reading.calculateCost(), 0.000001);
    }

    @Test
    void calculatesWaterCostThroughInterface() {
        CostCalculable reading = new WaterReading("Вода", "2026-09-01", 10, 4, 2);
        assertEquals(60.0, reading.calculateCost(), 0.000001);
    }

    @Test
    void inheritedRecordMethodUsesSubtypeCost() {
        UtilityReading reading = new WaterReading("Вода", "2026-09-01", 10, 4, 2);
        assertEquals(10.0, reading.consumptionCost().consumption(), 0.000001);
        assertEquals(60.0, reading.consumptionCost().cost(), 0.000001);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidWastewaterTariff(double tariff) {
        assertThrows(IllegalArgumentException.class,
                () -> new WaterReading("Вода", "2026-09-01", 10, 4, tariff));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void inheritsMeterValidation(String meter) {
        assertThrows(IllegalArgumentException.class,
                () -> new WaterReading(meter, "2026-09-01", 10, 4, 2));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void inheritsDateValidation(String date) {
        assertThrows(IllegalArgumentException.class,
                () -> new WaterReading("Вода", date, 10, 4, 2));
    }

    @ParameterizedTest
    @CsvSource({"-1, 4", "10, -1"})
    void inheritsNumericValidation(double value, double tariff) {
        assertThrows(IllegalArgumentException.class,
                () -> new WaterReading("Вода", "2026-09-01", value, tariff, 2));
    }

    @Test
    void rejectsOverflowingWastewaterCost() {
        assertThrows(IllegalArgumentException.class,
                () -> new WaterReading("Вода", "2026-09-01", 1e308, 0, 2));
    }

    @Test
    void rejectsOverflowingCombinedCostEvenWhenBothProductsAreFinite() {
        assertThrows(IllegalArgumentException.class,
                () -> new WaterReading("Вода", "2026-09-01", 1e308, 1, 1));
    }

    @Test
    void inheritsBaseCostOverflowCheck() {
        assertThrows(IllegalArgumentException.class,
                () -> new WaterReading("Вода", "2026-09-01", 1e308, 10, 0));
    }
}
