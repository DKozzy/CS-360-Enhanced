package com.example.weighttrackingapp_weighpoint_roger_fisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Unit tests for the WeightAnalytics algorithms.
 */
public class WeightAnalyticsTest {

    private List<WeightEntry> entries;

    /**
     * Creates sample weight data before each test.
     *
     * Entries are ordered newest to oldest to match the
     * ordering returned by DatabaseHelper.
     */
    @Before
    public void setUp() {

        entries = new ArrayList<>();

        entries.add(new WeightEntry(
                4,
                1,
                235.0,
                "2026-09-20",
                "Newest entry"
        ));

        entries.add(new WeightEntry(
                3,
                1,
                240.0,
                "2026-09-13",
                null
        ));

        entries.add(new WeightEntry(
                2,
                1,
                245.0,
                "2026-09-06",
                null
        ));

        entries.add(new WeightEntry(
                1,
                1,
                250.0,
                "2026-08-30",
                "Oldest entry"
        ));
    }

    @Test
    public void testMinimumWeight() {

        assertEquals(
                235.0,
                WeightAnalytics.getMinimumWeight(entries),
                0.001
        );
    }

    @Test
    public void testMaximumWeight() {

        assertEquals(
                250.0,
                WeightAnalytics.getMaximumWeight(entries),
                0.001
        );
    }

    @Test
    public void testAverageWeight() {

        assertEquals(
                242.5,
                WeightAnalytics.getAverageWeight(entries),
                0.001
        );
    }

    @Test
    public void testTotalWeightChange() {

        assertEquals(
                -15.0,
                WeightAnalytics.getTotalWeightChange(entries),
                0.001
        );
    }

    @Test
    public void testEmptyListReturnsNull() {

        List<WeightEntry> emptyEntries = new ArrayList<>();

        assertNull(
                WeightAnalytics.getMinimumWeight(emptyEntries)
        );

        assertNull(
                WeightAnalytics.getMaximumWeight(emptyEntries)
        );

        assertNull(
                WeightAnalytics.getAverageWeight(emptyEntries)
        );

        assertNull(
                WeightAnalytics.getTotalWeightChange(emptyEntries)
        );
    }

    @Test
    public void testNullListReturnsNull() {

        assertNull(
                WeightAnalytics.getMinimumWeight(null)
        );

        assertNull(
                WeightAnalytics.getMaximumWeight(null)
        );

        assertNull(
                WeightAnalytics.getAverageWeight(null)
        );

        assertNull(
                WeightAnalytics.getTotalWeightChange(null)
        );
    }

    @Test
    public void testSingleEntryHasNoTotalChange() {

        List<WeightEntry> singleEntry = new ArrayList<>();

        singleEntry.add(
                new WeightEntry(
                        1,
                        1,
                        250.0,
                        "2026-09-20",
                        null
                )
        );

        assertNull(
                WeightAnalytics.getTotalWeightChange(singleEntry)
        );
    }

    @Test
    public void testSortByDateAscending() {

        List<WeightEntry> sortedEntries =
                WeightAnalytics.sortByDateAscending(entries);

        assertEquals(
                "2026-08-30",
                sortedEntries.get(0).getDate()
        );

        assertEquals(
                "2026-09-20",
                sortedEntries.get(3).getDate()
        );
    }

    @Test
    public void testSortByWeightAscending() {

        List<WeightEntry> sortedEntries =
                WeightAnalytics.sortByWeightAscending(entries);

        assertEquals(
                235.0,
                sortedEntries.get(0).getWeight(),
                0.001
        );

        assertEquals(
                250.0,
                sortedEntries.get(3).getWeight(),
                0.001
        );
    }

    @Test
    public void testSortingDoesNotModifyOriginalList() {

        WeightAnalytics.sortByDateAscending(entries);
        WeightAnalytics.sortByWeightAscending(entries);

        // Original list should still begin with the newest entry.
        assertEquals(
                "2026-09-20",
                entries.get(0).getDate()
        );

        assertEquals(
                235.0,
                entries.get(0).getWeight(),
                0.001
        );
    }

    @Test
    public void testFilterBySevenDays() throws Exception {

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        Date referenceDate =
                dateFormat.parse("2026-09-20");

        List<WeightEntry> filteredEntries =
                WeightAnalytics.filterByDays(
                        entries,
                        7,
                        referenceDate
                );

        assertEquals(1, filteredEntries.size());

        assertEquals(
                "2026-09-20",
                filteredEntries.get(0).getDate()
        );
    }

    @Test
    public void testFilterByThirtyDays() throws Exception {

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        Date referenceDate =
                dateFormat.parse("2026-09-20");

        List<WeightEntry> filteredEntries =
                WeightAnalytics.filterByDays(
                        entries,
                        30,
                        referenceDate
                );

        assertEquals(4, filteredEntries.size());
    }

    @Test
    public void testFilterWithInvalidDayRange() {

        List<WeightEntry> filteredEntries =
                WeightAnalytics.filterByDays(
                        entries,
                        0,
                        new Date()
                );

        assertEquals(0, filteredEntries.size());
    }

    @Test
    public void testCalculateMovingAverage() {

        List<Double> averages =
                WeightAnalytics.calculateMovingAverage(
                        entries,
                        3
                );

        assertEquals(2, averages.size());

        assertEquals(
                245.0,
                averages.get(0),
                0.001
        );

        assertEquals(
                240.0,
                averages.get(1),
                0.001
        );
    }

    @Test
    public void testMovingAverageUsesChronologicalOrder() {

        List<Double> averages =
                WeightAnalytics.calculateMovingAverage(
                        entries,
                        2
                );

        /*
         * Oldest entries are 250 and 245 even though
         * the original list is newest-to-oldest.
         */
        assertEquals(
                247.5,
                averages.get(0),
                0.001
        );

        assertEquals(
                237.5,
                averages.get(2),
                0.001
        );
    }

    @Test
    public void testMovingAverageWindowLargerThanData() {

        List<Double> averages =
                WeightAnalytics.calculateMovingAverage(
                        entries,
                        10
                );

        assertEquals(0, averages.size());
    }

    @Test
    public void testMovingAverageInvalidWindow() {

        List<Double> averages =
                WeightAnalytics.calculateMovingAverage(
                        entries,
                        0
                );

        assertEquals(0, averages.size());
    }

    @Test
    public void testDetermineLosingTrend() {

        assertEquals(
                WeightAnalytics.WeightTrend.LOSING,
                WeightAnalytics.determineTrend(entries)
        );
    }

    @Test
    public void testDetermineGainingTrend() {

        List<WeightEntry> gainingEntries =
                new ArrayList<>();

        gainingEntries.add(new WeightEntry(
                3, 1, 250.0, "2026-09-20", null));

        gainingEntries.add(new WeightEntry(
                2, 1, 240.0, "2026-09-13", null));

        gainingEntries.add(new WeightEntry(
                1, 1, 230.0, "2026-09-06", null));

        assertEquals(
                WeightAnalytics.WeightTrend.GAINING,
                WeightAnalytics.determineTrend(gainingEntries)
        );
    }

    @Test
    public void testDetermineStableTrend() {

        List<WeightEntry> stableEntries =
                new ArrayList<>();

        stableEntries.add(new WeightEntry(
                4, 1, 240.2, "2026-09-20", null));

        stableEntries.add(new WeightEntry(
                3, 1, 239.8, "2026-09-13", null));

        stableEntries.add(new WeightEntry(
                2, 1, 240.0, "2026-09-06", null));

        stableEntries.add(new WeightEntry(
                1, 1, 240.0, "2026-08-30", null));

        assertEquals(
                WeightAnalytics.WeightTrend.STABLE,
                WeightAnalytics.determineTrend(stableEntries)
        );
    }

    @Test
    public void testTrendWithInsufficientData() {

        List<WeightEntry> singleEntry =
                new ArrayList<>();

        singleEntry.add(
                new WeightEntry(
                        1,
                        1,
                        250.0,
                        "2026-09-20",
                        null
                )
        );

        assertEquals(
                WeightAnalytics.WeightTrend.INSUFFICIENT_DATA,
                WeightAnalytics.determineTrend(singleEntry)
        );

        assertEquals(
                WeightAnalytics.WeightTrend.INSUFFICIENT_DATA,
                WeightAnalytics.determineTrend(null)
        );
    }

    @Test
    public void filterByDaysIncludesStartDateWhenReferenceHasTime() {

        List<WeightEntry> entries = new ArrayList<>();

        entries.add(new WeightEntry(
                1,
                1,
                240.0,
                "2026-09-14",
                ""
        ));

        Calendar calendar = Calendar.getInstance();
        calendar.set(
                2026,
                Calendar.SEPTEMBER,
                20,
                18,
                30,
                0
        );
        calendar.set(Calendar.MILLISECOND, 0);

        Date referenceDate = calendar.getTime();

        List<WeightEntry> result =
                WeightAnalytics.filterByDays(
                        entries,
                        7,
                        referenceDate
                );

        assertEquals(1, result.size());
    }
}