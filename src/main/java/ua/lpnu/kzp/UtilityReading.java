package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;

/** Базовий комунальний запис зі спільними незмінними полями та валідацією. */
public class UtilityReading implements CostCalculable {
    /** Назва лічильника без крайових пробілів. */
    private final String meter;
    /** Дата як непорожній текст. */
    private final String date;
    /** Скінченне невід'ємне споживання. */
    private final double value;
    /** Скінченний невід'ємний тариф. */
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
        this(validateReading(meter, date, value, tariff),
                date.strip(), value, tariff, true);
    }

    /**
     * Присвоює вже перевірені й нормалізовані значення без повторної валідації.
     *
     * @param meter очищена назва лічильника
     * @param date очищена дата
     * @param value перевірене споживання
     * @param tariff перевірений тариф
     * @param validated службовий параметр для відмінної сигнатури конструктора
     */
    private UtilityReading(String meter, String date, double value,
            double tariff, boolean validated) {
        this.meter = meter;
        this.date = date;
        this.value = value;
        this.tariff = tariff;
    }

    /**
     * Перевіряє обов’язкові текстові поля за спільними правилами конструктора та фабрики.
     *
     * @param meter назва лічильника
     * @param date дата як текст
     * @throws IllegalArgumentException якщо поле null або порожнє
     */
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
     * Перевіряє весь запис і повертає очищену назву лічильника.
     *
     * @param meter назва лічильника
     * @param date дата як текст
     * @param value споживання
     * @param tariff тариф
     * @return назва без крайових пробілів
     * @throws IllegalArgumentException якщо значення неприпустимі
     */
    private static String validateReading(
            String meter, String date, double value, double tariff) {
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

        return meter.strip();
    }

    /**
     * Створює відповідний підтип із CSV-рядка за категорією назви послуги.
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

            UtilityKind kind = UtilityKind.fromMeter(fields[0]);
            return switch (kind) {
                case ELECTRICITY ->
                        new ElectricityReading(fields[0], fields[1], value, tariff);
                case WATER ->
                        new WaterReading(fields[0], fields[1], value, tariff);
                case OTHER ->
                        new UtilityReading(fields[0], fields[1], value, tariff);
            };
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Помилка: споживання або тариф має неправильний числовий формат",
                    exception);
        }
    }

    /**
     * Читає назву лічильника.
     * @return назва лічильника
     */
    public String getMeter() {
        return meter;
    }

    /**
     * Читає дату запису.
     * @return дата як текст
     */
    public String getDate() {
        return date;
    }

    /**
     * Читає споживання.
     * @return споживання
     */
    public double getValue() {
        return value;
    }

    /**
     * Читає тариф.
     * @return тариф
     */
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
     * Обчислює вартість звичайного комунального запису.
     *
     * @return споживання, помножене на тариф
     */
    @Override
    public double calculateCost() {
        return value * tariff;
    }

    /**
     * Повертає споживання та обчислену вартість запису.
     *
     * @return незмінна пара споживання та вартості
     */
    public ConsumptionCost consumptionCost() {
        return new ConsumptionCost(value, calculateCost());
    }

    /**
     * Повертає категорію звичайного комунального запису.
     *
     * @return категорія OTHER
     */
    public UtilityKind getKind() {
        return UtilityKind.OTHER;
    }

    /** Порівнює записи одного фактичного класу за спільними полями. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        UtilityReading reading = (UtilityReading) other;

        return meter.equals(reading.meter)
                && date.equals(reading.date)
                && Double.compare(value, reading.value) == 0
                && Double.compare(tariff, reading.tariff) == 0;
    }

    /** Повертає хеш-код, узгоджений із логічною рівністю. */
    @Override
    public int hashCode() {
        return Objects.hash(getClass(), meter, date, value, tariff);
    }
}
