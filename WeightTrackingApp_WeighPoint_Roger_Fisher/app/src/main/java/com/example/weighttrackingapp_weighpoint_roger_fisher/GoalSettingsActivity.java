package com.example.weighttrackingapp_weighpoint_roger_fisher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * Manages goal-weight settings, displays the user's current
 * progress, and saves goal information to SQLite.
 */
public class GoalSettingsActivity extends AppCompatActivity {

    private EditText editTextCurrentWeight;
    private EditText editTextGoalWeight;
    private ProgressBar progressGoal;
    private TextView textProgressPercent;
    private TextView textPoundsRemaining;

    private DatabaseHelper databaseHelper;

    private long userId;
    private String username;

    private Double currentWeight;
    private Double startingWeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_settings);

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

        editTextCurrentWeight =
                findViewById(R.id.editTextCurrentWeight);

        editTextGoalWeight =
                findViewById(R.id.editTextGoalWeight);

        progressGoal =
                findViewById(R.id.progressGoal);

        textProgressPercent =
                findViewById(R.id.textProgressPercent);

        textPoundsRemaining =
                findViewById(R.id.textPoundsRemaining);

        Button buttonSaveGoal =
                findViewById(R.id.buttonSaveGoal);

        ImageButton buttonHome =
                findViewById(R.id.buttonHome);

        ImageButton buttonHistory =
                findViewById(R.id.buttonHistory);

        ImageButton buttonGoals =
                findViewById(R.id.buttonGoals);

        ImageButton buttonSettings =
                findViewById(R.id.buttonSettings);

        loadGoalInformation();

        buttonSaveGoal.setOnClickListener(view ->
                saveGoal());

        buttonHome.setOnClickListener(view ->
                openActivity(DashboardActivity.class));

        buttonHistory.setOnClickListener(view ->
                openActivity(WeightHistoryActivity.class));

        buttonGoals.setOnClickListener(view ->
                loadGoalInformation());

        buttonSettings.setOnClickListener(view ->
                openActivity(SMSPermissionActivity.class));
    }

    /**
     * Loads the user's current weight, starting weight,
     * and saved goal from SQLite.
     */
    private void loadGoalInformation() {

        currentWeight =
                databaseHelper.getLatestWeight(userId);

        startingWeight =
                databaseHelper.getStartingWeight(userId);

        Double goalWeight =
                databaseHelper.getGoalWeight(userId);

        if (currentWeight != null) {

            editTextCurrentWeight.setText(
                    String.format(
                            Locale.US,
                            "%.1f",
                            currentWeight
                    )
            );

        } else {

            editTextCurrentWeight.setText("");
        }

        if (goalWeight != null) {

            editTextGoalWeight.setText(
                    String.format(
                            Locale.US,
                            "%.1f",
                            goalWeight
                    )
            );

            updateProgress(goalWeight);

        } else {

            editTextGoalWeight.setText("");
            progressGoal.setProgress(0);
            textProgressPercent.setText(R.string.progress_zero);
            textPoundsRemaining.setText("--");
        }
    }

    /**
     * Validates and saves the user's goal weight.
     */
    private void saveGoal() {

        String goalText =
                editTextGoalWeight
                        .getText()
                        .toString()
                        .trim();

        if (goalText.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please enter a goal weight.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        double goalWeight;

        try {

            goalWeight =
                    Double.parseDouble(goalText);

        } catch (NumberFormatException exception) {

            Toast.makeText(
                    this,
                    "Please enter a valid goal weight.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (goalWeight <= 0) {

            Toast.makeText(
                    this,
                    "Goal weight must be greater than zero.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        boolean saved =
                databaseHelper.saveGoalWeight(
                        userId,
                        goalWeight
                );

        if (saved) {

            Toast.makeText(
                    this,
                    "Goal weight saved.",
                    Toast.LENGTH_SHORT
            ).show();

            updateProgress(goalWeight);

        } else {

            Toast.makeText(
                    this,
                    "Unable to save goal weight.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
    /**
     * Calculates progress toward the user's goal.
     */
    private void updateProgress(double goalWeight) {

        if (currentWeight == null) {

            progressGoal.setProgress(0);
            textProgressPercent.setText(R.string.progress_zero);
            textPoundsRemaining.setText("--");
            return;
        }

        double poundsRemaining =
                Math.max(
                        0,
                        currentWeight - goalWeight
                );

        textPoundsRemaining.setText(
                String.format(
                        Locale.US,
                        "%.1f lbs",
                        poundsRemaining
                )
        );

        /*
         * Progress requires both a starting weight and a goal.
         * Example:
         *
         * Starting = 400
         * Current  = 350
         * Goal     = 225
         *
         * Lost = 50
         * Total needed = 175
         * Progress = 50 / 175
         */
        int progress = 0;

        if (startingWeight != null &&
                startingWeight > goalWeight) {

            double totalNeeded =
                    startingWeight - goalWeight;

            double progressMade =
                    startingWeight - currentWeight;

            progress =
                    (int) Math.round(
                            (progressMade / totalNeeded) * 100
                    );

            progress =
                    Math.max(
                            0,
                            Math.min(100, progress)
                    );
        }

        progressGoal.setProgress(progress);

        textProgressPercent.setText(
                String.format(
                        Locale.US,
                        "%d%% complete",
                        progress
                )
        );
    }

    /**
     * Opens another WeighPoint screen while preserving
     * the logged-in user's information.
     */
    private void openActivity(Class<?> activityClass) {

        Intent intent =
                new Intent(
                        GoalSettingsActivity.this,
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