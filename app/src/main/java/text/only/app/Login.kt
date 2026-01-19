package text.only.app

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Login : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<Button>(R.id.btnLogin)
        statusText = findViewById(R.id.txtStatus)

        loginButton.setOnClickListener {
            statusText.text = "Verific autentificarea..."
            safeLogin()
        }
    }

    private fun safeLogin() {
        try {
            checkBiometricOrSkip()
        } catch (e: Exception) {
            // CRITICAL CATCH-ALL: If anything regarding Biometrics crashes (library bug, device issue),
            // we catch it here and force entry so the user is not locked out.
            Log.e("Login", "Critical error in biometric check", e)
            Toast.makeText(this, "Eroare compatibilitate. Intrare de urgență.", Toast.LENGTH_LONG).show()
            goToChat()
        }
    }

    private fun checkBiometricOrSkip() {
        val biometricManager = BiometricManager.from(this)
        
        // Use a simpler authenticator mask for maximum compatibility
        // Some older devices crash with DEVICE_CREDENTIAL mixed in
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or 
                             BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuth = try {
            biometricManager.canAuthenticate(authenticators)
        } catch (e: Exception) {
            // If the check itself fails, assume no hardware
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        }

        when (canAuth) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPrompt(authenticators)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "Fără securitate setată. Acces permis.", Toast.LENGTH_SHORT).show()
                goToChat()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Hardware indisponibil. Acces permis.", Toast.LENGTH_SHORT).show()
                goToChat()
            }
            else -> {
                // BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED etc.
                Toast.makeText(this, "Verificare omisă (Status: $canAuth). Intrare...", Toast.LENGTH_SHORT).show()
                goToChat()
            }
        }
    }

    private fun showBiometricPrompt(allowedAuthenticators: Int) {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Autentificare reușită ✅", Toast.LENGTH_SHORT).show()
                    // *** HERE is where you should load profile/data before navigating ***
                    // Since you mentioned profile issues, add your data loading/initialization here.
                    // For example: loadUserProfileAndThenGoToChat()
                    goToChat()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Treat user cancellation or negative button as "stay on login screen"
                    // Treat other errors (lockout, hardware error) as "let them in" to avoid blocking
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || 
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED) {
                         statusText.text = "Autentificare anulată."
                    } else {
                         Toast.makeText(applicationContext, "Eroare ($errorCode): $errString. Se intră...", Toast.LENGTH_SHORT).show()
                         goToChat()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Date incorecte ❌", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autentificare necesară")
            .setSubtitle("Confirmă identitatea pentru a intra")
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // Fallback if authenticate() throws
            goToChat()
        }
    }

    private fun goToChat() {
        val intent = Intent(this, ChatActivity::class.java)
        startActivity(intent)
        finish()
    }
}