package ua.lpnu.kzp;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє параметризоване сховище. */
class RepositoryTest
{
    private static Repository<UtilityReading> filled()
    {
        Repository<UtilityReading> repository = new Repository<>();
        repository.add(UtilityReading.fromCsv("Електроенергія;2026-09-01;100;4"));
        repository.add(UtilityReading.fromCsv("Вода;2026-09-01;10;30"));
        repository.add(UtilityReading.fromCsv("Газ;2026-09-02;5;8"));
        return repository;
    }

    @Test
    void newRepositoryIsEmpty()
    {
        Repository<String> repository = new Repository<>();
        assertEquals(0, repository.size());
        assertEquals(List.of(), repository.all());
    }

    @Test
    void allKeepsInsertionOrderAndDuplicates()
    {
        Repository<String> repository = new Repository<>();
        repository.add("b");
        repository.add("a");
        repository.add("b");
        assertEquals(List.of("b", "a", "b"), repository.all());
        assertEquals(3, repository.size());
    }

    @Test
    void addRejectsNull()
    {
        Repository<String> repository = new Repository<>();
        assertThrows(NullPointerException.class, () -> repository.add(null));
        assertEquals(0, repository.size());
    }

    @Test
    void snapshotIsUnmodifiable()
    {
        Repository<UtilityReading> repository = filled();
        List<UtilityReading> snapshot = repository.all();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(UtilityReading.fromCsv("Газ;2026-09-03;1;1")));
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertEquals(3, repository.size());
    }

    @Test
    void snapshotDoesNotFollowLaterAdds()
    {
        Repository<String> repository = new Repository<>();
        repository.add("перший");
        List<String> before = repository.all();
        repository.add("другий");
        assertEquals(List.of("перший"), before);
        assertEquals(List.of("перший", "другий"), repository.all());
    }

    @Test
    void findReturnsMatchesInOrder()
    {
        Repository<UtilityReading> repository = filled();
        List<UtilityReading> found = repository.find(reading -> reading.getValue() >= 10);
        assertEquals(List.of("Електроенергія", "Вода"), found.stream().map(UtilityReading::getMeter).toList());
    }

    @Test
    void findAcceptsPredicateOfSupertype()
    {
        Repository<UtilityReading> repository = filled();
        Predicate<CostCalculable> expensive = item -> item.calculateCost() > 250;
        Predicate<Object> any = item -> true;
        assertEquals(2, repository.find(expensive).size());
        assertEquals(3, repository.find(any).size());
    }

    @Test
    void findWithoutMatchesIsEmptyAndDoesNotChangeRepository()
    {
        Repository<UtilityReading> repository = filled();
        List<UtilityReading> before = repository.all();
        assertTrue(repository.find(reading -> reading.getMeter().equals("Тепло")).isEmpty());
        assertEquals(before, repository.all());
    }

    @Test
    void findResultIsUnmodifiable()
    {
        Repository<String> repository = new Repository<>();
        repository.add("x");
        List<String> found = repository.find(text -> true);
        assertThrows(UnsupportedOperationException.class, () -> found.add("y"));
    }

    @Test
    void findRejectsNullCondition()
    {
        assertThrows(NullPointerException.class, () -> filled().find(null));
    }
}
