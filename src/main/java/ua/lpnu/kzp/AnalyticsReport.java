package ua.lpnu.kzp;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/** Форматує результати аналітичних запитів без файлового й консольного виводу. */
public final class AnalyticsReport
{
    private static final double THRESHOLD = 50.0;
    private static final int TOP_LIMIT = 5;
    private static final String SEARCH_METER = "Вода";
    private static final String MISSING_METER = "Тепло";

    private AnalyticsReport()
    {
    }

    /**
     * Формує аналітичний звіт із прийнятих записів.
     *
     * @param readings записи, що пройшли валідацію та перевірку сум
     * @return один текст результатів усіх запитів
     * @throws NullPointerException якщо список null
     */
    public static String build(List<UtilityReading> readings)
    {
        Objects.requireNonNull(readings, "Список не може бути null");

        StringBuilder report = new StringBuilder();

        report.append(line("Поріг споживання", number(THRESHOLD)));
        report.append(line("Понад поріг", describeReadings(UtilityQueries.aboveThreshold(readings, THRESHOLD))));

        String values = UtilityQueries.consumptions(readings).stream()
                .map(AnalyticsReport::number)
                .collect(Collectors.joining(", ", "[", "]"));

        report.append(line("Споживання", values));

        String groups = UtilityQueries.consumptionByMeter(readings).entrySet().stream()
                .map(entry -> String.format(Locale.ROOT, "%s=%.2f", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));

        report.append(line("Суми за лічильниками", groups));

        DoubleSummaryStatistics statistics = UtilityQueries.consumptionStatistics(readings);

        report.append(line("Кількість", statistics.getCount()));
        report.append(line("Сума", number(statistics.getSum())));
        report.append(line("Середнє", number(statistics.getAverage())));

        String minimum = statistics.getCount() == 0 ? "немає даних" : number(statistics.getMin());
        String maximum = statistics.getCount() == 0 ? "немає даних" : number(statistics.getMax());

        report.append(line("Мінімум", minimum));
        report.append(line("Максимум", maximum));

        report.append(line("Топ-5", describeReadings(UtilityQueries.topByConsumption(readings, TOP_LIMIT))));

        String found = UtilityQueries.findByMeter(readings, SEARCH_METER)
                .map(AnalyticsReport::describe)
                .orElse("Не знайдено");

        String missing = UtilityQueries.findByMeter(readings, MISSING_METER)
                .map(AnalyticsReport::describe)
                .orElse("Не знайдено");

        report.append(line("Пошук Вода", found));
        report.append(line("Пошук Тепло", missing));

        return report.toString();
    }

    /** Перетворює список записів на текст для людини. */
    private static String describeReadings(List<UtilityReading> readings)
    {
        if (readings.isEmpty())
        {
            return "немає даних";
        }

        return readings.stream()
                .map(AnalyticsReport::describe)
                .collect(Collectors.joining(", "));
    }

    /** Описує один запис, не змінюючи його. */
    private static String describe(UtilityReading reading)
    {
        return String.format(Locale.ROOT, "%s (%s): %.2f", reading.getMeter(), reading.getDate(), reading.getValue());
    }

    /** Форматує дробове число з двома знаками після крапки. */
    private static String number(double value)
    {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** Формує підписаний рядок із платформним переносом. */
    private static String line(String title, Object value)
    {
        return String.format(Locale.ROOT, "%s: %s%n", title, value);
    }
}
