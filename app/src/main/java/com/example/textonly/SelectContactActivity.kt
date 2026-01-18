class SelectContactActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChatListAdapter
    private lateinit var contactsHelper: ContactsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_contact)

        recycler = findViewById(R.id.recyclerContacts)
        contactsHelper = ContactsHelper(this)

        val contacts = contactsHelper.getContacts()
        adapter = ChatListAdapter(contacts) { contact ->
            val intent = Intent(this, ChatWindowActivity::class.java)
            intent.putExtra("contact_name", contact.name)
            intent.putExtra("contact_phone", contact.phone)
            startActivity(intent)
            finish()
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }
}
