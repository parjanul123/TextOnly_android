package text.only.app

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CallActivity : AppCompatActivity() {

    private lateinit var txtCallStatus: TextView
    private lateinit var btnSpeaker: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var btnCamera: ImageButton
    private lateinit var btnGift: ImageButton
    private lateinit var btnEndCall: ImageButton

    private var isSpeakerOn = false
    private var isMicOn = true
    private var isCameraOn = true

    private var userCoins = 120 // exemplu; ulterior vine din portofelul utilizatorului

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        txtCallStatus = findViewById(R.id.txtCallStatus)
        btnSpeaker = findViewById(R.id.btnSpeaker)
        btnMic = findViewById(R.id.btnMic)
        btnCamera = findViewById(R.id.btnCamera)
        btnGift = findViewById(R.id.btnGift)
        btnEndCall = findViewById(R.id.btnEndCall)

        val contactName = intent.getStringExtra("contact_name")
        val callType = intent.getStringExtra("call_type")
        txtCallStatus.text = "Apel $callType cu $contactName..."

        // 🔊 Difuzor
        btnSpeaker.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            val msg = if (isSpeakerOn) "Difuzor activat 🔊" else "Difuzor dezactivat 🔇"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        // 🎤 Microfon
        btnMic.setOnClickListener {
            isMicOn = !isMicOn
            val msg = if (isMicOn) "Microfon pornit 🎤" else "Microfon oprit 🤫"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        // 📸 Cameră
        btnCamera.setOnClickListener {
            if (callType == "audio") {
                Toast.makeText(this, "Apel audio — camera nu e disponibilă", Toast.LENGTH_SHORT).show()
            } else {
                isCameraOn = !isCameraOn
                val msg = if (isCameraOn) "Camera pornită 📸" else "Camera oprită 🚫"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        // 🎁 Gift-uri OnlyCoins
        btnGift.setOnClickListener {
            val giftSheet = GiftBottomSheet(userCoins) { gift ->
                userCoins -= gift.price
                Toast.makeText(
                    this,
                    "Ai trimis ${gift.name}! (-${gift.price} 🪙)\nMonede rămase: $userCoins",
                    Toast.LENGTH_SHORT
                ).show()

                // TODO: adaugă animația pe ecran (inimă / confetti / etc.)
            }
            giftSheet.show(supportFragmentManager, "GiftBottomSheet")
        }

        // ⛔ Închidere apel
        btnEndCall.setOnClickListener {
            Toast.makeText(this, "Apel închis ⛔", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
