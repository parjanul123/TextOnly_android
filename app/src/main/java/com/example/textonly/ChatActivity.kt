package text.only.app

import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import text.only.app.qrlogin.ToolbarMenuHandler

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerMainList: RecyclerView
    private lateinit var mainListAdapter: MainListAdapter
    private var mainList = mutableListOf<MainListItem>()
    private var actionMode: ActionMode? = null
    
    // ... (alte referințe UI)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recyclerMainList = findViewById(R.id.recyclerChats)
        val fabAddChat: FloatingActionButton = findViewById(R.id.fabAddChat)
        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        val btnAddServer: ImageButton = findViewById(R.id.btnAddServer)

        setupRecyclerView()
        loadAllItemsFromDb() // Încărcăm totul

        ToolbarMenuHandler.setupToolbar(this, null, btnSettings)

        fabAddChat.setOnClickListener {
            startActivity(Intent(this, SelectContactActivity::class.java))
        }

        btnAddServer.setOnClickListener {
            showCreateServerDialog()
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
            
            // TODO: Sortează lista combinată, de ex. după nume
            
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
        // ... (funcția rămâne la fel, dar reîncarcă totul)
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
