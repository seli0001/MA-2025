package com.example.rpghabittracker.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.ui.auth.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Settings screen for managing account and app preferences
 */
public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private TextView textEmail;
    private LinearLayout layoutChangePassword, layoutLogout, layoutDeleteAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        
        initViews();
        loadSettings();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        textEmail = findViewById(R.id.textEmail);
        
        layoutChangePassword = findViewById(R.id.layoutChangePassword);
        layoutLogout = findViewById(R.id.layoutLogout);
        layoutDeleteAccount = findViewById(R.id.layoutDeleteAccount);
        
        // Click listeners
        layoutChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        layoutLogout.setOnClickListener(v -> confirmLogout());
        layoutDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
    }
    
    private void loadSettings() {
        // User email
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            textEmail.setText(user.getEmail());
        }
    }
    
    private void showChangePasswordDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText editCurrentPassword = dialogView.findViewById(R.id.editCurrentPassword);
        TextInputEditText editNewPassword = dialogView.findViewById(R.id.editNewPassword);
        TextInputEditText editConfirmPassword = dialogView.findViewById(R.id.editConfirmPassword);
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Promeni lozinku")
                .setView(dialogView)
                .setPositiveButton("Promeni", (dialog, which) -> {
                    String currentPw = editCurrentPassword.getText() != null ? 
                            editCurrentPassword.getText().toString() : "";
                    String newPw = editNewPassword.getText() != null ? 
                            editNewPassword.getText().toString() : "";
                    String confirmPw = editConfirmPassword.getText() != null ? 
                            editConfirmPassword.getText().toString() : "";
                    
                    if (currentPw.isEmpty() || newPw.isEmpty()) {
                        Toast.makeText(this, "Popunite sva polja", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (!newPw.equals(confirmPw)) {
                        Toast.makeText(this, "Lozinke se ne poklapaju", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (newPw.length() < 6) {
                        Toast.makeText(this, "Nova lozinka mora imati bar 6 karaktera", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    changePassword(currentPw, newPw);
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }
    
    private void changePassword(String currentPassword, String newPassword) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Greška: Korisnik nije prijavljen", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Re-authenticate first
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Update password
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid2 -> 
                                Toast.makeText(this, "Lozinka uspešno promenjena", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e -> 
                                Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Pogrešna trenutna lozinka", Toast.LENGTH_SHORT).show()
                );
    }
    
    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Odjava")
                .setMessage("Da li ste sigurni da želite da se odjavite?")
                .setPositiveButton("Odjavi se", (dialog, which) -> logout())
                .setNegativeButton("Otkaži", null)
                .show();
    }
    
    private void logout() {
        auth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void confirmDeleteAccount() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Obriši nalog")
                .setMessage("Ova akcija je nepovratna! Svi vaši podaci, uključujući zadatke, dostignuća i napredak, biće trajno obrisani.\n\nDa li ste sigurni?")
                .setPositiveButton("Obriši nalog", (dialog, which) -> showDeleteConfirmation())
                .setNegativeButton("Otkaži", null)
                .show();
    }
    
    private void showDeleteConfirmation() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);
        TextInputEditText editPassword = dialogView.findViewById(R.id.editPassword);
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Potvrdi brisanje")
                .setMessage("Unesite lozinku da biste potvrdili brisanje naloga.")
                .setView(dialogView)
                .setPositiveButton("Obriši", (dialog, which) -> {
                    String password = editPassword.getText() != null ? 
                            editPassword.getText().toString() : "";
                    
                    if (password.isEmpty()) {
                        Toast.makeText(this, "Unesite lozinku", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    deleteAccount(password);
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }
    
    private void deleteAccount(String password) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Greška: Korisnik nije prijavljen", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = user.getUid();
        
        // Re-authenticate
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Delete user data from Firestore first
                    deleteUserData(userId, () -> {
                        // Then delete authentication account
                        user.delete()
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "Nalog je obrisan", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> 
                                    Toast.makeText(this, "Greška pri brisanju naloga: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    });
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Pogrešna lozinka", Toast.LENGTH_SHORT).show()
                );
    }
    
    private void deleteUserData(String userId, Runnable onComplete) {
        // Delete user tasks
        firestore.collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                    }
                    
                    // Delete user categories
                    firestore.collection("categories")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(catSnapshot -> {
                                for (com.google.firebase.firestore.DocumentSnapshot doc : catSnapshot) {
                                    doc.getReference().delete();
                                }
                                
                                // Delete user document
                                firestore.collection("users").document(userId)
                                        .delete()
                                        .addOnCompleteListener(task -> onComplete.run());
                            })
                            .addOnFailureListener(e -> onComplete.run());
                })
                .addOnFailureListener(e -> onComplete.run());
    }
}
