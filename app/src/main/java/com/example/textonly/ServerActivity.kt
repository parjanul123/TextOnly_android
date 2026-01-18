package text.only.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ServerActivity : AppCompatActivity(), MessageInteractionListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerChannels: RecyclerView
    private lateinit var btnAddChannel: Button
    private lateinit var channelAdapter: ChannelAdapter
    private var channels = mutableListOf<Channel>()
    private var serverId: Int = -1
    private lateinit var recyclerMembers: RecyclerView
    private lateinit var headerServerName: TextView
    private lateinit var memberAdapter: MemberAdapter
    private val members = mutableListOf<Member>()
    private lateinit var toggle: ActionBarDrawerToggle

    // Voice UI
    private lateinit var layoutVoiceBar: LinearLayout
    private lateinit var txtVoiceBarChannel: TextView
    private lateinit var btnVoiceBarDisconnect: ImageButton
    private lateinit var containerVoiceInfo: LinearLayout
    
    private var currentVoiceChannelName: String? = null

    private val inviteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val contactName = data?.getStringExtra("CONTACT_NAME")
            val contactPhone = data?.getStringExtra("CONTACT_PHONE")

            if (contactName != null && contactPhone != null) {
                lifecycleScope.launch {
                    val db = AppDatabase.getInstance(applicationContext)
                    
                    // 1. Asigurăm că există conversația
                    val conversation = ConversationEntity(contactName = contactName, contactPhone = contactPhone)
                    db.conversationDao().insert(conversation)

                    // 2. Creăm mesajul de invitație
                    val contextId = "private_$contactPhone"
                    val expiryTime = System.currentTimeMillis() + 7200000 // 2 ore
                    val inviteCode = "INV-${System.currentTimeMillis()}"
                    val serverName = intent.getStringExtra("SERVER_NAME") ?: "Server"

                    val inviteMessage = MessageEntity(
                        contextId = contextId,
                        content = "Invitație Server: $serverName",
                        isSent = true,
                        type = "INVITE",
                        inviteCode = inviteCode,
                        inviteServerName = serverName,
                        inviteExpiry = expiryTime
                    )

                    db.messageDao().insert(inviteMessage)
                    Toast.makeText(this@ServerActivity, "Invitație trimisă lui $contactName!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        serverId = intent.getIntExtra("SERVER_ID", -1)
        if (serverId == -1) {
            finish()
            return
        }
        
        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbarServer)
        recyclerChannels = findViewById(R.id.recyclerChannels)
        btnAddChannel = findViewById(R.id.btnAddChannel)
        headerServerName = findViewById(R.id.server_name_header)
        recyclerMembers = findViewById(R.id.recyclerMembers)
        
        // Voice Bar init
        layoutVoiceBar = findViewById(R.id.layoutVoiceBar)
        txtVoiceBarChannel = findViewById(R.id.txtVoiceBarChannel)
        btnVoiceBarDisconnect = findViewById(R.id.btnVoiceBarDisconnect)
        containerVoiceInfo = findViewById(R.id.containerVoiceInfo)

        val serverName = intent.getStringExtra("SERVER_NAME")
        toolbar.title = serverName ?: "Server"
        headerServerName.text = serverName ?: "Server"
        setSupportActionBar(toolbar)
        
        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        setupRecyclerViews()
        loadChannelsFromDb()
        setupVoiceBarListeners()
        setupBackNavigation()
        
        btnAddChannel.setOnClickListener { showCreateChannelDialog() }
    }
    
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                
                // Dacă suntem în ecranul Full Screen de Voce
                if (currentFragment is ChannelVoiceFragment) {
                    // Închidem fragmentul (ne întoarcem la ecranul de server/text)
                    supportFragmentManager.beginTransaction().remove(currentFragment).commit()
                    toolbar.title = intent.getStringExtra("SERVER_NAME") ?: "Server"
                    
                    // RESTAURĂM BARA MINI (pentru că rămânem conectați)
                    if (currentVoiceChannelName != null) {
                        layoutVoiceBar.visibility = View.VISIBLE
                        txtVoiceBarChannel.text = currentVoiceChannelName
                    }
                } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    // Dacă nu suntem în voce și nici meniuri deschise, ieșim din activity
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (currentVoiceChannelName != null) {
            try {
                // Intră în modul PiP dacă suntem conectați la voce
                enterPictureInPictureMode()
            } catch (e: Exception) {
                // PiP poate să nu fie suportat sau permis
            }
        }
    }
    
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            // Ascundem elementele UI inutile
            toolbar.visibility = View.GONE
            layoutVoiceBar.visibility = View.GONE // Ascundem bara mini
            // Drawer-ul se blochează implicit în PiP, dar putem ascunde iconița de meniu
            supportActionBar?.hide()
            
            // Putem notifica fragmentul curent să își simplifice UI-ul dacă e necesar
        } else {
            // Restaurăm UI
            toolbar.visibility = View.VISIBLE
            supportActionBar?.show()
            
            // Re-afișăm bara mini DOAR dacă nu suntem în fragmentul de voce full screen
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment !is ChannelVoiceFragment && currentVoiceChannelName != null) {
                layoutVoiceBar.visibility = View.VISIBLE
            }
        }
    }
    
    private fun setupVoiceBarListeners() {
        btnVoiceBarDisconnect.setOnClickListener {
            disconnectVoice()
        }
        containerVoiceInfo.setOnClickListener {
            // Dacă dăm click pe bară, redeschidem fragmentul de voce
            currentVoiceChannelName?.let { channelName ->
                openChannel(Channel(channelName, ChannelType.VOICE))
            }
        }
    }
    
    private fun setupRecyclerViews() {
        channelAdapter = ChannelAdapter(channels) { channel -> openChannel(channel) }
        recyclerChannels.layoutManager = LinearLayoutManager(this)
        recyclerChannels.adapter = channelAdapter
        memberAdapter = MemberAdapter(members) { member -> showMemberDetailsDialog(member) }
        recyclerMembers.layoutManager = LinearLayoutManager(this)
        recyclerMembers.adapter = memberAdapter
    }

    private fun loadChannelsFromDb() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val channelEntities = db.channelDao().getChannelsForServer(serverId)
            channels.clear()
            channels.addAll(channelEntities.map { Channel(it.name, ChannelType.valueOf(it.type)) })
            channelAdapter.notifyDataSetChanged()
        }
    }

    private fun getCurrentUser(): VoiceUser {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val name = prefs.getString("displayName", "Eu") ?: "Eu"
        val avatarUri = prefs.getString("profileImageUri", null)
        return VoiceUser(name = name, avatarUri = avatarUri)
    }

    private fun openChannel(channel: Channel) {
        val channelId = channel.name
        
        if (channel.type == ChannelType.TEXT) {
            // Deschidem fragment text
            val contextId = "channel_${serverId}_$channelId"
            val fragment = ChatFragment.newInstance(contextId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            toolbar.title = "# ${channel.name}"
            
            // Dacă suntem conectați la voce, arătăm bara jos
            if (currentVoiceChannelName != null) {
                layoutVoiceBar.visibility = View.VISIBLE
                txtVoiceBarChannel.text = currentVoiceChannelName
            } else {
                layoutVoiceBar.visibility = View.GONE
            }
            
        } else if (channel.type == ChannelType.VOICE) {
            val user = getCurrentUser()
            
            // Logică de conectare/switch voce
            if (currentVoiceChannelName != channel.name) {
                if (currentVoiceChannelName != null) {
                   // Remove from old
                   channelAdapter.moveUserToChannel(user, channel.name) // Move directly enforces remove
                   channelAdapter.collapseChannel(currentVoiceChannelName!!)
                } else {
                    channelAdapter.moveUserToChannel(user, channel.name)
                }
                
                // Ne conectăm la noul canal
                currentVoiceChannelName = channel.name
                
                // Expandăm automat canalul pentru a vedea utilizatorul
                channelAdapter.expandChannel(channel.name)
            }
            
            // Deschidem fragmentul de voce
            val fragment = ChannelVoiceFragment.newInstance(channel.name)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            
            toolbar.title = "\uD83D\uDD0A ${channel.name}"
            
            // Ascundem bara mini, pentru că suntem în full screen
            layoutVoiceBar.visibility = View.GONE
        }

        drawerLayout.closeDrawer(GravityCompat.START)
    }
    
    fun disconnectVoice() {
        if (currentVoiceChannelName != null) {
            Toast.makeText(this, "Deconectat de la voce.", Toast.LENGTH_SHORT).show()
            
            // Eliminăm userul din toate canalele
            val user = getCurrentUser()
            channelAdapter.removeUserFromAllChannels(user)
            
            currentVoiceChannelName = null
            layoutVoiceBar.visibility = View.GONE
            
            // Dacă suntem chiar în fragmentul de voce, ieșim sau afișăm ceva gol
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is ChannelVoiceFragment) {
                 supportFragmentManager.beginTransaction()
                    .remove(currentFragment)
                    .commit()
                 toolbar.title = "Server"
                 supportActionBar?.show() // Asigurăm că toolbar reapare
            }
        }
    }

    override fun onUnlockFileRequested(message: FileMessage, position: Int) {
        // TODO: Implementează logica de plată pentru fișiere pe canale
    }

    override fun onInviteAction(inviteCode: String, accepted: Boolean, position: Int) {
        // Această acțiune nu este relevantă în ecranul de server, dar trebuie implementată
    }
    
    private fun showCreateChannelDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Creează un Canal Nou")
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_channel, null)
        val inputChannelName = dialogView.findViewById<TextInputEditText>(R.id.inputChannelName)
        val radioGroupType = dialogView.findViewById<RadioGroup>(R.id.radioGroupType)
        builder.setView(dialogView)
        builder.setPositiveButton("Creează") { _, _ ->
            val channelName = inputChannelName.text.toString().trim()
            val selectedTypeId = radioGroupType.checkedRadioButtonId
            if (channelName.isNotEmpty() && selectedTypeId != -1) {
                val channelType = if (selectedTypeId == R.id.radioText) ChannelType.TEXT else ChannelType.VOICE
                lifecycleScope.launch {
                    val newChannelEntity = ChannelEntity(serverId = serverId, name = channelName, type = channelType.name)
                    AppDatabase.getInstance(applicationContext).channelDao().insert(newChannelEntity)
                    loadChannelsFromDb()
                }
            }
        }
        builder.setNegativeButton("Anulează", null)
        builder.show()
    }

    private fun showMemberDetailsDialog(member: Member) { /* ... */ }
    override fun onPostCreate(savedInstanceState: Bundle?) { super.onPostCreate(savedInstanceState); toggle.syncState() }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); toggle.onConfigurationChanged(newConfig) }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean { menuInflater.inflate(R.menu.server_menu, menu); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) { return true }
        when (item.itemId) {
            R.id.action_invite -> {
                val intent = Intent(this, SelectContactActivity::class.java)
                intent.putExtra("IS_FOR_INVITE", true)
                inviteLauncher.launch(intent)
                return true
            }
            R.id.action_members -> { drawerLayout.openDrawer(GravityCompat.END); return true }
        }
        return super.onOptionsItemSelected(item)
    }
}
