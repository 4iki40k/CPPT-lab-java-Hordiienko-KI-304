package ua.lpnu.kzp;

import java.util.Objects;

/** Комунальний запис води з можливим тарифом водовідведення. */
public final class WaterReading extends UtilityReading
{
    /** Скінченний невід'ємний тариф водовідведення. */
    private final double wastewaterTariff;
    /** Перевірена вартість, обчислена під час створення незмінного об'єкта. */
    private final double cost;

    /**
     * Створює запис без додаткової плати за водовідведення.
     *
     * @param meter назва лічильника
     * @param date дата
     * @param value споживання
     * @param tariff тариф водопостачання
     * @throws IllegalArgumentException якщо спільні значення неприпустимі
     */
    public WaterReading(String meter, String date, double value, double tariff)
    {
        this(meter, date, value, tariff, 0.0);
    }

    /**
     * Створює запис із додатковим тарифом водовідведення.
     *
     * @param meter назва лічильника
     * @param date дата
     * @param value споживання
     * @param tariff тариф водопостачання
     * @param wastewaterTariff тариф водовідведення
     * @throws IllegalArgumentException якщо значення неприпустимі
     */
    public WaterReading(String meter, String date, double value, double tariff, double wastewaterTariff)
    {
        super(meter, date, value, tariff);

        if (!Double.isFinite(wastewaterTariff) || wastewaterTariff < 0)
        {
            throw new IllegalArgumentException("Тариф водовідведення має бути скінченним і невід’ємним");
        }

        double calculatedCost = value * tariff + value * wastewaterTariff;

        if (!Double.isFinite(calculatedCost))
        {
            throw new IllegalArgumentException("Вартість водопостачання та водовідведення перевищує допустимий діапазон");
        }

        this.wastewaterTariff = wastewaterTariff;
        this.cost = calculatedCost;
    }

    /**
     * Повертає додатковий тариф водовідведення.
     *
     * @return тариф водовідведення
     */
    public double getWastewaterTariff()
    {
        return wastewaterTariff;
    }

    /**
     * Повертає категорію водопостачання.
     * @return категорія WATER
     */
    @Override
    public UtilityKind getKind()
    {
        return UtilityKind.WATER;
    }

    /** Повертає вартість водопостачання та водовідведення. */
    @Override
    public double calculateCost()
    {
        return cost;
    }

    /** Порівнює спільні поля та тариф водовідведення. */
    @Override
    public boolean equals(Object other)
    {
        if (!super.equals(other))
        {
            return false;
        }

        WaterReading reading = (WaterReading) other;
        return Double.compare(wastewaterTariff, reading.wastewaterTariff) == 0;
    }

    /** Повертає хеш-код із урахуванням тарифу водовідведення. */
    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), wastewaterTariff);
    }
}
