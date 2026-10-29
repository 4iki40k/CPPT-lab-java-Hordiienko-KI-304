package ua.lpnu.kzp;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.Optional;

/** Чисті запити до колекції комунальних записів. */
public final class UtilityQueries
{
    private UtilityQueries()
    {
    }

    /**
     * Відбирає записи зі споживанням строго понад поріг.
     *
     * @param readings вхідні записи
     * @param threshold скінченний поріг
     * @return новий список відібраних записів
     * @throws NullPointerException якщо список null
     * @throws IllegalArgumentException якщо поріг нескінченний або NaN
     */
    public static List<UtilityReading> aboveThreshold(List<UtilityReading> readings, double threshold)
    {
        Objects.requireNonNull(readings, "Список не може бути null");

        if (!Double.isFinite(threshold))
        {
            throw new IllegalArgumentException("Поріг має бути скінченним числом");
        }

        return readings.stream()
                .filter(reading -> reading.getValue() > threshold)
                .toList();
    }

    /**
     * Дістає споживання всіх записів у початковому порядку.
     *
     * @param readings вхідні записи
     * @return новий список значень споживання
     * @throws NullPointerException якщо список null
     */
    public static List<Double> consumptions(List<UtilityReading> readings)
    {
        Objects.requireNonNull(readings, "Список не може бути null");

        return readings.stream()
                .map(UtilityReading::getValue)
                .toList();
    }

    /**
     * Підсумовує споживання за назвою лічильника.
     *
     * @param readings вхідні записи
     * @return нова карта з назвами лічильників у відсортованому порядку
     * @throws NullPointerException якщо список null
     */
    public static Map<String, Double> consumptionByMeter(List<UtilityReading> readings)
    {
        Objects.requireNonNull(readings, "Список не може бути null");

        return readings.stream()
                .collect(Collectors.groupingBy(UtilityReading::getMeter, TreeMap::new, Collectors.summingDouble(UtilityReading::getValue)));
    }

    /**
     * Обчислює зведену статистику споживання.
     *
     * @param readings вхідні записи
     * @return кількість, сума, середнє, мінімум і максимум;
     *         для порожнього входу min і max мають спеціальні значення нескінченності
     * @throws NullPointerException якщо список null
     */
    public static DoubleSummaryStatistics consumptionStatistics(List<UtilityReading> readings)
    {
        Objects.requireNonNull(readings, "Список не може бути null");

        return readings.stream()
                .collect(Collectors.summarizingDouble(UtilityReading::getValue));
    }

    /**
     * Вибирає найбільші споживання, за рівності впорядковує за датою.
     *
     * @param readings вхідні записи
     * @param limit максимальна кількість результатів
     * @return новий список; за рівності обох ключів зберігається початковий порядок
     * @throws NullPointerException якщо список null
     * @throws IllegalArgumentException якщо limit від'ємний
     */
    public static List<UtilityReading> topByConsumption(List<UtilityReading> readings, int limit)
    {
        Objects.requireNonNull(readings, "Список не може бути null");

        if (limit < 0)
        {
            throw new IllegalArgumentException("Ліміт не може бути від’ємним");
        }

        Comparator<UtilityReading> order = Comparator
                .comparingDouble(UtilityReading::getValue)
                .reversed()
                .thenComparing(UtilityReading::getLocalDate);

        return readings.stream()
                .sorted(order)
                .limit(limit)
                .toList();
    }

    /**
     * Шукає перший запис за точною назвою лічильника без крайових пробілів.
     *
     * @param readings вхідні записи
     * @param meter назва для пошуку; порівняння чутливе до регістру
     * @return перший відповідний запис або порожній Optional
     * @throws NullPointerException якщо список або назва null
     */
    public static Optional<UtilityReading> findByMeter(List<UtilityReading> readings, String meter)
    {
        Objects.requireNonNull(readings, "Список не може бути null");
        Objects.requireNonNull(meter, "Назва не може бути null");

        String key = meter.strip();

        return readings.stream()
                .filter(reading -> reading.getMeter().equals(key))
                .findFirst();
    }
}
