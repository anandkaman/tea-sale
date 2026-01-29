package com.goldtea.sales.ui.splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.goldtea.sales.MainActivity;
import com.goldtea.sales.R;
import com.goldtea.sales.auth.BiometricAuthManager;
import com.goldtea.sales.data.AppPreferences;
import com.goldtea.sales.data.firestore.FirestoreManager;

public class SplashActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "GoldTeaSalesPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    private BiometricAuthManager authManager;
    private AppPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved theme before super.onCreate()
        applySavedTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        prefs = new AppPreferences(this);
        
        // Initialize Firestore (replaces MongoDB)
        FirestoreManager.getInstance(this).initialize(new FirestoreManager.OnInitializeListener() {
            @Override
            public void onSuccess() {
                checkBiometricAndProceed();
            }
            
            @Override
            public void onError(String error) {
                // Proceed anyway - Firestore will work offline
                Log.w("SplashActivity", "Firestore init warning: " + error);
                checkBiometricAndProceed();
            }
        });
    }
    
    private void checkBiometricAndProceed() {
        // Check if biometric is available
        if (BiometricAuthManager.isBiometricAvailable(this)) {
            // Show biometric authentication
            new Handler(Looper.getMainLooper()).postDelayed(this::showBiometricAuth, 1000);
        } else {
            // No biometric available, proceed directly
            Toast.makeText(this, "Biometric not available, proceeding...", Toast.LENGTH_SHORT).show();
            proceedToMain();
        }
    }
    
    private void showBiometricAuth() {
        authManager = new BiometricAuthManager(this, new BiometricAuthManager.AuthCallback() {
            @Override
            public void onAuthSuccess() {
                prefs.setAuthenticated(true);
                proceedToMain();
            }
            
            @Override
            public void onAuthError(String error) {
                Toast.makeText(SplashActivity.this, 
                        getString(R.string.auth_error, error), 
                        Toast.LENGTH_LONG).show();
                finish();
            }
            
            @Override
            public void onAuthFailed() {
                Toast.makeText(SplashActivity.this, 
                        R.string.auth_failed, 
                        Toast.LENGTH_SHORT).show();
            }
        });
        
        authManager.authenticate();
    }
    
    private void proceedToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (authManager != null) {
            authManager.cancel();
        }
    }

    private void applySavedTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
