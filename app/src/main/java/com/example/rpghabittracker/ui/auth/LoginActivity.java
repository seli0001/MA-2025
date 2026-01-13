package com.example.rpghabittracker.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
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
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton loginButton;
    private TextView registerTextView, forgotPasswordText;
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
        
        setContentView(R.layout.activity_login);
        
        firebaseRepository = new FirebaseRepository();
        
        // Check if user is already logged in AND email is verified
        FirebaseUser currentUser = firebaseRepository.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            navigateToMainActivity();
            return;
        } else if (currentUser != null && !currentUser.isEmailVerified()) {
            // User logged in but email not verified - sign out
            firebaseRepository.logoutUser();
        }
        
        initializeViews();
        setupClickListeners();
    }
    
    private void initializeViews() {
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }
    
    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> loginUser());
        
        registerTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
        
        forgotPasswordText.setOnClickListener(v -> {
            // TODO: Implement forgot password
            Toast.makeText(this, "Resetovanje lozinke - uskoro!", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loginUser() {
        // Clear previous errors
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        // Validation
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
            passwordInputLayout.setError("Lozinka mora imati najmanje 6 karaktera");
            passwordEditText.requestFocus();
            return;
        }
        
        // Show loading
        setLoading(true);
        
        // Authenticate with Firebase
        firebaseRepository.loginUser(email, password,
            user -> {
                // Check if email is verified
                if (!user.isEmailVerified()) {
                    setLoading(false);
                    showEmailNotVerifiedDialog(email);
                    return;
                }
                
                // Success - email verified
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Uspešno prijavljivanje! 🎮", Toast.LENGTH_SHORT).show();
                navigateToMainActivity();
            },
            e -> {
                // Failure
                setLoading(false);
                String errorMessage = getErrorMessage(e.getMessage());
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        );
    }
    
    private void showEmailNotVerifiedDialog(String email) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Email nije verifikovan ⚠️")
            .setMessage("Molimo verifikujte vašu email adresu pre prijavljivanja.\n\nProverite inbox za: " + email)
            .setPositiveButton("U redu", (dialog, which) -> {
                firebaseRepository.logoutUser();
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
                firebaseRepository.logoutUser();
            },
            e -> {
                Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                firebaseRepository.logoutUser();
            }
        );
    }
    
    private String getErrorMessage(String firebaseError) {
        if (firebaseError == null) return "Greška pri prijavljivanju";
        
        if (firebaseError.contains("no user record") || firebaseError.contains("user-not-found")) {
            return "Korisnik sa ovim emailom ne postoji";
        } else if (firebaseError.contains("password is invalid") || firebaseError.contains("wrong-password")) {
            return "Pogrešna lozinka";
        } else if (firebaseError.contains("badly formatted") || firebaseError.contains("invalid-email")) {
            return "Email format nije validan";
        } else if (firebaseError.contains("network")) {
            return "Nema internet konekcije";
        } else if (firebaseError.contains("too-many-requests")) {
            return "Previše pokušaja. Pokušajte kasnije.";
        }
        
        return "Greška: " + firebaseError;
    }
    
    private void setLoading(boolean isLoading) {
        loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
