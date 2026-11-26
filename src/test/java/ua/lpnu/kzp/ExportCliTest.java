package ua.lpnu.kzp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє режим --export, сховище в Main і незмінність попередніх режимів. */
class ExportCliTest
{
    private static final String CONTROL_SUMMARY = String.join(System.lineSeparator(),
            "Записів у сховищі: 6",
            "Заголовок: дата,лічильник,тариф,значення",
            "Фізичних рядків CSV: 7",
            "Відновлених об'єктів: 6",
            "Round-trip: true",
            "Помилок: 1",
            "Рядок 7: Помилка: споживання або тариф має неправильний числовий формат",
            "");

    @TempDir
    Path directory;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args)
    {
        out.reset();
        err.reset();
        try (PrintStream stdout = new PrintStream(out, true, StandardCharsets.UTF_8); PrintStream stderr = new PrintStream(err, true, StandardCharsets.UTF_8))
        {
            return Main.run(args, stdout, stderr);
        }
    }

    private String stdout()
    {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String stderr()
    {
        return err.toString(StandardCharsets.UTF_8);
    }

    @Test
    void controlStorageFileExportsAndRoundTrips() throws IOException, DataStorageException
    {
        Path output = directory.resolve("readings.csv");
        assertEquals(0, run("--export", "--input", Path.of("data", "storage.csv").toString(), "--output", output.toString()));
        assertEquals(CONTROL_SUMMARY, stdout());
        assertEquals("", stderr());

        String csv = Files.readString(output, StandardCharsets.UTF_8);
        String nl = System.lineSeparator();
        assertTrue(csv.startsWith("дата,лічильник,тариф,значення" + nl + "2026-09-01,Електроенергія,4.32,120.5" + nl));
        assertTrue(csv.contains("2026-09-01,\"Вода, гаряча\",98.5,3.2" + nl));
        assertTrue(csv.contains("2026-09-01,\"Газ \"\"Схід\"\"\",7.96,25.0" + nl));
        assertEquals(6, UtilityReadingRecord.readCsv(output).size());
    }

    @Test
    void exportedRecordsMatchAcceptedReadingsOfInput() throws IOException, DataStorageException
    {
        Path output = directory.resolve("readings.csv");
        assertEquals(0, run("--export", "--input", Path.of("data", "storage.csv").toString(), "--output", output.toString()));
        List<UtilityReadingRecord> expected = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("data", "storage.csv"), StandardCharsets.UTF_8))
        {
            try
            {
                expected.add(UtilityReadingRecord.from(UtilityReading.fromCsv(line)));
            }
            catch (IllegalArgumentException exception)
            {
                // Хибний рядок не експортується, як і в інших режимах.
            }
        }
        assertEquals(expected, UtilityReadingRecord.readCsv(output));
    }

    @Test
    void optionOrderDoesNotMatter() throws IOException
    {
        Path first = directory.resolve("a.csv");
        Path second = directory.resolve("b.csv");
        String input = Path.of("data", "storage.csv").toString();
        assertEquals(0, run("--export", "--input", input, "--output", first.toString()));
        assertEquals(0, run("--output", second.toString(), "--input", input, "--export"));
        assertEquals(Files.readString(first, StandardCharsets.UTF_8), Files.readString(second, StandardCharsets.UTF_8));
    }

    @Test
    void inputWithoutValidLinesGivesHeaderOnlyCsv() throws IOException
    {
        Path input = Files.writeString(directory.resolve("bad.csv"), "Газ;2026-09-02;-5;8\n", StandardCharsets.UTF_8);
        Path output = directory.resolve("empty.csv");
        assertEquals(0, run("--export", "--input", input.toString(), "--output", output.toString()));
        assertTrue(stdout().contains("Записів у сховищі: 0"));
        assertTrue(stdout().contains("Фізичних рядків CSV: 1"));
        assertTrue(stdout().contains("Round-trip: true"));
        assertTrue(stdout().contains("Помилок: 1"));
        assertEquals("дата,лічильник,тариф,значення" + System.lineSeparator(), Files.readString(output, StandardCharsets.UTF_8));
    }

    @Test
    void shorterExportReplacesOldFile() throws IOException
    {
        Path output = Files.writeString(directory.resolve("readings.csv"), "старий рядок\n".repeat(50), StandardCharsets.UTF_8);
        Path input = Files.writeString(directory.resolve("one.csv"), "Вода;2026-09-01;5;30\n", StandardCharsets.UTF_8);
        assertEquals(0, run("--export", "--input", input.toString(), "--output", output.toString()));
        assertFalse(Files.readString(output, StandardCharsets.UTF_8).contains("старий"));
    }

    @Test
    void analyticsAndExportAreIncompatible()
    {
        assertEquals(2, run("--export", "--analytics"));
        assertTrue(stderr().contains("несумісні"));
        assertEquals("", stdout());
        assertEquals(2, run("--analytics", "--export"));
    }

    @Test
    void repeatedExportIsRejected()
    {
        assertEquals(2, run("--export", "--export"));
        assertTrue(stderr().contains("--export"));
    }

    @Test
    void exportOverInputIsRejected()
    {
        Path input = Path.of("data", "storage.csv");
        assertEquals(2, run("--export", "--input", input.toString(), "--output", input.toString()));
    }

    @Test
    void missingInputIsFileError()
    {
        assertEquals(1, run("--export", "--input", directory.resolve("missing.csv").toString(), "--output", directory.resolve("r.csv").toString()));
        assertTrue(stderr().contains("Помилка роботи з файлами"));
    }

    @Test
    void unwritableOutputIsStorageErrorWithCause() throws IOException
    {
        Path blocker = Files.writeString(directory.resolve("blocker"), "x", StandardCharsets.UTF_8);
        assertEquals(1, run("--export", "--input", Path.of("data", "storage.csv").toString(), "--output", blocker.resolve("r.csv").toString()));
        assertTrue(stderr().startsWith("Помилка збереження даних: Не вдалося записати CSV"), stderr());
        assertTrue(stderr().contains("причина: java.nio.file."), stderr());
        assertEquals("", stdout());
    }

    @Test
    void helpDescribesExport()
    {
        assertEquals(0, run("--help"));
        assertTrue(stdout().contains("--export"));
        assertTrue(stdout().contains("data/storage.csv"));
        assertTrue(stdout().contains("out/readings.csv"));
    }

    @Test
    void previousReportsAreUnchangedByRepository() throws IOException
    {
        List<String> lines = Files.readAllLines(Path.of("src", "test", "resources", "lab01", "input.csv"), StandardCharsets.UTF_8);
        String expected = Files.readString(Path.of("src", "test", "resources", "lab01", "report.txt"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertEquals(expected, Main.buildReport(lines).replace("\r\n", "\n"));
        String analytics = Main.buildAnalyticsReport(Files.readAllLines(Path.of("data", "analytics.csv"), StandardCharsets.UTF_8));
        assertTrue(analytics.contains("Кількість: 6"));
        assertTrue(analytics.contains("Сума: 310.00"));
    }

    @Test
    void standaloneExportUsesDefaultPaths() throws IOException, InterruptedException
    {
        Path data = Files.createDirectories(directory.resolve("data"));
        Files.copy(Path.of("data", "storage.csv"), data.resolve("storage.csv"));
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        String java = Path.of(System.getProperty("java.home"), "bin", executable).toString();
        String classpath = Path.of("target", "classes").toAbsolutePath().toString();
        Process process = new ProcessBuilder(java, "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-cp", classpath, "ua.lpnu.kzp.Main", "--export")
                .directory(directory.toFile())
                .start();
        try
        {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Java-процес не завершився вчасно");
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), stderr);
            assertEquals(CONTROL_SUMMARY, stdout);
            assertTrue(Files.exists(directory.resolve("out").resolve("readings.csv")));
            assertFalse(Files.exists(directory.resolve("out").resolve("report.txt")));
        }
        finally
        {
            if (process.isAlive())
            {
                process.destroyForcibly();
            }
        }
    }
}
