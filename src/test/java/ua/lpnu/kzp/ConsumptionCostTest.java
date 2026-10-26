package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Перевіряє компоненти, рівність і допустимість значень record. */
class ConsumptionCostTest {
    @Test
    void exposesComponents() {
        ConsumptionCost amounts = new ConsumptionCost(10.5, 42.0);
        assertEquals(10.5, amounts.consumption(), 0.000001);
        assertEquals(42.0, amounts.cost(), 0.000001);
    }

    @Test
    void usesValueEqualityAndMatchingHashCodes() {
        ConsumptionCost first = new ConsumptionCost(10, 30);
        ConsumptionCost second = new ConsumptionCost(10, 30);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new ConsumptionCost(10, 31));
        assertNotEquals(first, new ConsumptionCost(11, 30));
    }

    @Test
    void allowsZeroComponents() {
        ConsumptionCost amounts = new ConsumptionCost(0, 0);
        assertEquals(0.0, amounts.consumption(), 0.000001);
        assertEquals(0.0, amounts.cost(), 0.000001);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidConsumption(double consumption) {
        assertThrows(IllegalArgumentException.class, () -> new ConsumptionCost(consumption, 30));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidCost(double cost) {
        assertThrows(IllegalArgumentException.class, () -> new ConsumptionCost(10, cost));
    }
}
