package com.example.textonly

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qrlogin.ToolbarMenuHandler
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerChats: RecyclerView
    private lateinit var txtEmpty: TextView
    private lateinit var searchBar: EditText
    private lateinit var fabAddChat: FloatingActionButton
    private lateinit var btnSettings: ImageButton

    private lateinit var contactsHelper: ContactsHelper
    private var chatList = mutableListOf<Contact>()
    private lateinit var adapter: ChatListAdapter

    private val pickContactLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val contactUri = result.data!!.data ?: return@registerForActivityResult

            val cursor = contentResolver.query(
                contactUri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val name = it.getString(0)
                    val phone = it.getString(1)

                    val intent = Intent(this, ChatWindowActivity::class.java)
                    intent.putExtra("contact_name", name)
                    intent.putExtra("contact_phone", phone)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // 🔄 Verifică actualizări la pornire
        val updateChecker = UpdateChecker(this)
        updateChecker.checkForUpdates()

        contactsHelper = ContactsHelper(this)

        recyclerChats = findViewById(R.id.recyclerChats)
        txtEmpty = findViewById(R.id.txtEmpty)
        searchBar = findViewById(R.id.searchBar)
        fabAddChat = findViewById(R.id.fabAddChat)
        btnSettings = findViewById(R.id.btnSettings)

        ToolbarMenuHandler.setupToolbar(this, null, btnSettings)

        chatList = contactsHelper.getContacts().toMutableList()
        adapter = ChatListAdapter(chatList)
        recyclerChats.layoutManager = LinearLayoutManager(this)
        recyclerChats.adapter = adapter

        updateEmptyState()

        searchBar.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim() ?: ""
            val filtered = chatList.filter {
                it.name.contains(query, ignoreCase = true)
            }

            adapter = ChatListAdapter(filtered)
            recyclerChats.adapter = adapter

            txtEmpty.text = if (filtered.isEmpty()) "Nicio conversație găsită" else ""
            txtEmpty.visibility = if (filtered.isEmpty()) TextView.VISIBLE else TextView.GONE
        }

        fabAddChat.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            pickContactLauncher.launch(intent)
        }
    }

    private fun updateEmptyState() {
        txtEmpty.visibility = if (chatList.isEmpty()) TextView.VISIBLE else TextView.GONE
    }
}
