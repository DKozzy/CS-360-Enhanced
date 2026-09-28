package com.example.weighttrackingapp_weighpoint_roger_fisher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Performs statistical calculations on collections of weight entries.
 *
 * Keeping weight analysis separate from Android activities and database
 * operations allows the algorithms to be reused and tested independently
 * from the user interface.
 */
public class WeightAnalytics {

    /**
     * Describes the overall direction of a user's recent
     * weight measurements.
     */
    public enum WeightTrend {
        LOSING,
        GAINING,
        STABLE,
        INSUFFICIENT_DATA
    }

    /**
     * Returns the minimum recorded weight.
     *
     * @param entries weight entries to analyze
     * @return minimum weight, or null when no entries exist
     */
    public static Double getMinimumWeight(List<WeightEntry> entries) {

        if (entries == null || entries.isEmpty()) {
            return null;
        }

        double minimum = entries.get(0).getWeight();

        for (WeightEntry entry : entries) {
            minimum = Math.min(minimum, entry.getWeight());
        }

        return minimum;
    }

    /**
     * Returns the maximum recorded weight.
     *
     * @param entries weight entries to analyze
     * @return maximum weight, or null when no entries exist
     */
    public static Double getMaximumWeight(List<WeightEntry> entries) {

        if (entries == null || entries.isEmpty()) {
            return null;
        }

        double maximum = entries.get(0).getWeight();

        for (WeightEntry entry : entries) {
            maximum = Math.max(maximum, entry.getWeight());
        }

        return maximum;
    }

    /**
     * Calculates the average weight across all supplied entries.
     *
     * @param entries weight entries to analyze
     * @return average weight, or null when no entries exist
     */
    public static Double getAverageWeight(List<WeightEntry> entries) {

        if (entries == null || entries.isEmpty()) {
            return null;
        }

        double total = 0.0;

        for (WeightEntry entry : entries) {
            total += entry.getWeight();
        }

        return total / entries.size();
    }

    /**
     * Calculates rolling averages across chronologically ordered
     * weight entries using the requested window size.
     *
     * A sliding-window approach avoids recalculating the entire
     * window for every result.
     *
     * @param entries weight entries to analyze
     * @param windowSize number of consecutive entries in each average
     * @return rolling averages in chronological order
     */
    public static List<Double> calculateMovingAverage(
            List<WeightEntry> entries,
            int windowSize) {

        List<Double> averages = new ArrayList<>();

        if (entries == null ||
                entries.isEmpty() ||
                windowSize <= 0 ||
                entries.size() < windowSize) {

            return averages;
        }

        // Work chronologically without modifying the original list.
        List<WeightEntry> sortedEntries =
                sortByDateAscending(entries);

        double windowTotal = 0.0;

        // Build the first window.
        for (int i = 0; i < windowSize; i++) {
            windowTotal += sortedEntries.get(i).getWeight();
        }

        averages.add(windowTotal / windowSize);

        /*
         * Slide the window forward one entry at a time.
         * Remove the value leaving the window and add
         * the new value entering it.
         */
        for (int i = windowSize;
             i < sortedEntries.size();
             i++) {

            windowTotal -=
                    sortedEntries
                            .get(i - windowSize)
                            .getWeight();

            windowTotal +=
                    sortedEntries
                            .get(i)
                            .getWeight();

            averages.add(windowTotal / windowSize);
        }

        return averages;
    }

    /**
     * Calculates the total weight change between the oldest
     * and newest entries in the supplied list.
     *
     * A negative result represents weight loss while a positive
     * result represents weight gain.
     *
     * @param entries weight entries ordered newest to oldest
     * @return total weight change, or null if fewer than two entries exist
     */
    public static Double getTotalWeightChange(List<WeightEntry> entries) {

        if (entries == null || entries.size() < 2) {
            return null;
        }

        WeightEntry newestEntry = entries.get(0);
        WeightEntry oldestEntry = entries.get(entries.size() - 1);

        return newestEntry.getWeight() - oldestEntry.getWeight();
    }

    /**
     * Returns a copy of the supplied entries sorted by date
     * from oldest to newest.
     *
     * The original collection is not modified.
     *
     * @param entries weight entries to sort
     * @return sorted list, or an empty list when no entries exist
     */
    public static List<WeightEntry> sortByDateAscending(
            List<WeightEntry> entries) {

        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }

        List<WeightEntry> sortedEntries =
                new ArrayList<>(entries);

        sortedEntries.sort(
                Comparator.comparing(WeightEntry::getDate)
        );

        return sortedEntries;
    }

    /**
     * Returns a copy of the supplied entries sorted by weight
     * from lowest to highest.
     *
     * The original collection is not modified.
     *
     * @param entries weight entries to sort
     * @return sorted list, or an empty list when no entries exist
     */
    public static List<WeightEntry> sortByWeightAscending(
            List<WeightEntry> entries) {

        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }

        List<WeightEntry> sortedEntries =
                new ArrayList<>(entries);

        sortedEntries.sort(
                Comparator.comparingDouble(WeightEntry::getWeight)
        );

        return sortedEntries;
    }

    /**
     * Returns entries recorded within a specified number of days
     * ending on the supplied reference date.
     *
     * A new list is returned so the original collection remains
     * unchanged. Entries with invalid dates are skipped.
     *
     * @param entries weight entries to filter
     * @param days number of days to include
     * @param referenceDate final date of the filtering period
     * @return entries that fall within the requested date range
     */
    public static List<WeightEntry> filterByDays(
            List<WeightEntry> entries,
            int days,
            Date referenceDate) {

        List<WeightEntry> filteredEntries = new ArrayList<>();

        if (entries == null ||
                entries.isEmpty() ||
                days <= 0 ||
                referenceDate == null) {

            return filteredEntries;
        }

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        dateFormat.setLenient(false);

        /*
         * Normalize the reference date to midnight so filtering
         * compares calendar dates rather than times of day.
         */
        Calendar referenceCalendar = Calendar.getInstance();
        referenceCalendar.setTime(referenceDate);
        referenceCalendar.set(Calendar.HOUR_OF_DAY, 0);
        referenceCalendar.set(Calendar.MINUTE, 0);
        referenceCalendar.set(Calendar.SECOND, 0);
        referenceCalendar.set(Calendar.MILLISECOND, 0);

        Date normalizedReferenceDate =
                referenceCalendar.getTime();

        Calendar startCalendar =
                (Calendar) referenceCalendar.clone();

        startCalendar.add(
                Calendar.DAY_OF_YEAR,
                -(days - 1)
        );

        Date startDate =
                startCalendar.getTime();

        for (WeightEntry entry : entries) {

            try {

                Date entryDate =
                        dateFormat.parse(entry.getDate());

                if (entryDate != null &&
                        !entryDate.before(startDate) &&
                        !entryDate.after(normalizedReferenceDate)) {

                    filteredEntries.add(entry);
                }

            } catch (ParseException exception) {
                // Invalid stored dates are excluded from analysis.
            }
        }

        return filteredEntries;
    }

    /**
     * Determines the overall weight trend by comparing the
     * average weight of the older half of the entries with
     * the average weight of the newer half.
     *
     * A one-pound tolerance is used so small normal
     * fluctuations are classified as stable.
     *
     * @param entries weight entries to analyze
     * @return the calculated weight trend
     */
    public static WeightTrend determineTrend(
            List<WeightEntry> entries) {

        if (entries == null || entries.size() < 2) {
            return WeightTrend.INSUFFICIENT_DATA;
        }

        List<WeightEntry> sortedEntries =
                sortByDateAscending(entries);

        int midpoint = sortedEntries.size() / 2;

        double olderTotal = 0.0;
        double newerTotal = 0.0;

        for (int i = 0; i < midpoint; i++) {
            olderTotal += sortedEntries.get(i).getWeight();
        }

        for (int i = midpoint;
             i < sortedEntries.size();
             i++) {

            newerTotal += sortedEntries.get(i).getWeight();
        }

        double olderAverage =
                olderTotal / midpoint;

        double newerAverage =
                newerTotal /
                        (sortedEntries.size() - midpoint);

        double difference =
                newerAverage - olderAverage;

        if (difference < -1.0) {
            return WeightTrend.LOSING;
        }

        if (difference > 1.0) {
            return WeightTrend.GAINING;
        }

        return WeightTrend.STABLE;
    }
}