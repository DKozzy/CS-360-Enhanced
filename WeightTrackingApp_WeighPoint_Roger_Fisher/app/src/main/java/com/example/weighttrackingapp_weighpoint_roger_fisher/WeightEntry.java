package com.example.weighttrackingapp_weighpoint_roger_fisher;

/**
 * Represents a single weight entry recorded by a WeighPoint user.
 *
 * WeightEntry provides an object-based representation of the
 * weight data stored in SQLite. These objects can be placed into
 * collections for sorting, filtering, and statistical analysis.
 */
public class WeightEntry {

    private final long entryId;
    private final long userId;
    private final double weight;
    private final String date;
    private final String notes;

    /**
     * Creates a WeightEntry containing the values stored
     * for one weight record.
     *
     * @param entryId unique database ID for the entry
     * @param userId ID of the user who owns the entry
     * @param weight recorded weight in pounds
     * @param date recorded date in yyyy-MM-dd format
     * @param notes optional notes associated with the entry
     */
    public WeightEntry(
            long entryId,
            long userId,
            double weight,
            String date,
            String notes) {

        this.entryId = entryId;
        this.userId = userId;
        this.weight = weight;
        this.date = date;
        this.notes = notes;
    }

    public long getEntryId() {
        return entryId;
    }

    public long getUserId() {
        return userId;
    }

    public double getWeight() {
        return weight;
    }

    public String getDate() {
        return date;
    }

    public String getNotes() {
        return notes;
    }
}