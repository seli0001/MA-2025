package com.example.rpghabittracker.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.repository.FirebaseRepository;
import com.example.rpghabittracker.ui.home.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {
    
    private ImageButton backButton;
    private TextInputLayout usernameInputLayout, emailInputLayout, passwordInputLayout, confirmPasswordInputLayout;
    private TextInputEditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private MaterialButton registerButton;
    private TextView loginTextView;
    private FrameLayout loadingOverlay;
    
    private FirebaseRepository firebaseRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        
        setContentView(R.layout.activity_register);
        
        firebaseRepository = new FirebaseRepository();
        
        initializeViews();
        setupClickListeners();
    }
    
    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> onBackPressed());
        
        registerButton.setOnClickListener(v -> registerUser());
        
        loginTextView.setOnClickListener(v -> {
            finish(); // Go back to login
        });
        
        // Avatar selection (placeholder)
        findViewById(R.id.avatarContainer).setOnClickListener(v -> {
            Toast.makeText(this, "Izbor avatara - uskoro!", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void registerUser() {
        // Clear previous errors
        usernameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
        
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        
        // Validation
        if (TextUtils.isEmpty(username)) {
            usernameInputLayout.setError("Korisničko ime je obavezno");
            usernameEditText.requestFocus();
            return;
        }
        
        if (username.length() < 3) {
            usernameInputLayout.setError("Minimum 3 karaktera");
            usernameEditText.requestFocus();
            return;
        }
        
        if (username.length() > 20) {
            usernameInputLayout.setError("Maksimum 20 karaktera");
            usernameEditText.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email je obavezan");
            emailEditText.requestFocus();
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Unesite validan email");
            emailEditText.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Lozinka je obavezna");
            passwordEditText.requestFocus();
            return;
        }
        
        if (password.length() < 6) {
            passwordInputLayout.setError("Minimum 6 karaktera");
            passwordEditText.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInputLayout.setError("Potvrdite lozinku");
            confirmPasswordEditText.requestFocus();
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError("Lozinke se ne poklapaju");
            confirmPasswordEditText.requestFocus();
            return;
        }
        
        // Show loading
        setLoading(true);
        
        // Register with Firebase
        firebaseRepository.registerUser(email, password, username,
            user -> {
                // Success - but don't navigate yet, show verification message
                setLoading(false);
                showVerificationDialog(email);
            },
            e -> {
                // Failure
                setLoading(false);
                String errorMessage = getErrorMessage(e.getMessage());
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        );
    }
    
    private void showVerificationDialog(String email) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Verifikujte email ✉️")
            .setMessage("Poslali smo verifikacioni link na:\n\n" + email + "\n\nMolimo vas da kliknete na link u emailu da biste aktivirali nalog.")
            .setCancelable(false)
            .setPositiveButton("U redu", (dialog, which) -> {
                // Sign out and go back to login
                firebaseRepository.logoutUser();
                finish();
            })
            .setNeutralButton("Pošalji ponovo", (dialog, which) -> {
                resendVerificationEmail();
            })
            .show();
    }
    
    private void resendVerificationEmail() {
        firebaseRepository.resendVerificationEmail(
            aVoid -> {
                Toast.makeText(this, "Verifikacioni email je ponovo poslat!", Toast.LENGTH_SHORT).show();
                // Show dialog again
                String email = emailEditText.getText().toString().trim();
                showVerificationDialog(email);
            },
            e -> {
                Toast.makeText(this, "Greška pri slanju emaila: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        );
    }
    
    private String getErrorMessage(String firebaseError) {
        if (firebaseError == null) return "Greška pri registraciji";
        
        if (firebaseError.contains("email address is already in use") || 
            firebaseError.contains("email-already-in-use")) {
            return "Email adresa je već registrovana";
        } else if (firebaseError.contains("badly formatted") || 
                   firebaseError.contains("invalid-email")) {
            return "Email format nije validan";
        } else if (firebaseError.contains("weak-password")) {
            return "Lozinka je preslaba";
        } else if (firebaseError.contains("network")) {
            return "Nema internet konekcije";
        }
        
        return "Greška: " + firebaseError;
    }
    
    private void setLoading(boolean isLoading) {
        loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!isLoading);
        usernameEditText.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        confirmPasswordEditText.setEnabled(!isLoading);
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
