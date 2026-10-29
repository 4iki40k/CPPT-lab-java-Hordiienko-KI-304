package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Перевіряє чисті Stream-запити та еквівалентність цикловому еталону. */
class UtilityQueriesTest {
    private static List<UtilityReading> sampleReadings() {
        return List.of(
                UtilityReading.fromCsv("Електроенергія;2026-09-03;100;4"),
                UtilityReading.fromCsv("Вода;2026-09-02;60;30"),
                UtilityReading.fromCsv("Електроенергія;2026-09-01;60;4"),
                UtilityReading.fromCsv("Газ;2026-09-04;50;8"),
                UtilityReading.fromCsv("Вода;2026-09-05;30;30"),
                UtilityReading.fromCsv("Газ;2026-09-06;10;8"));
    }

    @Test
    void selectsStrictlyAboveFiftyAndPreservesEncounterOrder() {
        List<UtilityReading> readings = sampleReadings();
        assertEquals(List.of(readings.get(0), readings.get(1), readings.get(2)),
                UtilityQueries.aboveThreshold(readings, 50));
    }

    @Test
    void excludesExactlyEqualThreshold() {
        UtilityReading reading = new UtilityReading("Газ", "2026-09-01", 50, 4);
        assertEquals(List.of(), UtilityQueries.aboveThreshold(List.of(reading), 50));
    }

    @Test
    void zeroThresholdExcludesZeroConsumption() {
        UtilityReading zero = new UtilityReading("Газ", "2026-09-01", 0, 4);
        UtilityReading positive = new UtilityReading("Газ", "2026-09-02", 1, 4);
        assertEquals(List.of(positive), UtilityQueries.aboveThreshold(List.of(zero, positive), 0));
    }

    @Test
    void allowsNegativeFiniteThreshold() {
        List<UtilityReading> readings = sampleReadings();
        assertEquals(readings, UtilityQueries.aboveThreshold(readings, -1));
    }

    @ParameterizedTest
    @ValueSource(doubles = {100.0, 101.0, Double.MAX_VALUE})
    void thresholdAtOrAboveLargestValueProducesEmptyResult(double threshold) {
        assertEquals(List.of(), UtilityQueries.aboveThreshold(sampleReadings(), threshold));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsNonFiniteThreshold(double threshold) {
        assertThrows(IllegalArgumentException.class,
                () -> UtilityQueries.aboveThreshold(sampleReadings(), threshold));
    }

    @Test
    void mapsEveryConsumptionWithoutSortingOrDeduplicating() {
        assertEquals(List.of(100.0, 60.0, 60.0, 50.0, 30.0, 10.0),
                UtilityQueries.consumptions(sampleReadings()));
    }

    @Test
    void keepsRepeatedRecordsAndValues() {
        UtilityReading reading = new WaterReading("Вода", "2026-09-01", 60, 4);
        List<UtilityReading> source = List.of(reading, reading);
        List<UtilityReading> filtered = UtilityQueries.aboveThreshold(source, 50);
        assertEquals(2, filtered.size());
        assertSame(reading, filtered.get(0));
        assertSame(reading, filtered.get(1));
        assertEquals(List.of(60.0, 60.0), UtilityQueries.consumptions(source));
    }

    @Test
    void handlesEmptyInput() {
        assertEquals(List.of(), UtilityQueries.aboveThreshold(List.of(), 50));
        assertEquals(List.of(), UtilityQueries.consumptions(List.of()));
    }

    @Test
    void rejectsNullLists() {
        assertThrows(NullPointerException.class, () -> UtilityQueries.aboveThreshold(null, 50));
        assertThrows(NullPointerException.class, () -> UtilityQueries.consumptions(null));
        assertThrows(NullPointerException.class, () -> UtilityQueries.consumptionByMeter(null));
        assertThrows(NullPointerException.class, () -> UtilityQueries.consumptionStatistics(null));
        assertThrows(NullPointerException.class, () -> UtilityQueries.topByConsumption(null, 5));
        assertThrows(NullPointerException.class, () -> UtilityQueries.findByMeter(null, "Вода"));
    }

    @Test
    void doesNotMutateInputOrderOrMembership() {
        List<UtilityReading> source = new ArrayList<>(sampleReadings());
        List<UtilityReading> before = List.copyOf(source);
        UtilityQueries.aboveThreshold(source, 50);
        UtilityQueries.consumptions(source);
        UtilityQueries.consumptionByMeter(source);
        UtilityQueries.consumptionStatistics(source);
        UtilityQueries.topByConsumption(source, 5);
        UtilityQueries.findByMeter(source, "Вода");
        assertEquals(before, source);
        for (int index = 0; index < before.size(); index++) {
            assertSame(before.get(index), source.get(index));
        }
    }

    @Test
    void returnsUnmodifiableResultLists() {
        List<UtilityReading> source = sampleReadings();
        assertThrows(UnsupportedOperationException.class,
                () -> UtilityQueries.aboveThreshold(source, 50).add(source.get(0)));
        assertThrows(UnsupportedOperationException.class,
                () -> UtilityQueries.consumptions(source).add(1.0));
        assertThrows(UnsupportedOperationException.class,
                () -> UtilityQueries.topByConsumption(source, 5).add(source.get(0)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, 0.0, 50.0, 60.0, 100.0})
    void matchesIndependentLoopResults(double threshold) {
        List<UtilityReading> source = sampleReadings();
        List<UtilityReading> expectedFiltered = new ArrayList<>();
        List<Double> expectedValues = new ArrayList<>();
        for (UtilityReading reading : source) {
            if (reading.getValue() > threshold) {
                expectedFiltered.add(reading);
            }
            expectedValues.add(reading.getValue());
        }
        assertEquals(expectedFiltered, UtilityQueries.aboveThreshold(source, threshold));
        assertEquals(expectedValues, UtilityQueries.consumptions(source));
    }

    @Test
    void groupsConsumptionInsteadOfCostAndOrdersKeys() {
        Map<String, Double> grouped = UtilityQueries.consumptionByMeter(sampleReadings());
        assertEquals(Map.of("Електроенергія", 160.0, "Вода", 90.0, "Газ", 60.0), grouped);
        assertEquals(List.of("Вода", "Газ", "Електроенергія"), new ArrayList<>(grouped.keySet()));
    }

    @Test
    void groupingCountsEveryOccurrenceOfRepeatedRecord() {
        UtilityReading reading = new WaterReading("Вода", "2026-09-01", 60, 4);
        assertEquals(Map.of("Вода", 120.0),
                UtilityQueries.consumptionByMeter(List.of(reading, reading)));
    }

    @Test
    void groupsByExactMeterNameRatherThanCategoryOrNormalizedCase() {
        List<UtilityReading> readings = List.of(
                UtilityReading.fromCsv("Вода;2026-09-01;10;4"),
                UtilityReading.fromCsv("ВОДА;2026-09-01;20;4"),
                UtilityReading.fromCsv("Гаряча вода;2026-09-01;30;4"));
        assertEquals(Map.of("Вода", 10.0, "ВОДА", 20.0, "Гаряча вода", 30.0),
                UtilityQueries.consumptionByMeter(readings));
    }

    @Test
    void preservesGroupWithZeroConsumption() {
        UtilityReading reading = new WaterReading("Вода", "2026-09-01", 0, 4);
        assertEquals(Map.of("Вода", 0.0), UtilityQueries.consumptionByMeter(List.of(reading)));
    }

    @Test
    void calculatesAllConsumptionStatistics() {
        DoubleSummaryStatistics statistics = UtilityQueries.consumptionStatistics(sampleReadings());
        assertEquals(6L, statistics.getCount());
        assertEquals(310.0, statistics.getSum(), 0.000001);
        assertEquals(310.0 / 6, statistics.getAverage(), 0.000001);
        assertEquals(10.0, statistics.getMin(), 0.000001);
        assertEquals(100.0, statistics.getMax(), 0.000001);
    }

    @Test
    void documentsNativeEmptyStatisticsAndEmptyGrouping() {
        assertEquals(Map.of(), UtilityQueries.consumptionByMeter(List.of()));
        DoubleSummaryStatistics statistics = UtilityQueries.consumptionStatistics(List.of());
        assertEquals(0L, statistics.getCount());
        assertEquals(0.0, statistics.getSum(), 0.000001);
        assertEquals(0.0, statistics.getAverage(), 0.000001);
        assertEquals(Double.POSITIVE_INFINITY, statistics.getMin());
        assertEquals(Double.NEGATIVE_INFINITY, statistics.getMax());
    }

    @Test
    void distinguishesZeroStatisticsFromEmptyInput() {
        UtilityReading reading = new UtilityReading("Газ", "2026-09-01", 0, 4);
        DoubleSummaryStatistics statistics = UtilityQueries.consumptionStatistics(List.of(reading));
        assertEquals(1L, statistics.getCount());
        assertEquals(0.0, statistics.getSum(), 0.000001);
        assertEquals(0.0, statistics.getMin(), 0.000001);
        assertEquals(0.0, statistics.getMax(), 0.000001);
    }

    @Test
    void groupingAndStatisticsMatchIndependentLoop() {
        List<UtilityReading> source = sampleReadings();
        Map<String, Double> expectedGroups = new TreeMap<>();
        double sum = 0.0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        long count = 0;
        for (UtilityReading reading : source) {
            expectedGroups.merge(reading.getMeter(), reading.getValue(), Double::sum);
            sum += reading.getValue();
            min = Math.min(min, reading.getValue());
            max = Math.max(max, reading.getValue());
            count++;
        }
        assertEquals(expectedGroups, UtilityQueries.consumptionByMeter(source));
        DoubleSummaryStatistics actual = UtilityQueries.consumptionStatistics(source);
        assertEquals(count, actual.getCount());
        assertEquals(sum, actual.getSum(), 0.000001);
        assertEquals(sum / count, actual.getAverage(), 0.000001);
        assertEquals(min, actual.getMin(), 0.000001);
        assertEquals(max, actual.getMax(), 0.000001);
    }

    @Test
    void aggregatesFractionalConsumptionWithinFloatingPointTolerance() {
        List<UtilityReading> source = List.of(
                new UtilityReading("Газ", "2026-09-01", 0.1, 4),
                new UtilityReading("Газ", "2026-09-02", 0.2, 4),
                new UtilityReading("Газ", "2026-09-03", 0.3, 4));
        double expected = 0.0;
        for (UtilityReading reading : source) {
            expected += reading.getValue();
        }
        assertEquals(expected, UtilityQueries.consumptionByMeter(source).get("Газ"), 0.000001);
        assertEquals(expected, UtilityQueries.consumptionStatistics(source).getSum(), 0.000001);
    }

    @Test
    void modifyingNewMapDoesNotChangeSourceRecords() {
        List<UtilityReading> source = sampleReadings();
        Map<String, Double> grouped = UtilityQueries.consumptionByMeter(source);
        grouped.put("Вода", 999.0);
        assertEquals(60.0, source.get(1).getValue(), 0.000001);
        assertEquals(90.0, UtilityQueries.consumptionByMeter(source).get("Вода"), 0.000001);
    }

    @Test
    void everyStatisticsQueryReturnsIndependentResult() {
        List<UtilityReading> source = sampleReadings();
        DoubleSummaryStatistics statistics = UtilityQueries.consumptionStatistics(source);
        statistics.accept(999);
        assertEquals(7L, statistics.getCount());
        assertEquals(6L, UtilityQueries.consumptionStatistics(source).getCount());
        assertEquals(6, source.size());
    }

    @Test
    void topFiveUsesDescendingConsumptionAndAscendingDate() {
        List<UtilityReading> source = sampleReadings();
        assertEquals(List.of(source.get(0), source.get(2), source.get(1), source.get(3), source.get(4)),
                UtilityQueries.topByConsumption(source, 5));
    }

    @Test
    void topConsidersWholeSourceBeforeApplyingLimit() {
        List<UtilityReading> source = sampleReadings();
        List<UtilityReading> lowThenHigh = List.of(source.get(5), source.get(0));
        assertEquals(List.of(source.get(0)), UtilityQueries.topByConsumption(lowThenHigh, 1));
    }

    @Test
    void topKeepsOriginalOrderWhenBothSortKeysMatch() {
        UtilityReading first = new WaterReading("Вода", "2026-09-01", 60, 4);
        UtilityReading second = new ElectricityReading("Електроенергія", "2026-09-01", 60, 4);
        List<UtilityReading> result = UtilityQueries.topByConsumption(List.of(first, second, first), 5);
        assertEquals(3, result.size());
        assertSame(first, result.get(0));
        assertSame(second, result.get(1));
        assertSame(first, result.get(2));
    }

    @Test
    void topComparesCalendarDatesAcrossMonthsAndYears() {
        UtilityReading later = new UtilityReading("Газ", "2026-12-01", 60, 4);
        UtilityReading earlier = new UtilityReading("Газ", "2025-12-31", 60, 4);
        UtilityReading middle = new UtilityReading("Газ", "2026-01-01", 60, 4);
        assertEquals(List.of(earlier, middle, later),
                UtilityQueries.topByConsumption(List.of(later, earlier, middle), 5));
    }

    @Test
    void topReturnsEmptyResultForZeroLimitOrEmptySource() {
        assertEquals(List.of(), UtilityQueries.topByConsumption(sampleReadings(), 0));
        assertEquals(List.of(), UtilityQueries.topByConsumption(List.of(), 5));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void topRejectsNegativeLimit(int limit) {
        assertThrows(IllegalArgumentException.class,
                () -> UtilityQueries.topByConsumption(sampleReadings(), limit));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 6, Integer.MAX_VALUE})
    void topMatchesIndependentSelectionLoop(int limit) {
        List<UtilityReading> remaining = new ArrayList<>(sampleReadings());
        List<UtilityReading> expected = new ArrayList<>();
        for (int count = 0; count < limit && !remaining.isEmpty(); count++) {
            int bestIndex = 0;
            for (int index = 1; index < remaining.size(); index++) {
                UtilityReading candidate = remaining.get(index);
                UtilityReading best = remaining.get(bestIndex);
                int comparison = Double.compare(candidate.getValue(), best.getValue());
                if (comparison > 0 || (comparison == 0 && candidate.getLocalDate().isBefore(best.getLocalDate()))) {
                    bestIndex = index;
                }
            }
            expected.add(remaining.remove(bestIndex));
        }
        assertEquals(expected, UtilityQueries.topByConsumption(sampleReadings(), limit));
    }

    @Test
    void findReturnsFirstMatchingRecordRatherThanLatestDate() {
        List<UtilityReading> source = sampleReadings();
        UtilityReading found = UtilityQueries.findByMeter(source, "Вода").orElseThrow();
        assertSame(source.get(1), found);
        assertEquals("2026-09-02", found.getDate());
    }

    @Test
    void findStripsQueryButRemainsCaseSensitive() {
        List<UtilityReading> source = sampleReadings();
        assertSame(source.get(1), UtilityQueries.findByMeter(source, "  Вода  ").orElseThrow());
        assertTrue(UtilityQueries.findByMeter(source, "вода").isEmpty());
    }

    @Test
    void findReturnsEmptyOptionalForMissingKeyOrEmptyInput() {
        assertTrue(UtilityQueries.findByMeter(sampleReadings(), "Тепло").isEmpty());
        assertTrue(UtilityQueries.findByMeter(sampleReadings(), "").isEmpty());
        assertTrue(UtilityQueries.findByMeter(List.of(), "Вода").isEmpty());
    }

    @Test
    void findRejectsNullKey() {
        assertThrows(NullPointerException.class, () -> UtilityQueries.findByMeter(sampleReadings(), null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Вода", "Газ", "Тепло", "", " Вода "})
    void optionalSearchMatchesIndependentLoop(String meter) {
        List<UtilityReading> source = sampleReadings();
        Optional<UtilityReading> expected = Optional.empty();
        for (UtilityReading reading : source) {
            if (reading.getMeter().equals(meter.strip())) {
                expected = Optional.of(reading);
                break;
            }
        }
        assertEquals(expected, UtilityQueries.findByMeter(source, meter));
    }
}
