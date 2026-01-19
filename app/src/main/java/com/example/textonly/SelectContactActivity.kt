package text.only.app


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class SelectContactActivity : AppCompatActivity() {

    private lateinit var recyclerContacts: RecyclerView
    private lateinit var searchContacts: EditText
    private lateinit var adapter: ContactSelectAdapter // Am declarat adapter-ul aici
    private var allContacts = mutableListOf<Contact>()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) loadContacts() else Toast.makeText(this, "Permisiune refuzată.", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_contact)

        recyclerContacts = findViewById(R.id.recyclerContacts)
        searchContacts = findViewById(R.id.searchContacts)
        recyclerContacts.layoutManager = LinearLayoutManager(this)

        val isForInvite = intent.getBooleanExtra("IS_FOR_INVITE", false)

        adapter = ContactSelectAdapter(mutableListOf()) { contact ->
            if (isForInvite) {
                val resultIntent = Intent().apply {
                    putExtra("CONTACT_NAME", contact.name)
                    putExtra("CONTACT_PHONE", contact.phone)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                // --- FIX: Salvăm conversația în DB înainte de a deschide fereastra de chat ---
                lifecycleScope.launch {
                    val db = AppDatabase.getInstance(applicationContext)
                    // Insert (Ignore on conflict thanks to DAO logic) to ensure it appears in the main list
                    val newConversation = ConversationEntity(
                        contactName = contact.name, 
                        contactPhone = contact.phone
                    )
                    db.conversationDao().insert(newConversation)
                    
                    // Acum deschidem chat-ul
                    val intent = Intent(this@SelectContactActivity, ChatWindowActivity::class.java).apply {
                        putExtra("contact_name", contact.name)
                        putExtra("contact_phone", contact.phone)
                    }
                    startActivity(intent)
                    finish() 
                }
            }
        }
        recyclerContacts.adapter = adapter

        checkPermissionsAndLoadContacts()

        searchContacts.addTextChangedListener { text ->
            filter(text.toString())
        }
    }

    private fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            allContacts
        } else {
            allContacts.filter { it.name.startsWith(query, ignoreCase = true) }
        }
        adapter.updateContacts(filteredList)
    }

    private fun checkPermissionsAndLoadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            loadContacts()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun loadContacts() {
        val contacts = ContactsHelper(this).getContacts()
        if (contacts.isEmpty()) {
            Toast.makeText(this, "Nu am găsit contacte.", Toast.LENGTH_LONG).show()
        }
        allContacts.clear()
        allContacts.addAll(contacts)
        adapter.updateContacts(allContacts)
    }
}
