package com.example.weighttrackingapp_weighpoint_roger_fisher;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

public class DashboardActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;

    private long userId;
    private String username;
    private TextView textCurrentWeight;
    private TextView textProgressPercent;
    private TextView textGoalWeight;

    private ProgressBar progressGoal;

    private TextView[] recentDates;
    private TextView[] recentWeights;

    private final SimpleDateFormat databaseDateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("MMM d", Locale.US);

    private final SimpleDateFormat displayDateWithYearFormat =
            new SimpleDateFormat("MMM d, yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Receive information for the currently logged-in user.
        userId = getIntent().getLongExtra("USER_ID", -1);
        username = getIntent().getStringExtra("USERNAME");

        if (userId == -1) {
            Toast.makeText(
                    this,
                    "Unable to identify the logged-in user.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        databaseHelper = new DatabaseHelper(this);

        // Connect dashboard UI elements.
        TextView textGreeting = findViewById(R.id.textGreeting);
        textCurrentWeight = findViewById(R.id.textCurrentWeight);
        textProgressPercent = findViewById(R.id.textProgressPercent);
        textGoalWeight = findViewById(R.id.textGoalWeight);
        progressGoal = findViewById(R.id.progressGoal);

        recentDates = new TextView[]{
                findViewById(R.id.textRecentDateOne),
                findViewById(R.id.textRecentDateTwo),
                findViewById(R.id.textRecentDateThree)
        };

        recentWeights = new TextView[]{
                findViewById(R.id.textRecentWeightOne),
                findViewById(R.id.textRecentWeightTwo),
                findViewById(R.id.textRecentWeightThree)
        };

        Button buttonAddWeight = findViewById(R.id.buttonAddWeight);

        ImageButton buttonHome = findViewById(R.id.buttonHome);
        ImageButton buttonHistory = findViewById(R.id.buttonHistory);
        ImageButton buttonGoals = findViewById(R.id.buttonGoals);
        ImageButton buttonSettings = findViewById(R.id.buttonSettings);

        // Display the logged-in user's name.
        if (username != null && !username.isEmpty()) {
            textGreeting.setText(
                    getString(R.string.welcome_user, username));
        } else {
            textGreeting.setText(
                    R.string.welcome_generic);
        }

        // Main Add Weight action.
        buttonAddWeight.setOnClickListener(view ->
                openActivity(AddWeightActivity.class));

        // Bottom navigation.
        buttonHome.setOnClickListener(view ->
                loadDashboardData());

        buttonHistory.setOnClickListener(view ->
                openActivity(WeightHistoryActivity.class));

        buttonGoals.setOnClickListener(view ->
                openActivity(GoalSettingsActivity.class));

        buttonSettings.setOnClickListener(view ->
                openActivity(SMSPermissionActivity.class));
    }

    /**
     * Refreshes dashboard data whenever the activity
     * returns to the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Reload whenever the Dashboard becomes visible.
        if (databaseHelper != null && userId != -1) {
            loadDashboardData();
        }
    }

    /**
     * Loads the current weight, goal, progress, and recent
     * weight entries from SQLite.
     */
    private void loadDashboardData() {

        Double currentWeight =
                databaseHelper.getLatestWeight(userId);

        Double startingWeight =
                databaseHelper.getStartingWeight(userId);

        Double goalWeight =
                databaseHelper.getGoalWeight(userId);

        if (currentWeight != null) {
            textCurrentWeight.setText(
                    getString(
                            R.string.weight_value,
                            currentWeight
                    )
            );
        } else {
            textCurrentWeight.setText(
                    R.string.no_weight_recorded);
        }

        if (goalWeight != null) {
            textGoalWeight.setText(
                    getString(
                            R.string.goal_weight_value,
                            goalWeight
                    )
            );
        } else {
            textGoalWeight.setText(
                    R.string.goal_weight_not_set);
        }

        updateProgress(
                startingWeight,
                currentWeight,
                goalWeight
        );

        loadRecentEntries();
    }

    /**
     * Calculates progress from the user's starting weight
     * toward the saved goal weight.
     */
    private void updateProgress(
            Double startingWeight,
            Double currentWeight,
            Double goalWeight) {

        if (startingWeight == null ||
                currentWeight == null ||
                goalWeight == null) {

            progressGoal.setProgress(0);
            textProgressPercent.setText(
                    getString(
                            R.string.progress_complete,
                            0
                    )
            );
            return;
        }

        double totalNeeded =
                startingWeight - goalWeight;

        double progressMade =
                startingWeight - currentWeight;

        int percentage;

        if (totalNeeded <= 0) {

            percentage =
                    currentWeight <= goalWeight ? 100 : 0;

        } else {

            percentage = (int) Math.round(
                    (progressMade / totalNeeded) * 100
            );
        }

        // Keep progress within the valid ProgressBar range.
        percentage = Math.max(0, Math.min(100, percentage));

        progressGoal.setProgress(percentage);

        textProgressPercent.setText(
                getString(
                        R.string.progress_complete,
                        percentage
                )
        );
    }

    /**
     * Displays the three most recent SQLite weight records.
     */
    private void loadRecentEntries() {

        // Clear placeholder information first.
        for (int i = 0; i < recentDates.length; i++) {
            recentDates[i].setText(
                    R.string.dash_placeholder
            );

            recentWeights[i].setText(
                    R.string.dash_placeholder
            );
        }

        Cursor cursor =
                databaseHelper.getRecentWeightEntries(userId);

        int position = 0;

        while (cursor.moveToNext() &&
                position < recentDates.length) {

            String databaseDate =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    DatabaseHelper.WEIGHT_DATE
                            )
                    );

            double weight =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    DatabaseHelper.WEIGHT
                            )
                    );

            recentDates[position].setText(
                    formatDate(databaseDate)
            );

            recentWeights[position].setText(
                    getString(
                            R.string.weight_value,
                            weight
                    )
            );

            position++;
        }

        cursor.close();
    }

    /**
     * Converts SQLite's yyyy-MM-dd date into a user-friendly format.
     * Current-year entries display without the year, while older
     * entries include the year for clarity.
     */
    private String formatDate(String databaseDate) {

        try {

            Date date =
                    databaseDateFormat.parse(databaseDate);

            if (date != null) {

                Calendar entryCalendar =
                        Calendar.getInstance();

                entryCalendar.setTime(date);

                Calendar currentCalendar =
                        Calendar.getInstance();

                if (entryCalendar.get(Calendar.YEAR)
                        == currentCalendar.get(Calendar.YEAR)) {

                    return displayDateFormat.format(date);

                } else {

                    return displayDateWithYearFormat.format(date);
                }
            }

        } catch (ParseException exception) {
            // If parsing fails, display the stored database value.
        }

        return databaseDate;
    }

    /**
     * Opens another WeighPoint screen while preserving
     * the logged-in user's information.
     */
    private void openActivity(Class<?> activityClass) {

        Intent intent =
                new Intent(
                        DashboardActivity.this,
                        activityClass
                );

        intent.putExtra("USER_ID", userId);
        intent.putExtra("USERNAME", username);

        startActivity(intent);
    }

    /**
     * Releases the database connection when the activity
     * is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}