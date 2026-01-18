package text.only.app

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest
import java.util.Calendar

class OnlyGuardActivity : AppCompatActivity() {

    private lateinit var txtGuardCode: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnScanQr: Button
    private var timer: CountDownTimer? = null

    // Mock secret. In production, this should be a shared secret key stored securely (e.g., EncryptedSharedPreferences).
    private val mockSecret = "ONLYGUARD_SECRET_KEY_USER_123" 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_only_guard)

        txtGuardCode = findViewById(R.id.txtGuardCode)
        progressBar = findViewById(R.id.progressBarGuard)
        btnScanQr = findViewById(R.id.btnScanQr)

        btnScanQr.setOnClickListener {
            // Re-use existing QR Scan activity
            startActivity(Intent(this, ScanQRActivity::class.java))
        }

        startCodeGeneration()
    }

    override fun onResume() {
        super.onResume()
        startCodeGeneration()
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    private fun startCodeGeneration() {
        timer?.cancel()

        // Sync with the 30-second window
        val currentTime = System.currentTimeMillis()
        val seconds = (currentTime / 1000) % 30
        val millisRemaining = (30 - seconds) * 1000 - (currentTime % 1000)

        updateCode()

        // Timer to update progress bar smoothly
        timer = object : CountDownTimer(millisRemaining, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = ((millisUntilFinished.toFloat() / 30000) * 300).toInt()
                progressBar.progress = progress
            }

            override fun onFinish() {
                // When 30s window ends, regenerate immediately
                startCodeGeneration()
            }
        }.start()
    }

    private fun updateCode() {
        val code = generateSteamGuardCode(mockSecret)
        txtGuardCode.text = code
    }

    // A simplified Steam Guard style generator (5 chars) based on time
    // Steam uses HMAC-SHA1 and a custom base26 charset
    private fun generateSteamGuardCode(secret: String): String {
        val time = System.currentTimeMillis() / 1000 / 30
        val input = "$secret$time"
        
        // Simple hash logic for demonstration
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray())
        
        // Steam Guard uses a specific set of chars: 23456789BCDFGHJKMNPQRTVWXY (removing similar looking ones)
        val steamChars = charArrayOf('2', '3', '4', '5', '6', '7', '8', '9', 'B', 'C', 'D', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q', 'R', 'T', 'V', 'W', 'X', 'Y')
        
        val sb = StringBuilder()
        // Take first 5 bytes to pick chars
        for (i in 0 until 5) {
            val b = bytes[i].toInt() and 0xFF
            sb.append(steamChars[b % steamChars.size])
        }
        
        return sb.toString()
    }
}
