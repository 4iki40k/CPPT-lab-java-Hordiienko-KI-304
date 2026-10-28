package ua.lpnu.kzp;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Перевіряє вибір підтипів без зміни старого формату CSV та вартості. */
class ReadingFactoryTest {
    @ParameterizedTest
    @ValueSource(strings = {"Електроенергія", " ЕЛЕКТРОЕНЕРГІЯ ", "електрика"})
    void createsElectricityWithNeutralExtraParameters(String meter) {
        UtilityReading reading = UtilityReading.fromCsv(meter + ";2026-09-01;10.5;4.32");
        ElectricityReading electricity = assertInstanceOf(ElectricityReading.class, reading);
        assertEquals(meter.strip(), electricity.getMeter());
        assertEquals(UtilityKind.ELECTRICITY, electricity.getKind());
        assertEquals(0.0, electricity.getNightValue(), 0.000001);
        assertEquals(45.36, electricity.calculateCost(), 0.000001);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Вода", " ВОДА ", "Гаряча вода", "холодна вода"})
    void createsWaterWithNeutralExtraTariff(String meter) {
        UtilityReading reading = UtilityReading.fromCsv(meter + ";2026-09-01;10.5;4.32");
        WaterReading water = assertInstanceOf(WaterReading.class, reading);
        assertEquals(meter.strip(), water.getMeter());
        assertEquals(UtilityKind.WATER, water.getKind());
        assertEquals(0.0, water.getWastewaterTariff(), 0.000001);
        assertEquals(45.36, water.calculateCost(), 0.000001);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Газ", "Опалення", "Вода у підвалі"})
    void preservesGenericReadingsForUnknownServices(String meter) {
        UtilityReading reading = UtilityReading.fromCsv(meter + ";2026-09-01;10;4");
        assertEquals(UtilityReading.class, reading.getClass());
        assertEquals(UtilityKind.OTHER, reading.getKind());
        assertEquals(40.0, reading.calculateCost(), 0.000001);
    }

    @Test
    void processesFactorySubtypesInOneBaseTypeList() {
        List<UtilityReading> readings = List.of(
                UtilityReading.fromCsv("Електроенергія;2026-09-01;10;4"),
                UtilityReading.fromCsv("Вода;2026-09-01;10;4"),
                UtilityReading.fromCsv("Газ;2026-09-01;10;4"));
        assertInstanceOf(ElectricityReading.class, readings.get(0));
        assertInstanceOf(WaterReading.class, readings.get(1));
        double total = 0.0;
        for (UtilityReading reading : readings) {
            total += reading.consumptionCost().cost();
        }
        assertEquals(120.0, total, 0.000001);
    }

    @Test
    void doesNotSilentlyAcceptAnExtraSubtypeParameterInCsv() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> UtilityReading.fromCsv("Вода;2026-09-01;10;4;2"));
        assertEquals("Помилка: потрібно рівно 4 поля", error.getMessage());
    }
}
