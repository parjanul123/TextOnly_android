package text.only.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import text.only.app.qrlogin.ToolbarMenuHandler
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerMainList: RecyclerView
    private lateinit var mainListAdapter: MainListAdapter
    private var mainList = mutableListOf<MainListItem>()
    private var actionMode: ActionMode? = null
    
    // Voice Bar
    private lateinit var layoutVoiceBar: LinearLayout
    private lateinit var txtVoiceBarChannel: TextView
    private lateinit var btnVoiceBarDisconnect: ImageButton
    private lateinit var containerVoiceInfo: LinearLayout
    
    private val voiceListener = {
        runOnUiThread {
            updateVoiceBarUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recyclerMainList = findViewById(R.id.recyclerChats)
        val fabAddChat: FloatingActionButton = findViewById(R.id.fabAddChat)
        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        val btnAddServer: ImageButton = findViewById(R.id.btnAddServer)
        
        layoutVoiceBar = findViewById(R.id.layoutVoiceBar)
        txtVoiceBarChannel = findViewById(R.id.txtVoiceBarChannel)
        btnVoiceBarDisconnect = findViewById(R.id.btnVoiceBarDisconnect)
        containerVoiceInfo = findViewById(R.id.containerVoiceInfo)

        setupRecyclerView()
        loadAllItemsFromDb() // Încărcăm totul
        setupVoiceBarListeners()

        ToolbarMenuHandler.setupToolbar(this, null, btnSettings)

        fabAddChat.setOnClickListener {
            startActivity(Intent(this, SelectContactActivity::class.java))
        }

        btnAddServer.setOnClickListener {
            showCreateServerDialog()
        }
        
        VoiceConnectionManager.listeners.add(voiceListener)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        VoiceConnectionManager.listeners.remove(voiceListener)
    }
    
    override fun onResume() {
        super.onResume()
        updateVoiceBarUI()
        // --- MODIFICARE: Verificare actualizări nume contacte ---
        refreshContactNames()
    }
    
    private fun refreshContactNames() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            // 1. Luăm toate conversațiile
            val conversations = db.conversationDao().getAllConversations()
            var changesMade = false
            
            conversations.forEach { conv ->
                // 2. Căutăm numele în agenda telefonului pe baza numărului
                val newName = getContactNameFromPhone(conv.contactPhone)
                
                // 3. Dacă numele diferă și am găsit unul valid, actualizăm
                if (newName != null && newName != conv.contactName) {
                    val updatedConv = conv.copy(contactName = newName)
                    db.conversationDao().update(updatedConv) 
                    changesMade = true
                }
            }
            
            // 4. Reîncărcăm lista (always, as per the comment about redundancy of else block)
            loadAllItemsFromDb()
        }
    }
    
    private fun getContactNameFromPhone(phoneNumber: String): String? {
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return null
        }
        
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        
        try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    private fun updateVoiceBarUI() {
        val currentChannel = VoiceConnectionManager.currentChannelName
        if (currentChannel != null) {
            layoutVoiceBar.visibility = View.VISIBLE
            txtVoiceBarChannel.text = currentChannel
        } else {
            layoutVoiceBar.visibility = View.GONE
        }
    }
    
    private fun setupVoiceBarListeners() {
        btnVoiceBarDisconnect.setOnClickListener {
            // Disconnect Logic
            if (VoiceConnectionManager.currentChannelName != null) {
                Toast.makeText(this, "Deconectat de la voce.", Toast.LENGTH_SHORT).show()
                VoiceService.stop(this)
                layoutVoiceBar.visibility = View.GONE
            }
        }
        containerVoiceInfo.setOnClickListener {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                val channelName = VoiceConnectionManager.currentChannelName
                if (channelName != null) {
                    val intent = Intent(this@ChatActivity, ServerActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        mainListAdapter = MainListAdapter(mainList,
            { item -> // Click normal
                when (item) {
                    is MainListItem.ServerItem -> {
                        val intent = Intent(this, ServerActivity::class.java).apply {
                            putExtra("SERVER_NAME", item.server.name)
                            putExtra("SERVER_ID", item.server.id)
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // Reuses existing ServerActivity if active
                        }
                        startActivity(intent)
                    }
                    is MainListItem.ConversationItem -> {
                        val intent = Intent(this, ChatWindowActivity::class.java).apply {
                            putExtra("contact_name", item.conversation.contactName)
                            putExtra("contact_phone", item.conversation.contactPhone)
                        }
                        startActivity(intent)
                    }
                }
            },
            { item -> // Click lung
                if (actionMode == null) {
                    actionMode = startActionMode(ActionModeCallback(item))
                }
                true
            }
        )
        recyclerMainList.layoutManager = LinearLayoutManager(this)
        recyclerMainList.adapter = mainListAdapter
    }
    
    private fun loadAllItemsFromDb() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val servers = db.serverDao().getAllServers()
            val conversations = db.conversationDao().getAllConversations()

            mainList.clear()
            mainList.addAll(servers.map { MainListItem.ServerItem(it) })
            mainList.addAll(conversations.map { MainListItem.ConversationItem(it) })
            
            mainListAdapter.updateItems(mainList)
        }
    }
    
    private inner class ActionModeCallback(private val item: MainListItem) : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.context_menu, menu)
            mode?.title = when (item) {
                is MainListItem.ServerItem -> item.server.name
                is MainListItem.ConversationItem -> item.conversation.contactName
            }
            return true
        }
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
        override fun onActionItemClicked(mode: ActionMode?, menuItem: MenuItem?): Boolean {
            return when (menuItem?.itemId) {
                R.id.action_delete -> {
                    lifecycleScope.launch {
                        val db = AppDatabase.getInstance(applicationContext)
                        when (item) {
                            is MainListItem.ServerItem -> db.serverDao().delete(item.server)
                            is MainListItem.ConversationItem -> db.conversationDao().delete(item.conversation)
                        }
                        loadAllItemsFromDb()
                    }
                    Toast.makeText(this@ChatActivity, "Șters", Toast.LENGTH_SHORT).show()
                    mode?.finish()
                    true
                }
                else -> {
                    Toast.makeText(this@ChatActivity, "Acțiune: ${menuItem?.title}", Toast.LENGTH_SHORT).show()
                    mode?.finish()
                    true
                }
            }
        }
        override fun onDestroyActionMode(mode: ActionMode?) { actionMode = null }
    }

    private fun showCreateServerDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Creează un Server Nou")
        val input = EditText(this).apply { hint = "Numele serverului" }
        builder.setView(input)
        builder.setPositiveButton("Creează") { _, _ ->
            val serverName = input.text.toString().trim()
            if (serverName.isNotEmpty()) {
                lifecycleScope.launch {
                    AppDatabase.getInstance(applicationContext).serverDao().insert(Server(name = serverName))
                    loadAllItemsFromDb() // Reîncărcăm toată lista
                }
            }
        }
        builder.setNegativeButton("Anulează", null)
        builder.show()
    }
}