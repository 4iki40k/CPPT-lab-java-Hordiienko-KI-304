package ua.lpnu.kzp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Експортує поля з {@link CsvColumn} будь-якого плоского класу у CSV з кодуванням UTF-8.
 * Експортер не знає предметних класів: стовпці визначаються лише анотаціями.
 */
public final class CsvExporter
{
    /** Забороняє створення екземплярів статичного експортера. */
    private CsvExporter()
    {
    }

    /**
     * Повертає заголовки стовпців класу в порядку експорту.
     *
     * @param type клас записів
     * @return незмінний список значень {@link CsvColumn}
     * @throws DataStorageException якщо клас не має анотованих полів або поле недоступне
     */
    public static List<String> header(Class<?> type) throws DataStorageException
    {
        Objects.requireNonNull(type, "Тип не може бути null");
        return annotatedFields(type).stream()
                .map(field -> field.getAnnotation(CsvColumn.class).value())
                .toList();
    }

    /**
     * Формує CSV-текст із заголовком і рядками записів.
     * Кожен запис, включно з останнім, завершується {@link System#lineSeparator()}.
     *
     * @param <T> тип записів
     * @param type клас записів; потрібен для заголовка навіть без жодного запису
     * @param records записи для експорту
     * @return CSV-текст
     * @throws DataStorageException якщо поля неможливо прочитати або список містить null
     */
    public static <T> String toCsv(Class<T> type, List<? extends T> records) throws DataStorageException
    {
        Objects.requireNonNull(type, "Тип не може бути null");
        Objects.requireNonNull(records, "Записи не можуть бути null");
        List<Field> fields = annotatedFields(type);
        String separator = System.lineSeparator();

        // Один і той самий упорядкований набір полів дає і заголовок, і всі рядки.
        StringBuilder csv = new StringBuilder(fields.stream()
                .map(field -> escape(field.getAnnotation(CsvColumn.class).value()))
                .collect(Collectors.joining(",")))
                .append(separator);

        for (int index = 0; index < records.size(); index++)
        {
            T record = records.get(index);
            if (record == null)
            {
                throw new DataStorageException("Запис " + (index + 1) + " для експорту дорівнює null");
            }

            List<String> values = new ArrayList<>();
            for (Field field : fields)
            {
                try
                {
                    values.add(escape(field.get(record)));
                }
                catch (IllegalAccessException | IllegalArgumentException exception)
                {
                    throw new DataStorageException("Не вдалося прочитати поле " + field.getName() + " запису " + (index + 1), exception);
                }
            }
            csv.append(String.join(",", values)).append(separator);
        }

        return csv.toString();
    }

    /**
     * Записує анотовані поля записів у UTF-8 CSV, перезаписуючи наявний файл.
     *
     * @param <T> тип записів
     * @param path шлях до CSV-файла
     * @param type клас записів
     * @param records записи для експорту
     * @throws DataStorageException якщо поля неможливо прочитати або файл записати
     */
    public static <T> void write(Path path, Class<T> type, List<? extends T> records) throws DataStorageException
    {
        Objects.requireNonNull(path, "Шлях не може бути null");
        String csv = toCsv(type, records);

        try
        {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null)
            {
                Files.createDirectories(parent);
            }
            Files.writeString(path, csv, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        }
        catch (IOException exception)
        {
            throw new DataStorageException("Не вдалося записати CSV: " + path, exception);
        }
    }

    /**
     * Екранує значення за правилами CSV: поле з комою, лапкою, CR чи LF береться в лапки,
     * а лапки всередині подвоюються. null стає порожнім полем.
     *
     * @param value значення поля
     * @return текст поля для CSV
     */
    static String escape(Object value)
    {
        String text = Objects.toString(value, "");
        boolean quoted = text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n");
        return quoted ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
    }

    /**
     * Збирає анотовані поля класу та його суперкласів у стабільному порядку Java-імен.
     *
     * @param type клас записів
     * @return незмінний список доступних полів
     * @throws DataStorageException якщо анотованих полів немає або доступ заборонено
     */
    private static List<Field> annotatedFields(Class<?> type) throws DataStorageException
    {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass())
        {
            fields.addAll(Arrays.stream(current.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(CsvColumn.class))
                    .toList());
        }

        if (fields.isEmpty())
        {
            throw new DataStorageException("Клас " + type.getName() + " не має полів з @CsvColumn");
        }

        // getDeclaredFields не гарантує порядок, тому сортуємо за іменем, а за однакових імен у
        // підкласі та суперкласі — за назвою класу.
        fields.sort(Comparator.comparing(Field::getName).thenComparing(field -> field.getDeclaringClass().getName()));

        for (Field field : fields)
        {
            if (!field.trySetAccessible())
            {
                throw new DataStorageException("Немає доступу до поля " + field.getName());
            }
        }

        return List.copyOf(fields);
    }
}
