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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє аналітичний CLI та сумісність стандартного запуску. */
class AnalyticsCliTest
{
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

    private String runProcess(String... args) throws IOException, InterruptedException
    {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        String java = Path.of(System.getProperty("java.home"), "bin", executable).toString();
        String classpath = Path.of("target", "classes").toAbsolutePath().toString();
        List<String> command = new ArrayList<>(List.of(java, "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-cp", classpath, "ua.lpnu.kzp.Main"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).directory(directory.toFile()).start();
        try
        {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Java-процес не завершився вчасно");
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), stderr);
            return stdout;
        }
        finally
        {
            if (process.isAlive())
            {
                process.destroyForcibly();
            }
        }
    }

    @Test
    void standaloneAnalyticsUsesItsOwnDefaultInputAndOutput() throws IOException, InterruptedException
    {
        Path data = Files.createDirectories(directory.resolve("data"));
        Files.copy(Path.of("data", "analytics.csv"), data.resolve("analytics.csv"));
        String stdout = runProcess("--analytics");
        assertTrue(stdout.contains("Кількість: 6"));
        assertTrue(stdout.contains("Сума: 310.00"));
        assertTrue(stdout.contains("Помилок: 2"));
        assertEquals(stdout, Files.readString(directory.resolve("out").resolve("analytics-report.txt"), StandardCharsets.UTF_8));
        assertFalse(Files.exists(directory.resolve("out").resolve("report.txt")));
    }

    @Test
    void standaloneOrdinaryModeKeepsPreviousDefaultsAndReport() throws IOException, InterruptedException
    {
        Path data = Files.createDirectories(directory.resolve("data"));
        Files.copy(Path.of("data", "input.csv"), data.resolve("input.csv"));
        String stdout = runProcess();
        String expected = Files.readString(Path.of("src", "test", "resources", "lab01", "report.txt"), StandardCharsets.UTF_8);
        assertEquals(expected.replace("\r\n", "\n"), stdout.replace("\r\n", "\n"));
        assertEquals(stdout, Files.readString(directory.resolve("out").resolve("report.txt"), StandardCharsets.UTF_8));
        assertFalse(Files.exists(directory.resolve("out").resolve("analytics-report.txt")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void explicitPathsWorkBeforeAndAfterAnalyticsFlag(boolean flagFirst) throws IOException
    {
        Path input = directory.resolve("українські дані.csv");
        Path output = directory.resolve("нова тека").resolve("звіт.txt");
        Files.writeString(input, "Вода;2026-09-01;60;30\nГаз;2026-09-02;-5;8\nЕлектроенергія;2026-09-03;100;4\n", StandardCharsets.UTF_8);
        String[] args = flagFirst ? new String[] {"--analytics", "--input", input.toString(), "--output", output.toString()} : new String[] {"--output", output.toString(), "--input", input.toString(), "--analytics"};
        assertEquals(0, run(args));
        String stdout = out.toString(StandardCharsets.UTF_8);
        assertTrue(stdout.contains("Кількість: 2"));
        assertTrue(stdout.contains("Сума: 160.00"));
        assertTrue(stdout.contains("Рядок 2:"));
        assertEquals(stdout, Files.readString(output, StandardCharsets.UTF_8));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void ordinaryExplicitInputDoesNotEnableAnalytics() throws IOException
    {
        Path output = directory.resolve("report.txt");
        assertEquals(0, run("--input", Path.of("data", "analytics.csv").toString(), "--output", output.toString()));
        assertTrue(out.toString(StandardCharsets.UTF_8).startsWith("Коректних записів: 6"));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Загальна вартість: 3820.00"));
    }

    @Test
    void rejectsRepeatedAnalyticsFlag()
    {
        assertEquals(2, run("--analytics", "--analytics"));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("--analytics указано повторно"));
        assertEquals("", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void doesNotUseAnalyticsFlagAsInputPath()
    {
        assertEquals(2, run("--input", "--analytics"));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Не вказано шлях після --input"));
    }

    @Test
    void rejectsUnknownOptionInAnalyticsMode()
    {
        assertEquals(2, run("--analytics", "--unknown"));
    }

    @Test
    void reportsMissingInputWithoutCreatingReport()
    {
        Path output = directory.resolve("report.txt");
        assertEquals(1, run("--analytics", "--input", directory.resolve("missing.csv").toString(), "--output", output.toString()));
        assertFalse(Files.exists(output));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Помилка роботи з файлами"));
    }

    @Test
    void protectsInputFromBeingOverwritten() throws IOException
    {
        Path input = directory.resolve("input.csv");
        String contents = "Вода;2026-09-01;60;30";
        Files.writeString(input, contents, StandardCharsets.UTF_8);
        assertEquals(2, run("--analytics", "--input", input.toString(), "--output", input.toString()));
        assertEquals(contents, Files.readString(input, StandardCharsets.UTF_8));
    }

    @Test
    void handlesEmptyAnalyticsFile() throws IOException
    {
        Path input = directory.resolve("empty.csv");
        Files.writeString(input, "", StandardCharsets.UTF_8);
        assertEquals(0, run("--analytics", "--input", input.toString(), "--output", directory.resolve("report.txt").toString()));
        String report = out.toString(StandardCharsets.UTF_8);
        assertTrue(report.contains("Кількість: 0"));
        assertTrue(report.contains("Мінімум: немає даних"));
        assertTrue(report.contains("Помилок: 0"));
        assertFalse(report.contains("Infinity"));
    }

    @Test
    void handlesAllInvalidAnalyticsRowsWithoutFatalExit() throws IOException
    {
        Path input = directory.resolve("bad.csv");
        Files.writeString(input, "Вода;2026-02-30;10;30\nГаз;2026-09-01;-5;8\n", StandardCharsets.UTF_8);
        assertEquals(0, run("--analytics", "--input", input.toString(), "--output", directory.resolve("report.txt").toString()));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Кількість: 0"));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Помилок: 2"));
    }

    @Test
    void reportsOutputFailureWithoutClaimingSuccess() throws IOException
    {
        Path blocker = directory.resolve("file-not-directory");
        Files.writeString(blocker, "file", StandardCharsets.UTF_8);
        assertEquals(1, run("--analytics", "--input", Path.of("data", "analytics.csv").toString(), "--output", blocker.resolve("report.txt").toString()));
        assertEquals("", out.toString(StandardCharsets.UTF_8));
        assertFalse(err.toString(StandardCharsets.UTF_8).isEmpty());
    }

    @Test
    void helpDocumentsAnalyticsAndBothSetsOfDefaults()
    {
        assertEquals(0, run("--help"));
        String help = out.toString(StandardCharsets.UTF_8);
        assertTrue(help.contains("[--analytics]"));
        assertTrue(help.contains("data/input.csv"));
        assertTrue(help.contains("data/analytics.csv"));
        assertTrue(help.contains("out/report.txt"));
        assertTrue(help.contains("out/analytics-report.txt"));
    }
}
