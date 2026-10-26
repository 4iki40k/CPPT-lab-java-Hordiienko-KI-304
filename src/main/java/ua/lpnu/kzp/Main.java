package ua.lpnu.kzp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Консольний звіт про комунальні показники, лабораторна №1, варіант 9. */
public final class Main {
    /** Забороняє створення об'єктів службового класу з точкою входу. */
    private Main() {
    }

    /**
     * Запускає програму; помилки аргументів або файлів дають ненульовий код завершення.
     *
     * @param args параметри --help, --version, --input та --output
     */
    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /**
     * Виконує запуск без завершення JVM, щоб CLI можна було перевірити тестами.
     *
     * @param args аргументи командного рядка
     * @param out потік звіту або довідки
     * @param err потік повідомлень про помилки запуску
     * @return 0 при успіху, 1 при файловій помилці, 2 при помилці аргументів
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 1 && "--help".equals(args[0])) {
            out.printf("Використання: java -jar lab-java-1.0.0-all.jar [--input <файл>] [--output <файл>]%n"
                    + "  --input    Вхідний CSV, типово data/input.csv%n"
                    + "  --output   Файл звіту, типово out/report.txt%n"
                    + "  --help     Показати довідку (окремий режим)%n"
                    + "  --version  Показати версію (окремий режим)%n");
            return 0;
        }
        if (args.length == 1 && "--version".equals(args[0])) {
            String version = Main.class.getPackage().getImplementationVersion();
            out.printf("lab-java %s%n", version == null ? "development" : version);
            return 0;
        }

        try {
            Path input = Path.of("data", "input.csv");
            Path output = Path.of("out", "report.txt");
            boolean inputSpecified = false;
            boolean outputSpecified = false;
            for (int index = 0; index < args.length; index++) {
                String option = args[index];
                if (!"--input".equals(option) && !"--output".equals(option)) {
                    throw new IllegalArgumentException("Невідомий або несумісний параметр: " + option);
                }
                if (index + 1 >= args.length || args[index + 1].isBlank()
                        || args[index + 1].startsWith("--")) {
                    throw new IllegalArgumentException("Не вказано шлях після " + option);
                }
                Path path = Path.of(args[++index]);
                if ("--input".equals(option)) {
                    if (inputSpecified) {
                        throw new IllegalArgumentException("Параметр --input указано повторно");
                    }
                    input = path;
                    inputSpecified = true;
                } else {
                    if (outputSpecified) {
                        throw new IllegalArgumentException("Параметр --output указано повторно");
                    }
                    output = path;
                    outputSpecified = true;
                }
            }

            // Не дозволяємо випадково перезаписати вхідний CSV звітом.
            if (input.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize())
                    || (Files.exists(output) && Files.isSameFile(input, output))) {
                throw new IllegalArgumentException("Вхідний файл і файл звіту мають бути різними");
            }
            List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
            String report = buildReport(lines);
            writeReport(output, report);
            out.print(report);
            return 0;
        } catch (IllegalArgumentException exception) {
            err.printf("Помилка аргументів: %s%n", exception.getMessage());
            return 2;
        } catch (IOException exception) {
            err.printf("Помилка роботи з файлами: %s%n", exception.getMessage());
            return 1;
        }
    }

    /**
     * Перевіряє структуру, обов'язковий текст і допустимість чисел одного запису.
     *
     * @param line CSV-рядок із чотирма полями, розділеними крапкою з комою
     * @return порожній рядок для коректного запису, інакше причина відхилення
     */
    static String validateLine(String line) {
        String[] fields = line.split(";", -1); // розділення полів
        if (fields.length != 4) {
            return "Помилка: потрібно рівно 4 поля";
        }

        // Початкова валідація написана студентом за консультаціями ШІ.
        String meter = fields[0].strip();
        String date = fields[1].strip();
        // перевірка на порожність
        if (meter.isBlank() || date.isBlank()) {
            return "Помилка: назва лічильника або дата порожня";
        }

        String valueText = fields[2].strip();
        String tariffText = fields[3].strip();
        try {
            double value = Double.parseDouble(valueText);
            double tariff = Double.parseDouble(tariffText);
            if (!Double.isFinite(value) || !Double.isFinite(tariff)) {
                return "Помилка: споживання та тариф мають бути скінченними числами";
            }
            if (value < 0 || tariff < 0) {
                return "Помилка: споживання та тариф не можуть бути від’ємними";
            }
            if (!Double.isFinite(value * tariff)) {
                return "Помилка: вартість запису перевищує допустимий діапазон";
            }
        } catch (NumberFormatException exception) {
            return "Помилка: споживання або тариф має неправильний числовий формат";
        }
        return "";
    }

    /**
     * Обчислює показники лише коректних записів і формує один текст звіту.
     * Рядок, який спричинив би переповнення суми, відхиляється без зміни показників.
     *
     * @param lines рядки вхідного файла
     * @return звіт із показниками, номерами й причинами пропуску хибних рядків
     */
    static String buildReport(List<String> lines) {
        int validCount = 0;
        int errorCount = 0;
        double totalValue = 0.0;
        double totalCost = 0.0;
        double maxValue = 0.0;
        StringBuilder errors = new StringBuilder();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String error = validateLine(line);
            if (error.isEmpty()) {
                String[] fields = line.split(";", -1);
                double value = Double.parseDouble(fields[2].strip());
                double tariff = Double.parseDouble(fields[3].strip());
                double nextValue = totalValue + value;
                double nextCost = totalCost + value * tariff;
                if (!Double.isFinite(nextValue) || !Double.isFinite(nextCost)) {
                    error = "Помилка: сума перевищує допустимий діапазон";
                } else {
                    validCount++;
                    totalValue = nextValue;
                    totalCost = nextCost;
                    maxValue = Math.max(maxValue, value);
                }
            }
            if (!error.isEmpty()) {
                errorCount++;
                errors.append(String.format(Locale.ROOT, "Рядок %d: %s%n", index + 1, error));
            }
        }
        return formatReport(validCount, totalValue, totalCost, maxValue, errorCount, errors.toString());
    }

    /**
     * Форматує показники незалежно від локалі, без файлового чи консольного виводу.
     *
     * @param validCount кількість коректних записів
     * @param totalValue сумарне споживання
     * @param totalCost загальна вартість
     * @param maxValue найбільше споживання
     * @param errorCount кількість хибних записів
     * @param errors повідомлення про хибні записи
     * @return готовий текст звіту
     */
    private static String formatReport(int validCount, double totalValue, double totalCost,
            double maxValue, int errorCount, String errors) {
        String maximum = validCount == 0 ? "немає даних" : String.format(Locale.ROOT, "%.2f", maxValue);
        return String.format(Locale.ROOT,
                "Коректних записів: %d%nСумарне споживання: %.2f%nЗагальна вартість: %.2f%n"
                        + "Найбільше споживання: %s%nПомилок: %d%n%s",
                validCount, totalValue, totalCost, maximum, errorCount, errors);
    }

    /**
     * Записує звіт у UTF-8, створюючи батьківський каталог за потреби.
     *
     * @param output шлях до файла звіту
     * @param report готовий текст
     * @throws IOException якщо каталог або файл неможливо створити чи записати
     */
    static void writeReport(Path output, String report) throws IOException {
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, report, StandardCharsets.UTF_8);
    }
}
