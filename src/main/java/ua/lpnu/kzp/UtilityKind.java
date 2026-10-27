package ua.lpnu.kzp;

import java.util.Locale;

/** Категорія комунального запису. */
public enum UtilityKind {
    ELECTRICITY("електроенергія"),
    WATER("вода"),
    OTHER("інше");

    private final String label;

    UtilityKind(String label) {
        this.label = label;
    }

    /**
     * Повертає зрозумілий підпис категорії.
     *
     * @return підпис українською
     */
    public String label() {
        return label;
    }

    /**
     * Визначає категорію за назвою послуги.
     *
     * @param meter назва послуги
     * @return відповідна категорія або OTHER для невідомої назви
     * @throws IllegalArgumentException якщо назва null
     */
    public static UtilityKind fromMeter(String meter) {
        if (meter == null) {
            throw new IllegalArgumentException(
                    "Помилка: назва лічильника не може бути null");
        }

        String normalized = meter.strip().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "електроенергія", "електрика" -> ELECTRICITY;
            case "вода", "гаряча вода", "холодна вода" -> WATER;
                default -> OTHER;
        };
    }
}
