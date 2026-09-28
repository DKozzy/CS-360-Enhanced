package com.example.weighttrackingapp_weighpoint_roger_fisher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Manages the WeighPoint SQLite database, including
 * user accounts, weight history, goal weights, and SMS settings.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weighpoint.db";
    private static final int DATABASE_VERSION = 2;

    // Users table
    public static final String TABLE_USERS = "Users";
    public static final String USER_ID = "UserID";
    public static final String USERNAME = "Username";
    public static final String PASSWORD = "Password";

    // Daily weights table
    public static final String TABLE_WEIGHTS = "DailyWeights";
    public static final String ENTRY_ID = "EntryID";
    public static final String WEIGHT_USER_ID = "UserID";
    public static final String WEIGHT = "Weight";
    public static final String WEIGHT_DATE = "Date";
    public static final String NOTES = "Notes";

    // Goal weight table
    public static final String TABLE_GOALS = "GoalWeight";
    public static final String GOAL_ID = "GoalID";
    public static final String GOAL_USER_ID = "UserID";
    public static final String GOAL_WEIGHT = "GoalWeight";

    // SMS settings table
    public static final String TABLE_SMS_SETTINGS = "SmsSettings";
    public static final String SMS_USER_ID = "UserID";
    public static final String PHONE_NUMBER = "PhoneNumber";
    public static final String SMS_ENABLED = "SmsEnabled";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        // Allows foreign-key relationships between user data tables.
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Creates the database tables the first time
     * WeighPoint initializes its SQLite database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        String createSmsSettingsTable =
                "CREATE TABLE " + TABLE_SMS_SETTINGS + " (" +
                        SMS_USER_ID + " INTEGER PRIMARY KEY, " +
                        PHONE_NUMBER + " TEXT NOT NULL, " +
                        SMS_ENABLED + " INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(" + SMS_USER_ID + ") REFERENCES " +
                        TABLE_USERS + "(" + USER_ID + ") ON DELETE CASCADE)";

        // Stores account information used during login.
        String createUsersTable =
                "CREATE TABLE " + TABLE_USERS + " (" +
                        USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        USERNAME + " TEXT NOT NULL UNIQUE, " +
                        PASSWORD + " TEXT NOT NULL)";

        // Stores each individual weight entry belonging to a user.
        String createWeightsTable =
                "CREATE TABLE " + TABLE_WEIGHTS + " (" +
                        ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        WEIGHT_USER_ID + " INTEGER NOT NULL, " +
                        WEIGHT + " REAL NOT NULL, " +
                        WEIGHT_DATE + " TEXT NOT NULL, " +
                        NOTES + " TEXT, " +
                        "FOREIGN KEY(" + WEIGHT_USER_ID + ") REFERENCES " +
                        TABLE_USERS + "(" + USER_ID + ") ON DELETE CASCADE)";

        // Stores one goal weight for each user.
        String createGoalsTable =
                "CREATE TABLE " + TABLE_GOALS + " (" +
                        GOAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        GOAL_USER_ID + " INTEGER NOT NULL UNIQUE, " +
                        GOAL_WEIGHT + " REAL NOT NULL, " +
                        "FOREIGN KEY(" + GOAL_USER_ID + ") REFERENCES " +
                        TABLE_USERS + "(" + USER_ID + ") ON DELETE CASCADE)";

        db.execSQL(createUsersTable);
        db.execSQL(createWeightsTable);
        db.execSQL(createGoalsTable);
        db.execSQL(createSmsSettingsTable);
    }

    /**
     * Upgrades older database versions without deleting
     * existing user, weight, or goal data.
     * Version 2 adds the SMS settings table.
     */
    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        if (oldVersion < 2) {

            String createSmsSettingsTable =
                    "CREATE TABLE IF NOT EXISTS " + TABLE_SMS_SETTINGS + " (" +
                            SMS_USER_ID + " INTEGER PRIMARY KEY, " +
                            PHONE_NUMBER + " TEXT NOT NULL, " +
                            SMS_ENABLED + " INTEGER NOT NULL DEFAULT 0, " +
                            "FOREIGN KEY(" + SMS_USER_ID + ") REFERENCES " +
                            TABLE_USERS + "(" + USER_ID + ") ON DELETE CASCADE)";

            db.execSQL(createSmsSettingsTable);
        }
    }

    // -------------------------------------------------------------------------
    // USER ACCOUNT METHODS
    // -------------------------------------------------------------------------

    /**
     * Creates a new WeighPoint account.
     *
     * @return true if the account was created successfully.
     */
    public boolean createUser(String username, String password) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(USERNAME, username.trim());
        values.put(PASSWORD, hashPassword(password));

        try {
            long result = db.insertOrThrow(TABLE_USERS, null, values);
            return result != -1;
        } catch (SQLiteConstraintException exception) {
            // Username already exists.
            return false;
        }
    }

    /**
     * Validates login credentials and returns the user's ID.
     *
     * @return UserID when valid, or -1 if the login fails.
     */
    public long validateUser(String username, String password) {

        SQLiteDatabase db = getReadableDatabase();

        String selection =
                USERNAME + " = ? AND " + PASSWORD + " = ?";

        String[] selectionArgs = {
                username.trim(),
                hashPassword(password)
        };

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{USER_ID},
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        long userId = -1;

        if (cursor.moveToFirst()) {
            userId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(USER_ID)
            );
        }

        cursor.close();

        return userId;
    }

    // -------------------------------------------------------------------------
    // WEIGHT CRUD METHODS
    // -------------------------------------------------------------------------

    /**
     * CREATE: Adds a new daily weight entry.
     */
    public long addWeight(
            long userId,
            double weight,
            String date,
            String notes) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(WEIGHT_USER_ID, userId);
        values.put(WEIGHT, weight);
        values.put(WEIGHT_DATE, date);
        values.put(NOTES, notes);

        return db.insert(TABLE_WEIGHTS, null, values);
    }

    /**
     * READ: Returns all weight entries for a specific user.
     */
    public Cursor getWeightEntries(long userId) {

        SQLiteDatabase db = getReadableDatabase();

        return db.query(
                 TABLE_WEIGHTS,
                null,
                WEIGHT_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                WEIGHT_DATE + " DESC, " + ENTRY_ID + " DESC"
        );
    }

    /**
     * Returns all weight entries for a user as WeightEntry objects.
     *
     * The entries are stored in an ArrayList so they can be
     * sorted, filtered, and analyzed without depending directly
     * on an Android database Cursor.
     *
     * @param userId ID of the user whose entries should be loaded
     * @return list containing the user's weight entries
     */
    public List<WeightEntry> getWeightEntryList(long userId) {

        List<WeightEntry> entries = new ArrayList<>();

        Cursor cursor = getWeightEntries(userId);

        while (cursor.moveToNext()) {

            long entryId =
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow(ENTRY_ID)
                    );

            long weightUserId =
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow(WEIGHT_USER_ID)
                    );

            double weight =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(WEIGHT)
                    );

            String date =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(WEIGHT_DATE)
                    );

            String notes =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(NOTES)
                    );

            entries.add(
                    new WeightEntry(
                            entryId,
                            weightUserId,
                            weight,
                            date,
                            notes
                    )
            );
        }

        cursor.close();

        return entries;
    }

    /**
     * READ: Returns the three most recent entries for the dashboard.
     */
    public Cursor getRecentWeightEntries(long userId) {

        SQLiteDatabase db = getReadableDatabase();

        return db.query(
                TABLE_WEIGHTS,
                null,
                WEIGHT_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                WEIGHT_DATE + " DESC, " + ENTRY_ID + " DESC",
                "3"
        );
    }

    /**
     * READ: Returns one weight entry belonging to a specific user.
     */
    public Cursor getWeightEntry(long entryId, long userId) {

        SQLiteDatabase db = getReadableDatabase();

        return db.query(
                TABLE_WEIGHTS,
                null,
                ENTRY_ID + " = ? AND " + WEIGHT_USER_ID + " = ?",
                new String[]{
                        String.valueOf(entryId),
                        String.valueOf(userId)
                },
                null,
                null,
                null
        );
    }

    /**
     * UPDATE: Changes an existing weight record.
     */
    public boolean updateWeight(
            long entryId,
            double newWeight,
            String newDate,
            String newNotes) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(WEIGHT, newWeight);
        values.put(WEIGHT_DATE, newDate);
        values.put(NOTES, newNotes);

        int rowsUpdated = db.update(
                TABLE_WEIGHTS,
                values,
                ENTRY_ID + " = ?",
                new String[]{String.valueOf(entryId)}
        );

        return rowsUpdated > 0;
    }

    /**
     * DELETE: Removes an individual weight entry.
     */
    public boolean deleteWeight(long entryId) {

        SQLiteDatabase db = getWritableDatabase();

        int rowsDeleted = db.delete(
                TABLE_WEIGHTS,
                ENTRY_ID + " = ?",
                new String[]{String.valueOf(entryId)}
        );

        return rowsDeleted > 0;
    }

    /**
     * Gets the user's latest recorded weight.
     */
    public Double getLatestWeight(long userId) {

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_WEIGHTS,
                new String[]{WEIGHT},
                WEIGHT_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                WEIGHT_DATE + " DESC, " + ENTRY_ID + " DESC",
                "1"
        );

        Double latestWeight = null;

        if (cursor.moveToFirst()) {
            latestWeight = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(WEIGHT)
            );
        }

        cursor.close();

        return latestWeight;
    }

    /**
     * Gets the user's first recorded weight.
     * This can be used as the starting point for progress calculations.
     */
    public Double getStartingWeight(long userId) {

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_WEIGHTS,
                new String[]{WEIGHT},
                WEIGHT_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                WEIGHT_DATE + " ASC, " + ENTRY_ID + " ASC",
                "1"
        );

        Double startingWeight = null;

        if (cursor.moveToFirst()) {
            startingWeight = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(WEIGHT)
            );
        }

        cursor.close();

        return startingWeight;
    }

    // -------------------------------------------------------------------------
    // SMS SETTINGS METHODS
    // -------------------------------------------------------------------------

    /**
     * Saves or updates the user's SMS notification settings.
     * Existing settings are updated; otherwise a new row is created.
     */
    public boolean saveSmsSettings(
            long userId,
            String phoneNumber,
            boolean enabled) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(PHONE_NUMBER, phoneNumber);
        values.put(SMS_ENABLED, enabled ? 1 : 0);

        int updatedRows = db.update(
                TABLE_SMS_SETTINGS,
                values,
                SMS_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}
        );

        if (updatedRows > 0) {
            return true;
        }

        values.put(SMS_USER_ID, userId);

        long result = db.insert(
                TABLE_SMS_SETTINGS,
                null,
                values
        );

        return result != -1;
    }

    /**
     * Returns the user's saved phone number.
     */
    public String getPhoneNumber(long userId) {

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_SMS_SETTINGS,
                new String[]{PHONE_NUMBER},
                SMS_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                null
        );

        String phoneNumber = null;

        if (cursor.moveToFirst()) {
            phoneNumber = cursor.getString(
                    cursor.getColumnIndexOrThrow(PHONE_NUMBER)
            );
        }

        cursor.close();

        return phoneNumber;
    }

    /**
     * Returns whether the user enabled SMS goal alerts.
     */
    public boolean areSmsAlertsEnabled(long userId) {

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_SMS_SETTINGS,
                new String[]{SMS_ENABLED},
                SMS_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                null
        );

        boolean enabled = false;

        if (cursor.moveToFirst()) {
            enabled = cursor.getInt(
                    cursor.getColumnIndexOrThrow(SMS_ENABLED)
            ) == 1;
        }

        cursor.close();

        return enabled;
    }

    // -------------------------------------------------------------------------
    // GOAL METHODS
    // -------------------------------------------------------------------------

    /**
     * Saves or updates the user's goal weight.
     * Each user maintains a single goal record.
     */
    public boolean saveGoalWeight(long userId, double goalWeight) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(GOAL_WEIGHT, goalWeight);

        int updatedRows = db.update(
                TABLE_GOALS,
                values,
                GOAL_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}
        );

        if (updatedRows > 0) {
            return true;
        }

        values.put(GOAL_USER_ID, userId);

        long result = db.insert(
                TABLE_GOALS,
                null,
                values
        );

        return result != -1;
    }

    /**
     * Returns the user's saved goal weight.
     */
    public Double getGoalWeight(long userId) {

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_GOALS,
                new String[]{GOAL_WEIGHT},
                GOAL_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                null
        );

        Double goalWeight = null;

        if (cursor.moveToFirst()) {
            goalWeight = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(GOAL_WEIGHT)
            );
        }

        cursor.close();

        return goalWeight;
    }

    // -------------------------------------------------------------------------
    // PASSWORD UTILITY
    // -------------------------------------------------------------------------

    /**
     * Hashes passwords before storing them in SQLite instead of saving
     * the original password text directly.
     */
    private String hashPassword(String password) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] encodedHash =
                    digest.digest(
                            password.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder builder = new StringBuilder();

            for (byte currentByte : encodedHash) {
                builder.append(
                        String.format("%02x", currentByte)
                );
            }

            return builder.toString();

        } catch (NoSuchAlgorithmException exception) {

            throw new RuntimeException(
                    "Unable to hash password.",
                    exception
            );
        }
    }
}