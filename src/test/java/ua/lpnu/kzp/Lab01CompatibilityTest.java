package ua.lpnu.kzp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Фіксує контрольний звіт v1.0.0 для перевірки сумісності лабораторної №2. */
class Lab01CompatibilityTest {
    @Test
    void preservesReportFromLab01IncludingErrorMessages() throws IOException {
        List<String> lines = readResource("/lab01/input.csv").lines().toList();
        String expected = readResource("/lab01/report.txt").replace("\r\n", "\n");
        String actual = Main.buildReport(lines).replace("\r\n", "\n");
        assertEquals(expected, actual);
    }

    private static String readResource(String name) throws IOException {
        try (InputStream input = Objects.requireNonNull(
                Lab01CompatibilityTest.class.getResourceAsStream(name), "Відсутній ресурс " + name)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
