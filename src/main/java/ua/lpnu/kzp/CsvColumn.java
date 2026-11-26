package ua.lpnu.kzp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Позначає поле, яке експортується як CSV-стовпець.
 * Анотація лише описує метадані; експорт виконує CsvExporter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvColumn
{
    /**
     * Повертає заголовок CSV-стовпця.
     *
     * @return назва стовпця у першому рядку CSV
     */
    String value();
}
