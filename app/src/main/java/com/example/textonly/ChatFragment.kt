package text.only.app

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatFragment : Fragment() {

    private lateinit var recyclerMessages: RecyclerView
    private lateinit var inputMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnAttach: ImageButton
    private lateinit var messageAdapter: MessageAdapter
    private val chatMessages = mutableListOf<ChatMessage>()

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { showSetPriceDialog(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerMessages = view.findViewById(R.id.recycler_chat_messages)
        inputMessage = view.findViewById(R.id.input_chat_message)
        btnSend = view.findViewById(R.id.btn_send_message)
        btnAttach = view.findViewById(R.id.btn_attach_file)

        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        // Activitatea care găzduiește acest fragment TREBUIE să implementeze MessageInteractionListener
        messageAdapter = MessageAdapter(chatMessages, requireActivity() as MessageInteractionListener)
        recyclerMessages.layoutManager = LinearLayoutManager(context)
        recyclerMessages.adapter = messageAdapter
    }

    private fun setupClickListeners() {
        btnAttach.setOnClickListener { filePickerLauncher.launch("*/*") }

        btnSend.setOnClickListener {
            val messageText = inputMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                val textMessage = TextMessage(content = messageText, isSent = true)
                chatMessages.add(textMessage)
                messageAdapter.notifyItemInserted(chatMessages.size - 1)
                recyclerMessages.scrollToPosition(chatMessages.size - 1)
                inputMessage.text.clear()
                // TODO: Notifică activitatea gazdă să trimită mesajul prin WebSocket
            }
        }
    }

    private fun showSetPriceDialog(fileUri: Uri) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Setează Prețul (OnlyCoins)")
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "0 (gratuit)"
        }
        builder.setView(input)
        builder.setPositiveButton("Trimite") { dialog, _ ->
            val price = input.text.toString().toIntOrNull() ?: 0
            sendFile(fileUri, price)
            dialog.dismiss()
        }
        builder.setNegativeButton("Anulează") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun sendFile(fileUri: Uri, price: Int) {
        val fileName = getFileName(fileUri)
        val fileType = context?.contentResolver?.getType(fileUri)
        val fileMessage = FileMessage(fileName, fileType, price, true, true, fileUri)
        
        chatMessages.add(fileMessage)
        messageAdapter.notifyItemInserted(chatMessages.size - 1)
        recyclerMessages.scrollToPosition(chatMessages.size - 1)
        
        Toast.makeText(context, "Se încarcă fișierul...", Toast.LENGTH_SHORT).show()
        // TODO: Notifică activitatea gazdă să încarce fișierul pe server
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            context?.contentResolver?.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) { result = result?.substring(cut + 1) }
            }
        }
        return result ?: "fișier_necunoscut"
    }
}
