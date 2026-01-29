package com.goldtea.sales.auth;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.goldtea.sales.R;

import java.util.concurrent.Executor;

public class BiometricAuthManager {
    private static final String TAG = "BiometricAuthManager";
    
    private final FragmentActivity activity;
    private final BiometricPrompt biometricPrompt;
    private final BiometricPrompt.PromptInfo promptInfo;
    
    public interface AuthCallback {
        void onAuthSuccess();
        void onAuthError(String error);
        void onAuthFailed();
    }
    
    public BiometricAuthManager(FragmentActivity activity, AuthCallback callback) {
        this.activity = activity;
        
        Executor executor = ContextCompat.getMainExecutor(activity);
        
        biometricPrompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Log.e(TAG, "Authentication error: " + errString);
                        callback.onAuthError(errString.toString());
                    }
                    
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Log.d(TAG, "Authentication succeeded");
                        callback.onAuthSuccess();
                    }
                    
                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Log.w(TAG, "Authentication failed");
                        callback.onAuthFailed();
                    }
                });
        
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.auth_biometric_title))
                .setSubtitle(activity.getString(R.string.auth_biometric_subtitle))
                .setDescription(activity.getString(R.string.auth_biometric_description))
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build();
    }
    
    /**
     * Check if biometric authentication is available
     */
    public static boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );
        
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
    }
    
    /**
     * Show biometric authentication prompt
     */
    public void authenticate() {
        biometricPrompt.authenticate(promptInfo);
    }
    
    /**
     * Cancel authentication
     */
    public void cancel() {
        biometricPrompt.cancelAuthentication();
    }
}
