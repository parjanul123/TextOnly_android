package text.only.app

import android.os.Bundle
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout

class ServerActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerChannels: RecyclerView
    private lateinit var fabAddChannel: FloatingActionButton
    private lateinit var channelAdapter: ChannelAdapter
    private val channels = mutableListOf<Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        toolbar = findViewById(R.id.toolbarServer)
        recyclerChannels = findViewById(R.id.recyclerChannels)
        fabAddChannel = findViewById(R.id.fabAddChannel)

        val serverName = intent.getStringExtra("SERVER_NAME")
        toolbar.title = serverName ?: "Server"
        setSupportActionBar(toolbar)

        setupRecyclerView()

        fabAddChannel.setOnClickListener {
            showCreateChannelDialog()
        }
        
        // TODO: Încarcă lista de canale de pe serverul tău
    }
    
    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter(channels)
        recyclerChannels.layoutManager = LinearLayoutManager(this)
        recyclerChannels.adapter = channelAdapter
    }

    private fun showCreateChannelDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Creează un Canal Nou")

        // Inflăm un layout custom pentru dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_channel, null)
        val inputChannelName = dialogView.findViewById<EditText>(R.id.inputChannelName)
        val radioGroupType = dialogView.findViewById<RadioGroup>(R.id.radioGroupType)
        
        builder.setView(dialogView)

        builder.setPositiveButton("Creează") { dialog, _ ->
            val channelName = inputChannelName.text.toString().trim()
            val selectedTypeId = radioGroupType.checkedRadioButtonId
            
            if (channelName.isNotEmpty() && selectedTypeId != -1) {
                val channelType = if (selectedTypeId == R.id.radioText) ChannelType.TEXT else ChannelType.VOICE
                
                //
                // --- TODO: AICI INTERVINE LOGICA TA DE BACKEND ---
                //
                // 1. Trimite `channelName` și `channelType` la backend
                //    (ex: POST /api/servers/{serverId}/channels).
                // 2. Serverul creează canalul și notifică membrii (posibil prin WebSocket).
                //
                
                // Adăugăm local pentru a vedea rezultatul instant
                channels.add(Channel(channelName, channelType))
                channelAdapter.notifyItemInserted(channels.size - 1)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Anulează") { dialog, _ -> dialog.cancel() }
        builder.show()
    }
}
