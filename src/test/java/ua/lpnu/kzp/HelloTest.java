package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Перевіряє підключення JUnit, а не поведінку предметної програми. */
class HelloTest {
    /** Підтверджує, що Surefire знаходить і запускає JUnit-тест. */
    @Test
    void checksTestInfrastructure() {
        assertEquals(4, 2 + 2);
    }
}
