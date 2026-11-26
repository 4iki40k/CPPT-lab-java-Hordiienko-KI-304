package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Плоский незмінний запис комунального показника для CSV-збереження, варіант 9.
 * Порядок стовпців задає експортер сортуванням Java-імен полів: date, meter, tariff, value.
 * Ключ запису для звіту round-trip — meter + date.
 */
public final class UtilityReadingRecord
{
    /** Кількість стовпців CSV. */
    private static final int FIELD_COUNT = 4;

    /** Назва лічильника без крайових пробілів. */
    @CsvColumn("лічильник")
    private final String meter;

    /** Календарна дата показника. */
    @CsvColumn("дата")
    private final LocalDate date;

    /** Скінченне невід'ємне споживання. */
    @CsvColumn("значення")
    private final double value;

    /** Скінченний невід'ємний тариф. */
    @CsvColumn("тариф")
    private final double tariff;

    /**
     * Створює перевірений запис.
     *
     * @param meter назва лічильника; крайові пробіли прибираються
     * @param date календарна дата
     * @param value споживання
     * @param tariff тариф
     * @throws IllegalArgumentException якщо назва порожня, дата відсутня або число неприпустиме
     */
    public UtilityReadingRecord(String meter, LocalDate date, double value, double tariff)
    {
        if (meter == null || meter.isBlank())
        {
            throw new IllegalArgumentException("Назва лічильника не може бути порожньою");
        }
        if (date == null)
        {
            throw new IllegalArgumentException("Дата не може бути null");
        }
        if (!Double.isFinite(value) || !Double.isFinite(tariff) || value < 0 || tariff < 0)
        {
            throw new IllegalArgumentException("Споживання та тариф мають бути скінченними невід'ємними числами");
        }
        this.meter = meter.strip();
        this.date = date;
        this.value = value;
        this.tariff = tariff;
    }

    /**
     * Створює плоский запис із поліморфного показника; додаткові параметри підтипів не зберігаються.
     *
     * @param reading коректний показник
     * @return запис зі спільними полями meter, date, value, tariff
     */
    public static UtilityReadingRecord from(UtilityReading reading)
    {
        Objects.requireNonNull(reading, "Показник не може бути null");
        return new UtilityReadingRecord(reading.getMeter(), reading.getLocalDate(), reading.getValue(), reading.getTariff());
    }

    /**
     * Розбирає один CSV-запис у порядку стовпців експорту: дата, лічильник, тариф, значення.
     *
     * @param line текст одного CSV-запису
     * @return відновлений запис
     * @throws DataStorageException якщо формат або значення некоректні
     */
    public static UtilityReadingRecord fromCsv(String line) throws DataStorageException
    {
        return fromFields(CsvParser.parse(line));
    }

    /**
     * Створює запис із уже розібраних полів у порядку експорту.
     *
     * @param fields поля дата, лічильник, тариф, значення
     * @return відновлений запис
     * @throws DataStorageException якщо кількість полів, число, дата або інваріант некоректні; причина зберігається
     */
    public static UtilityReadingRecord fromFields(List<String> fields) throws DataStorageException
    {
        Objects.requireNonNull(fields, "Поля не можуть бути null");
        if (fields.size() != FIELD_COUNT)
        {
            throw new DataStorageException("Очікується " + FIELD_COUNT + " поля CSV, отримано " + fields.size());
        }

        LocalDate date;
        try
        {
            date = LocalDate.parse(fields.get(0).strip());
        }
        catch (DateTimeParseException exception)
        {
            throw new DataStorageException("Поле «дата» має бути календарною датою YYYY-MM-DD: " + fields.get(0), exception);
        }

        double tariff = parseNumber(fields.get(2), "тариф");
        double value = parseNumber(fields.get(3), "значення");

        try
        {
            return new UtilityReadingRecord(fields.get(1), date, value, tariff);
        }
        catch (IllegalArgumentException exception)
        {
            throw new DataStorageException("Некоректний запис показника", exception);
        }
    }

    /**
     * Читає UTF-8 CSV, перевіряє заголовок і відновлює всі записи.
     * Файл лише із заголовком дає порожній список; повністю порожній файл є помилкою.
     *
     * @param path шлях до CSV-файла
     * @return незмінний список записів у порядку файла
     * @throws DataStorageException якщо файл неможливо прочитати, заголовок хибний або запис некоректний
     */
    public static List<UtilityReadingRecord> readCsv(Path path) throws DataStorageException
    {
        Objects.requireNonNull(path, "Шлях не може бути null");
        String text;
        try
        {
            text = Files.readString(path, StandardCharsets.UTF_8);
        }
        catch (IOException exception)
        {
            throw new DataStorageException("Не вдалося прочитати CSV: " + path, exception);
        }

        List<List<String>> rows = CsvParser.parseDocument(text);
        if (rows.isEmpty())
        {
            throw new DataStorageException("CSV-файл порожній, немає заголовка: " + path);
        }

        List<String> expected = CsvExporter.header(UtilityReadingRecord.class);
        if (!rows.get(0).equals(expected))
        {
            throw new DataStorageException("Очікується заголовок " + String.join(",", expected) + ", отримано " + String.join(",", rows.get(0)));
        }

        List<UtilityReadingRecord> records = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++)
        {
            try
            {
                records.add(fromFields(rows.get(index)));
            }
            catch (DataStorageException exception)
            {
                throw new DataStorageException("CSV-запис " + (index + 1) + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(records);
    }

    /**
     * Перетворює текст на число з крапкою.
     *
     * @param text текст поля
     * @param column назва стовпця для повідомлення
     * @return число
     * @throws DataStorageException якщо текст не є числом; причина — NumberFormatException
     */
    private static double parseNumber(String text, String column) throws DataStorageException
    {
        try
        {
            return Double.parseDouble(text.strip());
        }
        catch (NumberFormatException exception)
        {
            throw new DataStorageException("Поле «" + column + "» має бути числом: " + text, exception);
        }
    }

    /**
     * Повертає назву лічильника.
     *
     * @return назва лічильника
     */
    public String getMeter()
    {
        return meter;
    }

    /**
     * Повертає дату показника.
     *
     * @return календарна дата
     */
    public LocalDate getDate()
    {
        return date;
    }

    /**
     * Повертає споживання.
     *
     * @return споживання
     */
    public double getValue()
    {
        return value;
    }

    /**
     * Повертає тариф.
     *
     * @return тариф
     */
    public double getTariff()
    {
        return tariff;
    }

    /** Порівнює записи за всіма полями; числа — через Double.compare. */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof UtilityReadingRecord record))
        {
            return false;
        }
        return meter.equals(record.meter)
                && date.equals(record.date)
                && Double.compare(value, record.value) == 0
                && Double.compare(tariff, record.tariff) == 0;
    }

    /** Повертає хеш-код, узгоджений з {@link #equals(Object)}. */
    @Override
    public int hashCode()
    {
        return Objects.hash(meter, date, value, tariff);
    }

    /** Повертає опис запису з ключем meter + date. */
    @Override
    public String toString()
    {
        return String.format(Locale.ROOT, "%s (%s): значення %s, тариф %s", meter, date, value, tariff);
    }
}
