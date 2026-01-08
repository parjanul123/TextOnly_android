package text.only.app


import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
// import ua.naiksoftware.stomp.Stomp
// import ua.naiksoftware.stomp.StompClient

class ChatWindowActivity : AppCompatActivity() {

    private lateinit var txtContactName: TextView
    private lateinit var recyclerMessages: RecyclerView
    private lateinit var inputMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnCall: ImageButton
    private lateinit var btnVideoCall: ImageButton
    private lateinit var btnDetails: ImageButton

    private val messages = mutableListOf<String>()
    private lateinit var adapter: MessageAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var chatListener: ListenerRegistration? = null

    private lateinit var contactName: String
    private lateinit var contactPhone: String

    // ⚠️ Schimbă cu numărul tău real (telefonul 1)
    private val currentUserPhone = "0741111111"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_window)

        txtContactName = findViewById(R.id.txtContactName)
        recyclerMessages = findViewById(R.id.recyclerMessages)
        inputMessage = findViewById(R.id.inputMessage)
        btnSend = findViewById(R.id.btnSend)
        btnCall = findViewById(R.id.btnCall)
        btnVideoCall = findViewById(R.id.btnVideoCall)
        btnDetails = findViewById(R.id.btnDetails)

        contactName = intent.getStringExtra("contact_name") ?: "Necunoscut"
        contactPhone = intent.getStringExtra("contact_phone") ?: ""
        txtContactName.text = contactName

        adapter = MessageAdapter(messages)
        recyclerMessages.layoutManager = LinearLayoutManager(this)
        recyclerMessages.adapter = adapter

        listenForMessages()

        btnSend.setOnClickListener {
            val text = inputMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                inputMessage.text.clear()
            }
        }
    }

    private fun listenForMessages() {
        val chatId = getChatId(currentUserPhone, contactPhone)
        chatListener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Eroare Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    messages.clear()
                    for (doc in snapshots.documents) {
                        val sender = doc.getString("sender")
                        val text = doc.getString("text")
                        if (sender != null && text != null) {
                            val prefix = if (sender == currentUserPhone) "Tu: " else "${contactName}: "
                            messages.add(prefix + text)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    recyclerMessages.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun sendMessage(text: String) {
        val chatId = getChatId(currentUserPhone, contactPhone)
        val message = hashMapOf(
            "sender" to currentUserPhone,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message)
            .addOnFailureListener {
                Toast.makeText(this, "Eroare la trimitere: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getChatId(phone1: String, phone2: String): String {
        return if (phone1 < phone2) "${phone1}_$phone2" else "${phone2}_$phone1"
    }

    override fun onDestroy() {
        super.onDestroy()
        chatListener?.remove()
    }
}
