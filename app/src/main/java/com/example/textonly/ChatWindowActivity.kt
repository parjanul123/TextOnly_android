package text.only.app

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import text.only.app.ChatFragment

class ChatWindowActivity : AppCompatActivity(), MessageInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_window)

        val txtContactName = findViewById<TextView>(R.id.txtContactName)
        // ... (inițializare alte butoane)

        val contactName = intent.getStringExtra("contact_name")
        val contactPhone = intent.getStringExtra("contact_phone") // Avem nevoie de numărul de telefon
        txtContactName.text = contactName ?: "Contact"

        // ... (setOnClickListener pentru butoane)
        
        if (savedInstanceState == null) {
            // Creăm un ID unic pentru această conversație privată
            val contextId = "private_$contactPhone"
            supportFragmentManager.beginTransaction()
                .replace(R.id.chat_fragment_container, ChatFragment.newInstance(contextId))
                .commit()
        }
    }

    override fun onUnlockFileRequested(message: FileMessage, position: Int) { /* TODO */ }
    override fun onInviteAction(inviteCode: String, accepted: Boolean, position: Int) { /* TODO */ }
}
