package com.example.weighttrackingapp_weighpoint_roger_fisher;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

/**
 * Displays the logged-in user's saved weight history
 * and provides options for editing or deleting records.
 */
public class WeightHistoryActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private LinearLayout historyRowsContainer;

    private Spinner spinnerDateRange;
    private Spinner spinnerSortOrder;

    private TextView textAnalyticsTrend;
    private TextView textAnalyticsAverage;
    private TextView textAnalyticsChange;
    private TextView textAnalyticsRange;
    private TextView textAnalyticsMovingAverage;

    private long userId;
    private String username;

    private final SimpleDateFormat databaseDateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("MMM d", Locale.US);

    private final SimpleDateFormat displayDateWithYearFormat =
            new SimpleDateFormat("MMM d, yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_history);

        // Receive the currently logged-in user's information.
        userId = getIntent().getLongExtra("USER_ID", -1);
        username = getIntent().getStringExtra("USERNAME");

        if (userId == -1) {
            Toast.makeText(
                    this,
                    R.string.user_identification_failed,
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        databaseHelper = new DatabaseHelper(this);

        historyRowsContainer =
                findViewById(R.id.historyRowsContainer);

        spinnerDateRange =
                findViewById(R.id.spinnerDateRange);

        spinnerSortOrder =
                findViewById(R.id.spinnerSortOrder);

        textAnalyticsTrend =
                findViewById(R.id.textAnalyticsTrend);

        textAnalyticsAverage =
                findViewById(R.id.textAnalyticsAverage);

        textAnalyticsChange =
                findViewById(R.id.textAnalyticsChange);

        textAnalyticsRange =
                findViewById(R.id.textAnalyticsRange);

        textAnalyticsMovingAverage =
                findViewById(R.id.textAnalyticsMovingAverage);

        Button buttonAddWeight =
                findViewById(R.id.buttonAddWeight);

        ImageButton buttonHome =
                findViewById(R.id.buttonHome);

        ImageButton buttonHistory =
                findViewById(R.id.buttonHistory);

        ImageButton buttonGoals =
                findViewById(R.id.buttonGoals);

        ImageButton buttonSettings =
                findViewById(R.id.buttonSettings);

        setupAnalyticsControls();

        // Opens the Add Weight screen.
        buttonAddWeight.setOnClickListener(view ->
                openActivity(AddWeightActivity.class));

        // Bottom navigation.
        buttonHome.setOnClickListener(view ->
                openActivity(DashboardActivity.class));

        buttonHistory.setOnClickListener(view ->
                loadWeightHistory());

        buttonGoals.setOnClickListener(view ->
                openActivity(GoalSettingsActivity.class));

        buttonSettings.setOnClickListener(view ->
                openActivity(SMSPermissionActivity.class));
    }

    /**
     * Refreshes the weight history whenever the activity
     * returns to the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (databaseHelper != null && userId != -1) {
            loadWeightHistory();
        }
    }

    /**
     * Loads the user's weight records into a collection,
     * applies the selected date filter and sort order,
     * and displays the resulting entries.
     */
    private void loadWeightHistory() {

        historyRowsContainer.removeAllViews();

        /*
         * Convert the SQLite records into WeightEntry objects so
         * filtering and sorting can occur independently of Cursor logic.
         */
        List<WeightEntry> entries =
                databaseHelper.getWeightEntryList(userId);

        // Apply the currently selected date-range filter.
        entries = applyDateFilter(entries);

        /*
         * Calculate summary information before display sorting.
         * This keeps analytics independent of the user's chosen
         * presentation order.
         */
        updateAnalyticsSummary(entries);

        // Apply the currently selected sorting option.
        entries = applySortOrder(entries);

        if (entries.isEmpty()) {
            showEmptyHistoryMessage();
            return;
        }

        for (WeightEntry entry : entries) {

            addHistoryRow(
                    entry.getEntryId(),
                    entry.getDate(),
                    entry.getWeight()
            );
        }
    }

    /**
     * Applies the date range currently selected by the user.
     *
     * @param entries complete collection of weight entries
     * @return entries contained within the selected date range
     */
    private List<WeightEntry> applyDateFilter(
            List<WeightEntry> entries) {

        int selectedPosition =
                spinnerDateRange.getSelectedItemPosition();

        // Position 0 represents All Time.
        if (selectedPosition == 0) {
            return entries;
        }

        int days;

        switch (selectedPosition) {

            case 1:
                days = 7;
                break;

            case 2:
                days = 30;
                break;

            case 3:
                days = 90;
                break;

            default:
                return entries;
        }

        return WeightAnalytics.filterByDays(
                entries,
                days,
                new Date()
        );
    }

    /**
     * Applies the sort order currently selected by the user.
     *
     * @param entries weight entries to organize
     * @return entries arranged in the selected order
     */
    private List<WeightEntry> applySortOrder(
            List<WeightEntry> entries) {

        int selectedPosition =
                spinnerSortOrder.getSelectedItemPosition();

        switch (selectedPosition) {

            case 1:
                // Oldest date to newest date.
                return WeightAnalytics.sortByDateAscending(entries);

            case 2:
                // Lowest recorded weight to highest.
                return WeightAnalytics.sortByWeightAscending(entries);

            case 3:
                // Reverse the ascending weight result.
                List<WeightEntry> highestFirst =
                        WeightAnalytics.sortByWeightAscending(entries);

                java.util.Collections.reverse(highestFirst);

                return highestFirst;

            case 0:
            default:
                // DatabaseHelper originally provides newest first.
                return entries;
        }
    }

    /**
     * Updates the analytics summary using the currently
     * filtered collection of weight entries.
     *
     * @param entries filtered weight entries to analyze
     */
    private void updateAnalyticsSummary(
            List<WeightEntry> entries) {

        if (entries == null || entries.isEmpty()) {

            textAnalyticsTrend.setText(
                    R.string.analytics_trend_empty
            );

            textAnalyticsAverage.setText(
                    R.string.analytics_average_empty
            );

            textAnalyticsMovingAverage.setText(
                    R.string.analytics_recent_average_empty
            );

            textAnalyticsChange.setText(
                    R.string.analytics_change_empty
            );

            textAnalyticsRange.setText(
                    R.string.analytics_range_empty
            );

            return;
        }


        Double average =
                WeightAnalytics.getAverageWeight(entries);

        Double minimum =
                WeightAnalytics.getMinimumWeight(entries);

        Double maximum =
                WeightAnalytics.getMaximumWeight(entries);

        Double totalChange =
                WeightAnalytics.getTotalWeightChange(entries);

        WeightAnalytics.WeightTrend trend =
                WeightAnalytics.determineTrend(entries);

        /*
         * Use up to the seven most recent recorded entries
         * for the recent moving-average calculation.
         */
        int movingAverageWindow =
                Math.min(7, entries.size());

        List<Double> movingAverages =
                WeightAnalytics.calculateMovingAverage(
                        entries,
                        movingAverageWindow
                );

        Double recentAverage = null;

        if (!movingAverages.isEmpty()) {
            recentAverage =
                    movingAverages.get(
                            movingAverages.size() - 1
                    );
        }

        textAnalyticsAverage.setText(
                getString(
                        R.string.analytics_average_value,
                        average
                )
        );

        if (recentAverage != null) {

            textAnalyticsMovingAverage.setText(
                    getString(
                            R.string.analytics_recent_average_value,
                            recentAverage
                    )
            );

        } else {

            textAnalyticsMovingAverage.setText(
                    R.string.analytics_recent_average_empty
            );
        }

        textAnalyticsRange.setText(
                getString(
                        R.string.analytics_range_value,
                        minimum,
                        maximum
                )
        );

        if (totalChange == null) {

            textAnalyticsChange.setText(
                    R.string.analytics_change_empty
            );

        } else {

            textAnalyticsChange.setText(
                    getString(
                            R.string.analytics_change_value,
                            totalChange
                    )
            );
        }

        textAnalyticsTrend.setText(
                getString(
                        R.string.analytics_trend_value,
                        formatTrend(trend)
                )
        );
    }

    /**
     * Converts a WeightTrend value into text suitable
     * for display in the user interface.
     */
    private String formatTrend(
            WeightAnalytics.WeightTrend trend) {

        switch (trend) {

            case LOSING:
                return getString(R.string.trend_losing);

            case GAINING:
                return getString(R.string.trend_gaining);

            case STABLE:
                return getString(R.string.trend_stable);

            case INSUFFICIENT_DATA:
            default:
                return getString(
                        R.string.trend_insufficient_data
                );
        }
    }

    /**
     * Displays a message when no weight entries match
     * the currently selected filter.
     */
    private void showEmptyHistoryMessage() {

        TextView emptyMessage = new TextView(this);

        emptyMessage.setText(R.string.no_weight_history);
        emptyMessage.setTextSize(16);
        emptyMessage.setTextColor(
                Color.parseColor("#52645D")
        );

        emptyMessage.setGravity(Gravity.CENTER);

        emptyMessage.setPadding(
                0,
                dpToPx(24),
                0,
                dpToPx(24)
        );

        historyRowsContainer.addView(emptyMessage);
    }

    /**
     * Configures the date-range and sorting controls used
     * to organize weight-history records.
     */
    private void setupAnalyticsControls() {

        String[] dateRanges = {
                getString(R.string.analytics_all_time),
                getString(R.string.analytics_last_7_days),
                getString(R.string.analytics_last_30_days),
                getString(R.string.analytics_last_90_days)
        };

        String[] sortOrders = {
                getString(R.string.analytics_newest_first),
                getString(R.string.analytics_oldest_first),
                getString(R.string.analytics_lowest_weight),
                getString(R.string.analytics_highest_weight)
        };

        ArrayAdapter<String> dateRangeAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        dateRanges
                );

        dateRangeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerDateRange.setAdapter(dateRangeAdapter);

        ArrayAdapter<String> sortOrderAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        sortOrders
                );

        sortOrderAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerSortOrder.setAdapter(sortOrderAdapter);

        AdapterView.OnItemSelectedListener selectionListener =
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        loadWeightHistory();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {

                        // Keep the current history display unchanged.
                    }
                };

        spinnerDateRange.setOnItemSelectedListener(
                selectionListener
        );

        spinnerSortOrder.setOnItemSelectedListener(
                selectionListener
        );
    }

    /**
     * Creates one visible history row containing the date,
     * weight, and delete button.
     */
    private void addHistoryRow(
            long entryId,
            String date,
            double weight) {

        LinearLayout row = new LinearLayout(this);

        row.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(56)
                )
        );

        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView dateView = new TextView(this);

        LinearLayout.LayoutParams dateParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.2f
                );

        dateView.setLayoutParams(dateParams);
        dateView.setText(formatDate(date));
        dateView.setTextSize(16);
        dateView.setTextColor(
                Color.parseColor("#1F2925")
        );

        TextView weightView = new TextView(this);

        LinearLayout.LayoutParams weightParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.2f
                );

        weightView.setLayoutParams(weightParams);
        weightView.setGravity(Gravity.CENTER);

        weightView.setText(
                getString(
                        R.string.weight_value,
                        weight
                )
        );

        weightView.setTextSize(16);
        weightView.setTextColor(
                Color.parseColor("#1F2925")
        );

        ImageButton deleteButton =
                new ImageButton(this);

        deleteButton.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dpToPx(56),
                        dpToPx(56)
                )
        );

        deleteButton.setImageResource(
                R.drawable.ic_delete
        );

        deleteButton.setColorFilter(
                Color.parseColor("#A44343")
        );

        deleteButton.setBackground(
                ContextCompat.getDrawable(
                        this,
                        android.R.drawable.list_selector_background
                )
        );

        deleteButton.setPadding(
                dpToPx(16),
                dpToPx(16),
                dpToPx(16),
                dpToPx(16)
        );

        deleteButton.setContentDescription(
                getString(
                        R.string.delete_entry_description
                )
        );

        deleteButton.setOnClickListener(view ->
                confirmDelete(entryId));

        row.addView(dateView);
        row.addView(weightView);
        row.addView(deleteButton);

        // Tapping the row opens the selected record for editing.
        row.setOnClickListener(view ->
                openEditWeight(entryId));

        historyRowsContainer.addView(row);

        // Divider between database records.
        View divider = new View(this);

        divider.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(1)
                )
        );

        divider.setBackgroundColor(
                Color.parseColor("#E5ECE8")
        );

        historyRowsContainer.addView(divider);
    }

    /**
     * Opens the Add Weight screen in edit mode for
     * the selected database record.
     */
    private void openEditWeight(long entryId) {

        Intent intent = new Intent(
                WeightHistoryActivity.this,
                AddWeightActivity.class
        );

        intent.putExtra("USER_ID", userId);
        intent.putExtra("USERNAME", username);
        intent.putExtra("ENTRY_ID", entryId);

        startActivity(intent);
    }

    /**
     * Confirms that the user wants to permanently
     * delete the selected weight record.
     */
    private void confirmDelete(long entryId) {

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_weight_title)
                .setMessage(R.string.delete_weight_message)

                .setPositiveButton(
                        R.string.delete,
                        (dialog, which) ->
                                deleteWeight(entryId)
                )

                .setNegativeButton(
                        R.string.cancel,
                        null
                )

                .show();
    }

    /**
     * Deletes the selected SQLite record and refreshes
     * the Weight History screen.
     */
    private void deleteWeight(long entryId) {

        boolean deleted =
                databaseHelper.deleteWeight(entryId);

        if (deleted) {

            Toast.makeText(
                    this,
                    R.string.weight_deleted,
                    Toast.LENGTH_SHORT
            ).show();

            loadWeightHistory();

        } else {

            Toast.makeText(
                    this,
                    R.string.weight_delete_failed,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Converts yyyy-MM-dd database dates into a user-friendly format.
     * Dates from the current year display as "Aug 10", while dates
     * from previous years include the year, such as "Aug 10, 2025".
     */
    private String formatDate(String databaseDate) {

        try {

            Date date =
                    databaseDateFormat.parse(databaseDate);

            if (date != null) {

                Calendar entryCalendar = Calendar.getInstance();
                entryCalendar.setTime(date);

                Calendar currentCalendar = Calendar.getInstance();

                if (entryCalendar.get(Calendar.YEAR)
                        == currentCalendar.get(Calendar.YEAR)) {

                    return displayDateFormat.format(date);

                } else {

                    return displayDateWithYearFormat.format(date);
                }
            }

        } catch (ParseException exception) {
            // Display the original database value if parsing fails.
        }

        return databaseDate;
    }

    /**
     * Opens another activity while preserving the
     * currently logged-in user's information.
     */
    private void openActivity(Class<?> activityClass) {

        Intent intent =
                new Intent(
                        WeightHistoryActivity.this,
                        activityClass
                );

        intent.putExtra("USER_ID", userId);
        intent.putExtra("USERNAME", username);

        startActivity(intent);
    }

    /**
     * Converts density-independent pixels to actual pixels
     * when creating UI elements programmatically.
     */
    private int dpToPx(int dp) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(dp * density);
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