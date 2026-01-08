package text.only.app

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import android.content.Intent
import android.os.Bundle
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
            checkBiometricOrSkip()
        }
    }

    private fun checkBiometricOrSkip() {
        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // ✅ Are senzor sau PIN — afișăm dialogul biometric
                showBiometricPrompt()
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // ⚙️ Dacă nu are nicio metodă de securitate → intră direct
                Toast.makeText(this, "Fără autentificare biometrică — acces direct ✅", Toast.LENGTH_SHORT).show()
                goToChat()
            }

            else -> {
                // Orice alt caz neașteptat → acces direct
                goToChat()
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Autentificare reușită ✅", Toast.LENGTH_SHORT).show()
                    goToChat()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Dacă utilizatorul apasă “Anulează”, îl lăsăm să intre oricum
                    Toast.makeText(applicationContext, "Autentificare omisă", Toast.LENGTH_SHORT).show()
                    goToChat()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Autentificare eșuată ❌", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autentificare necesară")
            .setSubtitle("Folosește amprenta, PIN-ul sau modelul dispozitivului")
            // 🔹 Dacă nu are biometric, permite și PIN-ul (DEVICE_CREDENTIAL)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun goToChat() {
        val intent = Intent(this, ChatActivity::class.java)
        startActivity(intent)
        finish()
    }
}
