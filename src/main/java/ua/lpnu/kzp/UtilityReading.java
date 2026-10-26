package ua.lpnu.kzp;

import java.util.Locale;

/** Незмінний запис про комунальний показник. */
public final class UtilityReading {
    private final String meter;
    private final String date;
    private final double value;
    private final double tariff;


    /**
     * Створює запис після перевірки всіх значень.
     *
     * @param meter назва лічильника
     * @param date дата як текст
     * @param value споживання
     * @param tariff тариф
     * @throws IllegalArgumentException якщо значення неприпустимі
     */
    public UtilityReading(String meter, String date, double value, double tariff) {
        validateTextFields(meter, date);

        if (!Double.isFinite(value) || !Double.isFinite(tariff)) {
            throw new IllegalArgumentException(
                    "Помилка: споживання та тариф мають бути скінченними числами");
        }

        if (value < 0 || tariff < 0) {
            throw new IllegalArgumentException(
                    "Помилка: споживання та тариф не можуть бути від’ємними");
        }

        if (!Double.isFinite(value * tariff)) {
            throw new IllegalArgumentException(
                    "Помилка: вартість запису перевищує допустимий діапазон");
        }

        this.meter = meter.strip();
        this.date = date.strip();
        this.value = value;
        this.tariff = tariff;
    }

    /** Перевіряє обов’язкові текстові поля. */
    private static void validateTextFields(String meter, String date) {
        if (meter == null || date == null) {
            throw new IllegalArgumentException(
                    "Помилка: назва лічильника та дата не можуть бути null");
        }

        if (meter.isBlank() || date.isBlank()) {
            throw new IllegalArgumentException(
                    "Помилка: назва лічильника або дата порожня");
        }
    }

    /**
     * Створює запис із CSV-рядка.
     *
     * @param line рядок meter;date;value;tariff
     * @return коректний комунальний запис
     * @throws IllegalArgumentException якщо рядок або значення некоректні
     */
    public static UtilityReading fromCsv(String line) {
        if (line == null) {
            throw new IllegalArgumentException("Помилка: CSV-рядок не може бути null");
        }

        String[] fields = line.split(";", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("Помилка: потрібно рівно 4 поля");
        }

        validateTextFields(fields[0], fields[1]);

        try {
            double value = Double.parseDouble(fields[2].strip());
            double tariff = Double.parseDouble(fields[3].strip());

            return new UtilityReading(fields[0], fields[1], value, tariff);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Помилка: споживання або тариф має неправильний числовий формат",
                     exception);
        }
    }

    /** @return назва лічильника */
    public String getMeter() {
        return meter;
    }

    /** @return дата як текст */
    public String getDate() {
        return date;
    }

    /** @return споживання */
    public double getValue() {
        return value;
    }

    /** @return тариф */
    public double getTariff() {
        return tariff;
    }

    /** Повертає зрозумілий текстовий опис запису. */
    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "%s (%s): споживання %.2f, тариф %.2f",
                meter, date, value, tariff);
    }

    /**
     * Повертає споживання та обчислену вартість запису.
     *
     * @return незмінна пара споживання та вартості
     */
    public ConsumptionCost consumptionCost() {
        return new ConsumptionCost(value, value * tariff);
    }
}
