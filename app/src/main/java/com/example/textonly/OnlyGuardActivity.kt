package text.only.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class OnlyGuardActivity : AppCompatActivity() {

    // Layouts
    private lateinit var layoutSetup: LinearLayout
    private lateinit var layoutActive: LinearLayout
    private lateinit var layoutScan: LinearLayout
    private lateinit var layoutChallenge: LinearLayout
    
    // Buttons
    private lateinit var btnAddAuthenticator: Button
    private lateinit var btnRemoveAuthenticator: Button
    private lateinit var btnScanQr: Button
    private lateinit var btnOption1: Button
    private lateinit var btnOption2: Button
    private lateinit var btnOption3: Button
    private lateinit var btnCancelAuth: Button
    
    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val qrContent = result.data?.getStringExtra("SCANNED_QR")
            if (qrContent != null) {
                showChallengeUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_only_guard)

        // Init views
        layoutSetup = findViewById(R.id.layoutSetup)
        layoutActive = findViewById(R.id.layoutActive)
        layoutScan = findViewById(R.id.layoutScan)
        layoutChallenge = findViewById(R.id.layoutChallenge)
        
        btnAddAuthenticator = findViewById(R.id.btnAddAuthenticator)
        btnRemoveAuthenticator = findViewById(R.id.btnRemoveAuthenticator)
        btnScanQr = findViewById(R.id.btnScanQr)
        
        btnOption1 = findViewById(R.id.btnOption1)
        btnOption2 = findViewById(R.id.btnOption2)
        btnOption3 = findViewById(R.id.btnOption3)
        btnCancelAuth = findViewById(R.id.btnCancelAuth)

        checkStatus()
        
        btnAddAuthenticator.setOnClickListener {
            // Simulate adding logic (e.g., verifying phone number)
            enableAuthenticator()
        }
        
        btnRemoveAuthenticator.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Elimină OnlyGuard")
                .setMessage("Ești sigur că vrei să dezactivezi autentificarea în doi pași? Contul tău va fi mai puțin sigur.")
                .setPositiveButton("Elimină") { _, _ -> disableAuthenticator() }
                .setNegativeButton("Anulează", null)
                .show()
        }

        btnScanQr.setOnClickListener {
            val intent = Intent(this, ScanQRActivity::class.java)
            intent.putExtra("RETURN_RESULT", true)
            scanLauncher.launch(intent)
        }
        
        btnCancelAuth.setOnClickListener {
            resetToActiveScan()
        }
    }
    
    private fun checkStatus() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("isOnlyGuardEnabled", false)
        
        if (isEnabled) {
            showActiveState()
        } else {
            showSetupState()
        }
    }
    
    private fun enableAuthenticator() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("isOnlyGuardEnabled", true).apply()
        Toast.makeText(this, "OnlyGuard Activat! ✅", Toast.LENGTH_SHORT).show()
        showActiveState()
    }
    
    private fun disableAuthenticator() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("isOnlyGuardEnabled", false).apply()
        Toast.makeText(this, "OnlyGuard Eliminat.", Toast.LENGTH_SHORT).show()
        showSetupState()
    }

    private fun showSetupState() {
        layoutSetup.visibility = View.VISIBLE
        layoutActive.visibility = View.GONE
    }
    
    private fun showActiveState() {
        layoutSetup.visibility = View.GONE
        layoutActive.visibility = View.VISIBLE
        resetToActiveScan()
    }
    
    private fun resetToActiveScan() {
        layoutScan.visibility = View.VISIBLE
        layoutChallenge.visibility = View.GONE
    }

    private fun showChallengeUI() {
        layoutScan.visibility = View.GONE
        layoutChallenge.visibility = View.VISIBLE
        
        val options = mutableListOf<Int>()
        while (options.size < 3) {
            val n = Random.nextInt(10, 99)
            if (n !in options) options.add(n)
        }
        
        btnOption1.text = options[0].toString()
        btnOption2.text = options[1].toString()
        btnOption3.text = options[2].toString()
        
        val listener = View.OnClickListener { v ->
            val selectedNumber = (v as Button).text.toString()
            simulateAuthCheck(selectedNumber)
        }
        
        btnOption1.setOnClickListener(listener)
        btnOption2.setOnClickListener(listener)
        btnOption3.setOnClickListener(listener)
    }
    
    private fun simulateAuthCheck(number: String) {
        Toast.makeText(this, "Autentificare confirmată pentru $number! ✅", Toast.LENGTH_LONG).show()
        resetToActiveScan()
    }
}
