package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;

/**
 * Розбирає CSV-документ із комою як роздільником і полями в лапках.
 * Усередині лапок кома та перенесення рядка є даними, подвоєна лапка — однією лапкою.
 */
public final class CsvParser
{
    /** Забороняє створення екземплярів статичного парсера. */
    private CsvParser()
    {
    }

    /**
     * Розбирає рівно один CSV-запис.
     *
     * @param line текст одного запису, який може містити перенесення в лапках
     * @return поля запису
     * @throws DataStorageException якщо текст містить не один запис або має хибні лапки
     */
    public static List<String> parse(String line) throws DataStorageException
    {
        List<List<String>> records = parseDocument(line);
        if (records.size() != 1)
        {
            throw new DataStorageException("Очікується один CSV-запис, отримано " + records.size());
        }
        return records.get(0);
    }

    /**
     * Розбирає весь документ на записи. CRLF, LF і CR вважаються кінцем запису поза лапками.
     * Завершальне перенесення рядка не створює порожнього запису; порожній текст дає порожній список.
     *
     * @param text увесь текст CSV-документа
     * @return незмінний список записів, кожен — незмінний список полів
     * @throws DataStorageException якщо текст null, лапки не закриті або після закритих лапок є символ
     */
    public static List<List<String>> parseDocument(String text) throws DataStorageException
    {
        if (text == null)
        {
            throw new DataStorageException("CSV-документ не може бути null");
        }

        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        // quoted — поле зараз усередині лапок; quoteClosed — лапки вже закриті,
        // і далі допускається лише кома або кінець запису.
        boolean quoted = false;
        boolean quoteClosed = false;
        int recordNumber = 1;

        for (int index = 0; index < text.length(); index++)
        {
            char symbol = text.charAt(index);
            if (quoted)
            {
                if (symbol == '"' && index + 1 < text.length() && text.charAt(index + 1) == '"')
                {
                    current.append('"');
                    index++;
                }
                else if (symbol == '"')
                {
                    quoted = false;
                    quoteClosed = true;
                }
                else
                {
                    current.append(symbol);
                }
            }
            else if (symbol == '"' && !quoteClosed && current.length() == 0)
            {
                quoted = true;
            }
            else if (symbol == ',')
            {
                fields.add(current.toString());
                current.setLength(0);
                quoteClosed = false;
            }
            else if (symbol == '\r' || symbol == '\n')
            {
                fields.add(current.toString());
                records.add(List.copyOf(fields));
                fields = new ArrayList<>();
                current.setLength(0);
                quoteClosed = false;
                recordNumber++;
                if (symbol == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n')
                {
                    index++;
                }
            }
            else if (quoteClosed)
            {
                throw new DataStorageException("CSV-запис " + recordNumber + ": символ після закритих лапок");
            }
            else if (symbol == '"')
            {
                throw new DataStorageException("CSV-запис " + recordNumber + ": лапка всередині поля без лапок");
            }
            else
            {
                current.append(symbol);
            }
        }

        if (quoted)
        {
            throw new DataStorageException("CSV-запис " + recordNumber + ": незакриті лапки");
        }

        if (!fields.isEmpty() || current.length() > 0 || quoteClosed)
        {
            fields.add(current.toString());
            records.add(List.copyOf(fields));
        }

        return List.copyOf(records);
    }
}
