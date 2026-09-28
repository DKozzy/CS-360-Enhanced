package com.example.weighttrackingapp_weighpoint_roger_fisher;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddWeightActivity extends AppCompatActivity {

    private EditText editTextWeight;
    private EditText editTextDate;
    private EditText editTextNotes;

    private TextView textAddWeightTitle;
    private TextView textAddWeightSubtitle;
    private Button buttonSaveWeight;

    private DatabaseHelper databaseHelper;

    private long userId;
    private long entryId = -1;

    private String username;

    private final SimpleDateFormat databaseDateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("MMMM d, yyyy", Locale.US);

    private final Calendar selectedDate =
            Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_weight);

        userId = getIntent().getLongExtra("USER_ID", -1);
        username = getIntent().getStringExtra("USERNAME");

        // ENTRY_ID exists only when editing an existing record.
        entryId = getIntent().getLongExtra("ENTRY_ID", -1);

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

        textAddWeightTitle =
                findViewById(R.id.textAddWeightTitle);

        textAddWeightSubtitle =
                findViewById(R.id.textAddWeightSubtitle);

        editTextWeight =
                findViewById(R.id.editTextWeight);

        editTextDate =
                findViewById(R.id.editTextDate);

        editTextNotes =
                findViewById(R.id.editTextNotes);

        buttonSaveWeight =
                findViewById(R.id.buttonSaveWeight);

        Button buttonCancel =
                findViewById(R.id.buttonCancel);

        ImageButton buttonHome =
                findViewById(R.id.buttonHome);

        ImageButton buttonHistory =
                findViewById(R.id.buttonHistory);

        ImageButton buttonGoals =
                findViewById(R.id.buttonGoals);

        ImageButton buttonSettings =
                findViewById(R.id.buttonSettings);

        /*
         * If an ENTRY_ID was supplied, load the existing
         * SQLite record and turn this screen into Edit mode.
         */
        if (entryId != -1) {
            loadExistingEntry();
        } else {
            updateDateDisplay();
        }

        editTextDate.setOnClickListener(view ->
                showDatePicker());

        buttonSaveWeight.setOnClickListener(view -> {

            if (entryId == -1) {
                saveNewWeight();
            } else {
                updateExistingWeight();
            }
        });

        buttonCancel.setOnClickListener(view ->
                openActivity(WeightHistoryActivity.class));

        buttonHome.setOnClickListener(view ->
                openActivity(DashboardActivity.class));

        buttonHistory.setOnClickListener(view ->
                openActivity(WeightHistoryActivity.class));

        buttonGoals.setOnClickListener(view ->
                openActivity(GoalSettingsActivity.class));

        buttonSettings.setOnClickListener(view ->
                openActivity(SMSPermissionActivity.class));
    }

    /**
     * Loads an existing SQLite weight entry into the form.
     */
    private void loadExistingEntry() {

        Cursor cursor =
                databaseHelper.getWeightEntry(
                        entryId,
                        userId
                );

        if (cursor.moveToFirst()) {

            double weight =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    DatabaseHelper.WEIGHT
                            )
                    );

            String date =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    DatabaseHelper.WEIGHT_DATE
                            )
                    );

            String notes =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    DatabaseHelper.NOTES
                            )
                    );

            editTextWeight.setText(
                    String.format(
                            Locale.US,
                            "%.1f",
                            weight
                    )
            );

            editTextNotes.setText(
                    notes == null ? "" : notes
            );

            try {

                Date parsedDate =
                        databaseDateFormat.parse(date);

                if (parsedDate != null) {
                    selectedDate.setTime(parsedDate);
                }

            } catch (ParseException exception) {
                // Current date remains selected if parsing fails.
            }

            updateDateDisplay();

            textAddWeightTitle.setText(R.string.edit_weight_title);

            textAddWeightSubtitle.setText(
                    R.string.edit_weight_subtitle
            );

            buttonSaveWeight.setText(R.string.update_weight_button);

        } else {

            Toast.makeText(
                    this,
                    "Weight entry could not be found.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        }

        cursor.close();
    }

    /**
     * Opens a date picker and stores the selected date
     * for the weight record.
     */
    private void showDatePicker() {

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {

                            selectedDate.set(
                                    Calendar.YEAR,
                                    year
                            );

                            selectedDate.set(
                                    Calendar.MONTH,
                                    month
                            );

                            selectedDate.set(
                                    Calendar.DAY_OF_MONTH,
                                    dayOfMonth
                            );

                            updateDateDisplay();
                        },
                        selectedDate.get(Calendar.YEAR),
                        selectedDate.get(Calendar.MONTH),
                        selectedDate.get(Calendar.DAY_OF_MONTH)
                );

        datePickerDialog.show();
    }

    /**
     * Displays the currently selected date in a
     * user-friendly format.
     */
    private void updateDateDisplay() {

        editTextDate.setText(
                displayDateFormat.format(
                        selectedDate.getTime()
                )
        );
    }

    /**
     * Creates a new SQLite weight record.
     */
    private void saveNewWeight() {

        WeightFormData data = validateForm();

        if (data == null) {
            return;
        }

        long result =
                databaseHelper.addWeight(
                        userId,
                        data.weight,
                        data.date,
                        data.notes
                );

        if (result != -1) {

            Toast.makeText(
                    this,
                    "Weight saved successfully.",
                    Toast.LENGTH_SHORT
            ).show();

            // Check whether this weight reaches the user's goal.
            checkGoalAndSendSms(data.weight);

            openActivity(WeightHistoryActivity.class);

        } else {

            Toast.makeText(
                    this,
                    "Unable to save weight.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * UPDATE: Saves changes to an existing SQLite record.
     */
    private void updateExistingWeight() {

        WeightFormData data = validateForm();

        if (data == null) {
            return;
        }

        boolean updated =
                databaseHelper.updateWeight(
                        entryId,
                        data.weight,
                        data.date,
                        data.notes
                );

        if (updated) {

            Toast.makeText(
                    this,
                    "Weight updated successfully.",
                    Toast.LENGTH_SHORT
            ).show();

            // Check whether the updated weight reaches the user's goal.
            checkGoalAndSendSms(data.weight);

            openActivity(WeightHistoryActivity.class);

        } else {

            Toast.makeText(
                    this,
                    "Unable to update weight.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Checks whether the saved weight has reached the user's goal.
     * If SMS alerts are enabled and permission has been granted,
     * a goal notification is sent to the saved phone number.
     */
    private void checkGoalAndSendSms(double currentWeight) {

        Double goalWeight =
                databaseHelper.getGoalWeight(userId);

        // The user has not established a goal yet.
        if (goalWeight == null) {
            return;
        }

        // The goal has not been reached yet.
        if (currentWeight > goalWeight) {
            return;
        }

        // The user has SMS notifications turned off.
        if (!databaseHelper.areSmsAlertsEnabled(userId)) {
            return;
        }

        // Android permission was not granted.
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String phoneNumber =
                databaseHelper.getPhoneNumber(userId);

        if (phoneNumber == null ||
                phoneNumber.trim().isEmpty()) {
            return;
        }

        String message =
                "Congratulations! You reached your WeighPoint goal weight of "
                        + String.format(
                        Locale.US,
                        "%.1f lbs.",
                        goalWeight
                );

        try {

            SmsManager smsManager =
                    getSystemService(SmsManager.class);

            smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
            );

            Toast.makeText(
                    this,
                    "Goal weight SMS notification sent.",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception exception) {

            /*
             * SMS failure must not prevent the user's weight
             * from being saved or the app from continuing.
             */
            Toast.makeText(
                    this,
                    "Weight saved, but the SMS notification could not be sent.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Validates and prepares the form data used for
     * both CREATE and UPDATE operations.
     */
    private WeightFormData validateForm() {

        String weightText =
                editTextWeight
                        .getText()
                        .toString()
                        .trim();

        String notes =
                editTextNotes
                        .getText()
                        .toString()
                        .trim();

        if (weightText.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please enter your weight.",
                    Toast.LENGTH_SHORT
            ).show();

            return null;
        }

        double weight;

        try {

            weight =
                    Double.parseDouble(weightText);

        } catch (NumberFormatException exception) {

            Toast.makeText(
                    this,
                    "Please enter a valid weight.",
                    Toast.LENGTH_SHORT
            ).show();

            return null;
        }

        if (weight <= 0) {

            Toast.makeText(
                    this,
                    "Weight must be greater than zero.",
                    Toast.LENGTH_SHORT
            ).show();

            return null;
        }

        String databaseDate =
                databaseDateFormat.format(
                        selectedDate.getTime()
                );

        return new WeightFormData(
                weight,
                databaseDate,
                notes
        );
    }

    /**
     * Small helper object that keeps validated form
     * information together.
     */
    private static class WeightFormData {

        final double weight;
        final String date;
        final String notes;

        WeightFormData(
                double weight,
                String date,
                String notes) {

            this.weight = weight;
            this.date = date;
            this.notes = notes;
        }
    }

    /**
     * Opens another WeighPoint activity while preserving
     * the logged-in user's information.
     */
    private void openActivity(Class<?> activityClass) {

        Intent intent =
                new Intent(
                        AddWeightActivity.this,
                        activityClass
                );

        intent.putExtra("USER_ID", userId);
        intent.putExtra("USERNAME", username);

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}