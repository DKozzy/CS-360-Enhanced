package com.example.weighttrackingapp_weighpoint_roger_fisher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Handles user authentication and account creation.
 * Login credentials are validated against the SQLite Users table.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText passwordInput;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Connect Java variables to the UI elements.
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);

        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonCreateAccount = findViewById(R.id.buttonCreateAccount);

        // Create access to the SQLite database.
        databaseHelper = new DatabaseHelper(this);

        // Attempts to authenticate an existing user.
        buttonLogin.setOnClickListener(view -> loginUser());

        // Creates a new user account if the username does not already exist.
        buttonCreateAccount.setOnClickListener(view -> createAccount());
    }

    /**
     * Checks the entered username and password against the Users table.
     */
    private void loginUser() {

        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please enter a username and password.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        long userId = databaseHelper.validateUser(username, password);

        if (userId != -1) {

            Toast.makeText(
                    this,
                    "Login successful.",
                    Toast.LENGTH_SHORT
            ).show();

            openDashboard(userId, username);

        } else {

            Toast.makeText(
                    this,
                    "Invalid username or password.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Creates a new user account and saves it to SQLite.
     */
    private void createAccount() {

        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please enter a username and password.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (password.length() < 4) {
            Toast.makeText(
                    this,
                    "Password must be at least 4 characters.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        boolean accountCreated =
                databaseHelper.createUser(username, password);

        if (accountCreated) {

            Toast.makeText(
                    this,
                    "Account created successfully. You can now log in.",
                    Toast.LENGTH_LONG
            ).show();

            passwordInput.setText("");

        } else {

            Toast.makeText(
                    this,
                    "That username already exists.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Opens the dashboard and passes the logged-in user's information.
     */
    private void openDashboard(long userId, String username) {

        Intent intent =
                new Intent(LoginActivity.this, DashboardActivity.class);

        intent.putExtra("USER_ID", userId);
        intent.putExtra("USERNAME", username);

        startActivity(intent);

        // Prevents returning to the login screen with the Back button.
        finish();
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