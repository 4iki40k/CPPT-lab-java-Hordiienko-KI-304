package ua.lpnu.kzp;

import java.util.Objects;

/** Запис електроенергії з можливим нічним споживанням. */
public final class ElectricityReading extends UtilityReading {
    /** Частина загального споживання, що припадає на ніч. */
    private final double nightValue;
    /** Коефіцієнт нічного тарифу в межах від нуля до одиниці. */
    private final double nightCoefficient;
    /** Перевірена вартість денного й нічного споживання. */
    private final double cost;

    /**
     * Створює запис без нічного споживання.
     *
     * @param meter назва лічильника
     * @param date дата
     * @param value загальне споживання
     * @param tariff основний тариф
     * @throws IllegalArgumentException якщо спільні значення неприпустимі
     */
    public ElectricityReading(
            String meter, String date, double value, double tariff) {
        this(meter, date, value, tariff, 0.0, 0.5);
    }

    /**
     * Створює запис із нічною частиною споживання.
     *
     * @param meter назва лічильника
     * @param date дата
     * @param value загальне споживання
     * @param tariff основний тариф
     * @param nightValue нічна частина загального споживання
     * @param nightCoefficient коефіцієнт нічного тарифу від 0 до 1
     * @throws IllegalArgumentException якщо значення неприпустимі
     */
    public ElectricityReading(String meter, String date, double value,
            double tariff, double nightValue, double nightCoefficient) {
        super(meter, date, value, tariff);

        if (!Double.isFinite(nightValue)
                || nightValue < 0 || nightValue > value) {
            throw new IllegalArgumentException(
                    "Нічне споживання має бути скінченним і в межах загального");
        }

        if (!Double.isFinite(nightCoefficient)
                || nightCoefficient < 0 || nightCoefficient > 1) {
            throw new IllegalArgumentException(
                    "Нічний коефіцієнт має бути скінченним і в межах від 0 до 1");
        }

        double dayValue = value - nightValue;
        double calculatedCost = dayValue * tariff
                + nightValue * tariff * nightCoefficient;

        if (!Double.isFinite(calculatedCost)) {
            throw new IllegalArgumentException(
                    "Вартість електроенергії перевищує допустимий діапазон");
        }

        this.nightValue = nightValue;
        this.nightCoefficient = nightCoefficient;
        this.cost = calculatedCost;
    }

    /**
     * Повертає нічну частину споживання.
     *
     * @return нічне споживання
     */
    public double getNightValue() {
        return nightValue;
    }

    /**
     * Повертає коефіцієнт нічного тарифу.
     *
     * @return нічний коефіцієнт
     */
    public double getNightCoefficient() {
        return nightCoefficient;
    }

    /**
     * Повертає категорію електроенергії.
     * @return категорія ELECTRICITY
     */
    @Override
    public UtilityKind getKind() {
        return UtilityKind.ELECTRICITY;
    }

    /** Повертає вартість денного та нічного споживання. */
    @Override
    public double calculateCost() {
        return cost;
    }

    /** Порівнює спільні поля та параметри нічного споживання. */
    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        ElectricityReading reading = (ElectricityReading) other;
        return Double.compare(nightValue, reading.nightValue) == 0
                && Double.compare(nightCoefficient, reading.nightCoefficient) == 0;
    }

    /** Повертає хеш-код із урахуванням нічних параметрів. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nightValue, nightCoefficient);
    }
}
