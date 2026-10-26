package ua.lpnu.kzp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє аргументи, файлові операції та однаковий звіт у файлі й консолі. */
class CliTest {
    @TempDir
    Path directory;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        try (PrintStream stdout = new PrintStream(out, true, StandardCharsets.UTF_8);
                PrintStream stderr = new PrintStream(err, true, StandardCharsets.UTF_8)) {
            return Main.run(args, stdout, stderr);
        }
    }

    @Test
    void helpDoesNotNeedInputFile() {
        assertEquals(0, run("--help"));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("--input"));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void classpathVersionExplainsDevelopmentMode() {
        assertEquals(0, run("--version"));
        assertEquals("lab-java development" + System.lineSeparator(), out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void writesSameUtf8ReportToConsoleAndNestedOutputPath() throws IOException {
        Path input = directory.resolve("українські дані.csv");
        Files.writeString(input, "Вода;2026-09-01;10;30\nГаз;2026-09-01;помилка;8\n",
                StandardCharsets.UTF_8);
        Path output = directory.resolve("нова тека").resolve("звіт.txt");
        assertEquals(0, run("--output", output.toString(), "--input", input.toString()));
        String report = Files.readString(output, StandardCharsets.UTF_8);
        assertEquals(report, out.toString(StandardCharsets.UTF_8));
        assertTrue(report.contains("Загальна вартість: 300.00"));
        assertTrue(report.contains("Рядок 2:"));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsUnknownOption() {
        assertEquals(2, run("--unknown"));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Невідомий"));
    }

    @Test
    void rejectsMissingPath() {
        assertEquals(2, run("--input"));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Не вказано шлях"));
    }

    @Test
    void doesNotTreatNextOptionAsPath() {
        assertEquals(2, run("--input", "--output", "report.txt"));
    }

    @Test
    void rejectsRepeatedOption() {
        assertEquals(2, run("--input", "one.csv", "--input", "two.csv"));
    }

    @Test
    void rejectsInvalidPath() {
        assertEquals(2, run("--input", "invalid\u0000.csv"));
    }

    @Test
    void reportsMissingFileWithoutStackTrace() {
        assertEquals(1, run("--input", directory.resolve("missing.csv").toString()));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Помилка роботи з файлами"));
        assertEquals("", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsOverwritingInput() throws IOException {
        Path input = directory.resolve("input.csv");
        String contents = "Вода;2026-09-01;10;30";
        Files.writeString(input, contents, StandardCharsets.UTF_8);
        assertEquals(2, run("--input", input.toString(), "--output", input.toString()));
        assertEquals(contents, Files.readString(input, StandardCharsets.UTF_8));
    }

    @Test
    void reportsOutputFailure() throws IOException {
        Path input = directory.resolve("input.csv");
        Files.writeString(input, "Вода;2026-09-01;10;30", StandardCharsets.UTF_8);
        Path blocker = directory.resolve("not-a-directory");
        Files.writeString(blocker, "file", StandardCharsets.UTF_8);
        assertEquals(1, run("--input", input.toString(), "--output", blocker.resolve("report.txt").toString()));
        assertFalse(err.toString(StandardCharsets.UTF_8).isEmpty());
    }

    @Test
    void writesEmptyFileReport() throws IOException {
        Path input = directory.resolve("empty.csv");
        Files.writeString(input, "", StandardCharsets.UTF_8);
        Path output = directory.resolve("report.txt");
        assertEquals(0, run("--input", input.toString(), "--output", output.toString()));
        assertTrue(Files.readString(output, StandardCharsets.UTF_8).contains("немає даних"));
    }

    @Test
    void writesReportWithoutParentDirectoryComponent() throws IOException {
        // Абсолютний шлях нижче не потрібний: перевіряємо гілку parent == null.
        Path output = Path.of("lab01-test-report-" + directory.getFileName() + ".txt");
        try {
            Main.writeReport(output, "Український звіт");
            assertEquals("Український звіт", Files.readString(output, StandardCharsets.UTF_8));
        } finally {
            Files.deleteIfExists(output);
        }
    }
}
