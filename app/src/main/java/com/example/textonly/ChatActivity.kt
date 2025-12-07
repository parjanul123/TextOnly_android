package com.example.textonly

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerChats: RecyclerView
    private lateinit var txtEmpty: TextView
    private lateinit var searchBar: EditText
    private lateinit var fabAddChat: FloatingActionButton

    private lateinit var contactsHelper: ContactsHelper
    private var chatList = mutableListOf<Contact>()
    private lateinit var adapter: ChatListAdapter

    // 🔹 Launcher pentru alegerea contactului din agendă
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

        // ✅ Inițializează helperul AICI, după ce există un context valid
        contactsHelper = ContactsHelper(this)

        recyclerChats = findViewById(R.id.recyclerChats)
        txtEmpty = findViewById(R.id.txtEmpty)
        searchBar = findViewById(R.id.searchBar)
        fabAddChat = findViewById(R.id.fabAddChat)

        // 🔹 Încarcă lista de contacte salvate în aplicație
        chatList = contactsHelper.getContacts().toMutableList()
        adapter = ChatListAdapter(chatList)
        recyclerChats.layoutManager = LinearLayoutManager(this)
        recyclerChats.adapter = adapter

        updateEmptyState()

        // 🔍 Căutare conversații
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

        // ➕ Deschide agenda telefonului pentru conversație nouă
        fabAddChat.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            pickContactLauncher.launch(intent)
        }
    }

    private fun updateEmptyState() {
        txtEmpty.visibility = if (chatList.isEmpty()) TextView.VISIBLE else TextView.GONE
    }
}
