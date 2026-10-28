package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Перевіряє фіксовані категорії та їхні підписи. */
class UtilityKindTest {
    @Test
    void providesUkrainianLabels() {
        assertEquals("електроенергія", UtilityKind.ELECTRICITY.label());
        assertEquals("вода", UtilityKind.WATER.label());
        assertEquals("інше", UtilityKind.OTHER.label());
    }

    @Test
    void definesSupportedCategories() {
        assertArrayEquals(new UtilityKind[] {
                UtilityKind.ELECTRICITY, UtilityKind.WATER, UtilityKind.OTHER
        }, UtilityKind.values());
    }

    @ParameterizedTest
    @EnumSource(UtilityKind.class)
    void resolvesEveryCategoryByItsConstantName(UtilityKind kind) {
        assertEquals(kind, UtilityKind.valueOf(kind.name()));
    }

    @ParameterizedTest
    @CsvSource({
            "Електроенергія, ELECTRICITY",
            "ЕЛЕКТРОЕНЕРГІЯ, ELECTRICITY",
            "електрика, ELECTRICITY",
            "Вода, WATER",
            "ВОДА, WATER",
            "Гаряча вода, WATER",
            "холодна вода, WATER"
    })
    void classifiesKnownServiceNames(String meter, UtilityKind expected) {
        assertEquals(expected, UtilityKind.fromMeter(meter));
    }

    @Test
    void ignoresSurroundingSpacesInServiceName() {
        assertEquals(UtilityKind.WATER, UtilityKind.fromMeter("  ВОДА  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Газ", "Опалення", "Вода у підвалі"})
    void unknownNamesRemainOther(String meter) {
        assertEquals(UtilityKind.OTHER, UtilityKind.fromMeter(meter));
    }

    @Test
    void rejectsNullServiceName() {
        assertThrows(IllegalArgumentException.class, () -> UtilityKind.fromMeter(null));
    }
}
