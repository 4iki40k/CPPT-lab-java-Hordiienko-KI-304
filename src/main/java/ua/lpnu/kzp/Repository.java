package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Типобезпечне сховище об'єктів у пам'яті.
 *
 * @param <T> тип записів сховища
 */
public final class Repository<T>
{
    /** Записи в порядку додавання. */
    private final List<T> items = new ArrayList<>();

    /**
     * Додає непорожній запис у кінець сховища.
     *
     * @param item запис
     * @throws NullPointerException якщо запис дорівнює null
     */
    public void add(T item)
    {
        items.add(Objects.requireNonNull(item, "Запис не може бути null"));
    }

    /**
     * Повертає незмінний знімок усіх записів у порядку додавання.
     *
     * @return незалежна від сховища незмінна копія списку
     */
    public List<T> all()
    {
        return List.copyOf(items);
    }

    /**
     * Повертає записи, що задовольняють умову, у порядку додавання.
     *
     * @param condition умова для T або його супертипу
     * @return незмінний список знайдених записів
     * @throws NullPointerException якщо умова дорівнює null
     */
    public List<T> find(Predicate<? super T> condition)
    {
        Objects.requireNonNull(condition, "Умова не може бути null");
        return items.stream()
                .filter(condition)
                .toList();
    }

    /**
     * Повертає кількість записів.
     *
     * @return розмір сховища
     */
    public int size()
    {
        return items.size();
    }
}
