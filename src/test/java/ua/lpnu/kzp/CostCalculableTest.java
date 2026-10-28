package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Перевіряє спільний контракт і делегування вартості фактичному об'єкту. */
class CostCalculableTest {
    @Test
    void ordinaryReadingImplementsCostContract() {
        CostCalculable reading = new UtilityReading("Вода", "2026-09-01", 10.5, 4.32);
        assertEquals(45.36, reading.calculateCost(), 0.000001);
    }

    @Test
    void ordinaryReadingHasOtherCategory() {
        UtilityReading reading = new UtilityReading("Газ", "2026-09-01", 10, 4);
        assertEquals(UtilityKind.OTHER, reading.getKind());
    }

    @Test
    void consumptionCostUsesOverriddenCalculation() {
        UtilityReading reading = new FixedCostReading();
        assertEquals(10.0, reading.consumptionCost().consumption(), 0.000001);
        assertEquals(7.5, reading.consumptionCost().cost(), 0.000001);
    }

    @Test
    void baseConstructorDoesNotCallOverridableCalculation() {
        assertDoesNotThrow(FixedCostReading::new);
    }

    /** Тестовий підтип перевіряє механізм, не додаючи предметних тарифних правил. */
    private static final class FixedCostReading extends UtilityReading {
        private final boolean initialized;

        private FixedCostReading() {
            super("Тест", "2026-09-01", 10, 4);
            initialized = true;
        }

        @Override
        public double calculateCost() {
            if (!initialized) {
                throw new IllegalStateException("Метод підтипу викликано до завершення ініціалізації");
            }
            return 7.5;
        }
    }
}
