package com.example.weighttrackingapp_weighpoint_roger_fisher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

/**
 * Manages the user's SMS notification preferences,
 * runtime SEND_SMS permission, and saved phone number.
 */
public class SMSPermissionActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;

    private SwitchCompat switchGoalAlerts;
    private EditText editTextPhoneNumber;
    private TextView textPermissionStatus;

    private long userId;
    private String username;

    /**
     * Receives the user's response to the Android SMS permission dialog.
     */
    private final ActivityResultLauncher<String> smsPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {

                        if (isGranted) {

                            textPermissionStatus.setText(
                                    R.string.sms_permission_granted
                            );

                            saveSmsSettings();

                        } else {

                            textPermissionStatus.setText(
                                    R.string.sms_permission_denied
                            );

                            /*
                             * SMS remains disabled when permission is denied,
                             * but all other WeighPoint features continue working.
                             */
                            switchGoalAlerts.setChecked(false);

                            saveSmsSettings();
                        }
                    }
            );

    /**
     * Initializes the SMS settings screen, loads saved preferences,
     * and configures navigation and permission handling.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_permission);

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

        switchGoalAlerts =
                findViewById(R.id.switchGoalAlerts);

        editTextPhoneNumber =
                findViewById(R.id.editTextPhoneNumber);

        textPermissionStatus =
                findViewById(R.id.textPermissionStatus);

        Button buttonEnableSms =
                findViewById(R.id.buttonEnableSms);

        ImageButton buttonHome =
                findViewById(R.id.buttonHome);

        ImageButton buttonHistory =
                findViewById(R.id.buttonHistory);

        ImageButton buttonGoals =
                findViewById(R.id.buttonGoals);

        ImageButton buttonSettings =
                findViewById(R.id.buttonSettings);

        loadSavedSettings();
        updatePermissionStatus();

        /*
         * Save the user's phone number and alert preference.
         * Android requests SEND_SMS permission if it has not
         * already been granted.
         */
        buttonEnableSms.setOnClickListener(view ->
                enableSmsNotifications());

        // Bottom navigation.
        buttonHome.setOnClickListener(view ->
                openActivity(DashboardActivity.class));

        buttonHistory.setOnClickListener(view ->
                openActivity(WeightHistoryActivity.class));

        buttonGoals.setOnClickListener(view ->
                openActivity(GoalSettingsActivity.class));

        buttonSettings.setOnClickListener(view ->
                updatePermissionStatus());
    }

    /**
     * Loads the user's previously saved SMS settings from SQLite.
     */
    private void loadSavedSettings() {

        String phoneNumber =
                databaseHelper.getPhoneNumber(userId);

        boolean enabled =
                databaseHelper.areSmsAlertsEnabled(userId);

        if (phoneNumber != null) {
            editTextPhoneNumber.setText(phoneNumber);
        }

        switchGoalAlerts.setChecked(enabled);
    }

    /**
     * Validates the settings and requests Android SMS permission
     * if permission has not already been granted.
     */
    private void enableSmsNotifications() {

        String phoneNumber =
                editTextPhoneNumber
                        .getText()
                        .toString()
                        .trim();

        if (phoneNumber.isEmpty()) {

            Toast.makeText(
                    this,
                    R.string.enter_phone_number,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!switchGoalAlerts.isChecked()) {

            boolean saved =
                    databaseHelper.saveSmsSettings(
                            userId,
                            phoneNumber,
                            false
                    );

            if (saved) {
                Toast.makeText(
                        this,
                        R.string.sms_alerts_disabled,
                        Toast.LENGTH_SHORT
                ).show();
            }

            return;
        }

        if (hasSmsPermission()) {

            saveSmsSettings();

        } else {

            smsPermissionLauncher.launch(
                    Manifest.permission.SEND_SMS
            );
        }
    }

    /**
     * Stores the phone number and enabled/disabled status in SQLite.
     */
    private void saveSmsSettings() {

        String phoneNumber =
                editTextPhoneNumber
                        .getText()
                        .toString()
                        .trim();

        boolean enabled =
                switchGoalAlerts.isChecked()
                        && hasSmsPermission();

        boolean saved =
                databaseHelper.saveSmsSettings(
                        userId,
                        phoneNumber,
                        enabled
                );

        if (saved) {

            Toast.makeText(
                    this,
                    enabled
                            ? R.string.sms_alerts_enabled
                            : R.string.sms_alerts_disabled,
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    this,
                    R.string.sms_settings_save_failed,
                    Toast.LENGTH_SHORT
            ).show();
        }

        updatePermissionStatus();
    }

    /**
     * Returns whether Android has granted SEND_SMS permission.
     */
    private boolean hasSmsPermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Updates the permission status displayed to the user.
     */
    private void updatePermissionStatus() {

        if (hasSmsPermission()) {

            textPermissionStatus.setText(
                    R.string.sms_permission_granted
            );

        } else {

            textPermissionStatus.setText(
                    R.string.sms_permission_required
            );
        }
    }

    /**
     * Opens another WeighPoint screen while preserving
     * the logged-in user's information.
     */
    private void openActivity(Class<?> activityClass) {

        Intent intent =
                new Intent(
                        SMSPermissionActivity.this,
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