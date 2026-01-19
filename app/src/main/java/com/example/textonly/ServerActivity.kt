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
    
    // Eliminat: private var currentVoiceChannelName: String? = null
    // Acum folosim VoiceConnectionManager.currentChannelName

    private val inviteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val contactName = data?.getStringExtra("CONTACT_NAME")
            val contactPhone = data?.getStringExtra("CONTACT_PHONE")

            if (contactName != null && contactPhone != null) {
                lifecycleScope.launch {
                    val db = AppDatabase.getInstance(applicationContext)
                    val conversation = ConversationEntity(contactName = contactName, contactPhone = contactPhone)
                    db.conversationDao().insert(conversation)
                    val contextId = "private_$contactPhone"
                    val expiryTime = System.currentTimeMillis() + 7200000 
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
    
    // Listener pt updates din service
    private val voiceListener = {
        runOnUiThread {
            updateVoiceBarUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)
        
        initViews()
        handleIntent(intent)
        setupBackNavigation()
        
        // Înregistrează listener
        VoiceConnectionManager.listeners.add(voiceListener)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        VoiceConnectionManager.listeners.remove(voiceListener)
    }
    
    override fun onResume() {
        super.onResume()
        updateVoiceBarUI() // Refresh la revenire
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbarServer)
        recyclerChannels = findViewById(R.id.recyclerChannels)
        btnAddChannel = findViewById(R.id.btnAddChannel)
        headerServerName = findViewById(R.id.server_name_header)
        recyclerMembers = findViewById(R.id.recyclerMembers)
        
        layoutVoiceBar = findViewById(R.id.layoutVoiceBar)
        txtVoiceBarChannel = findViewById(R.id.txtVoiceBarChannel)
        btnVoiceBarDisconnect = findViewById(R.id.btnVoiceBarDisconnect)
        containerVoiceInfo = findViewById(R.id.containerVoiceInfo)

        setSupportActionBar(toolbar)
        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        setupRecyclerViews()
        setupVoiceBarListeners()
        btnAddChannel.setOnClickListener { showCreateChannelDialog() }
    }

    private fun handleIntent(intent: Intent) {
        val newServerId = intent.getIntExtra("SERVER_ID", -1)
        if (newServerId == -1) {
            // Dacă venim din notificare (VoiceService), poate nu avem SERVER_ID setat corect?
            // În acest caz, doar rămânem pe ecran (sau încărcăm un server default/ultimul)
            // Pentru simplificare, presupunem că intentul de lansare are datele sau că nu facem nimic dacă e null
            if (VoiceConnectionManager.isConnected) {
                // Suntem conectați la voce, dar nu știm pe ce server suntem vizual?
                // Ideal ar fi să salvăm serverId în Service. 
                // Dar să zicem că nu dăm finish() dacă e doar revenire din notificare fără extra.
                if (intent.action == Intent.ACTION_MAIN) { 
                     // Lansat din launcher sau notificare generică
                     return 
                }
            }
            if (!VoiceConnectionManager.isConnected) {
                finish()
                return
            }
        }

        if (newServerId != -1 && serverId != newServerId) {
             serverId = newServerId
             val serverName = intent.getStringExtra("SERVER_NAME")
             toolbar.title = serverName ?: "Server"
             headerServerName.text = serverName ?: "Server"
             
             loadChannelsFromDb()
             
             supportFragmentManager.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
             val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
             if (currentFragment != null) {
                 supportFragmentManager.beginTransaction().remove(currentFragment).commit()
             }
        }
        
        updateVoiceBarUI()
    }
    
    private fun updateVoiceBarUI() {
        val currentChannel = VoiceConnectionManager.currentChannelName
        if (currentChannel != null) {
             layoutVoiceBar.visibility = View.VISIBLE
             txtVoiceBarChannel.text = currentChannel
             
             // Verificăm dacă suntem în fragmentul voice. Dacă NU suntem, și avem voce, arătăm bara.
             // Dacă SUNTEM în fragmentul voice, ascundem bara.
             val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
             if (currentFragment is ChannelVoiceFragment) {
                 layoutVoiceBar.visibility = View.GONE
             }
        } else {
             layoutVoiceBar.visibility = View.GONE
        }
    }
    
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                val currentVoice = VoiceConnectionManager.currentChannelName
                
                if (currentFragment is ChannelVoiceFragment) {
                    supportFragmentManager.beginTransaction().remove(currentFragment).commit()
                    toolbar.title = intent.getStringExtra("SERVER_NAME") ?: "Server"
                    updateVoiceBarUI()
                } 
                else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                } 
                else if (currentVoice != null) {
                    val intent = Intent(this@ServerActivity, ChatActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                } 
                else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (VoiceConnectionManager.currentChannelName != null) {
            try {
                enterPictureInPictureMode()
            } catch (e: Exception) { }
        }
    }
    
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            toolbar.visibility = View.GONE
            layoutVoiceBar.visibility = View.GONE
            supportActionBar?.hide()
        } else {
            toolbar.visibility = View.VISIBLE
            supportActionBar?.show()
            updateVoiceBarUI()
        }
    }
    
    private fun setupVoiceBarListeners() {
        btnVoiceBarDisconnect.setOnClickListener { disconnectVoice() }
        containerVoiceInfo.setOnClickListener {
            VoiceConnectionManager.currentChannelName?.let { channelName -> openChannel(Channel(channelName, ChannelType.VOICE)) }
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
            val contextId = "channel_${serverId}_$channelId"
            val fragment = ChatFragment.newInstance(contextId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            toolbar.title = "# ${channel.name}"
            updateVoiceBarUI()
            
        } else if (channel.type == ChannelType.VOICE) {
            val user = getCurrentUser()
            val currentVoice = VoiceConnectionManager.currentChannelName
            
            if (currentVoice != channel.name) {
                // Start Service Call
                VoiceService.start(this, channel.name)
                
                // UI local update (Server-list adapter logic)
                if (currentVoice != null) {
                   channelAdapter.moveUserToChannel(user, channel.name) 
                   channelAdapter.collapseChannel(currentVoice)
                } else {
                    channelAdapter.moveUserToChannel(user, channel.name)
                }
                channelAdapter.expandChannel(channel.name)
            }
            
            val fragment = ChannelVoiceFragment.newInstance(channel.name)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            toolbar.title = "\uD83D\uDD0A ${channel.name}"
            layoutVoiceBar.visibility = View.GONE
        }
        drawerLayout.closeDrawer(GravityCompat.START)
    }
    
    fun disconnectVoice() {
        if (VoiceConnectionManager.currentChannelName != null) {
            Toast.makeText(this, "Deconectat de la voce.", Toast.LENGTH_SHORT).show()
            
            val user = getCurrentUser()
            channelAdapter.removeUserFromAllChannels(user)
            
            // Stop Service
            VoiceService.stop(this)
            
            layoutVoiceBar.visibility = View.GONE
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is ChannelVoiceFragment) {
                 supportFragmentManager.beginTransaction().remove(currentFragment).commit()
                 toolbar.title = intent.getStringExtra("SERVER_NAME") ?: "Server"
                 supportActionBar?.show()
            }
        }
    }

    override fun onUnlockFileRequested(message: FileMessage, position: Int) {}
    override fun onInviteAction(inviteCode: String, accepted: Boolean, position: Int) {}
    
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

    private fun showMemberDetailsDialog(member: Member) {}
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
