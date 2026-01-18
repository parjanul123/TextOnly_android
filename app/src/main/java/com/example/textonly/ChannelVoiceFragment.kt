package text.only.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class ChannelVoiceFragment : Fragment() {

    private lateinit var recyclerVoiceUsers: RecyclerView
    private lateinit var adapter: VoiceUserAdapter
    private val voiceUsers = mutableListOf<VoiceUser>()

    // Controls
    private lateinit var layoutVoiceControls: LinearLayout
    private lateinit var layoutHeader: RelativeLayout
    private lateinit var btnSettings: ImageButton
    private lateinit var btnDeafen: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var btnCam: ImageButton
    private lateinit var btnScreenShare: ImageButton
    private lateinit var btnHangUp: ImageButton
    private lateinit var btnStopWatching: ImageButton
    private lateinit var btnGiftVoice: ImageButton
    
    // State
    private var isMicMuted = false
    private var isCamOn = false
    private var isScreenSharing = false
    private var isDeafened = false
    private var wasMicMutedBeforeDeafen = false
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var myName: String = "Eu"
    
    private var currentCameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.CAMERA] == true) {
                if (isCamOn) startCameraForMe()
            } else {
                Toast.makeText(context, "Permisiune cameră refuzată", Toast.LENGTH_SHORT).show()
                isCamOn = false
                updateCamUI()
            }
        }
        
    private val screenShareLauncher = 
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                startScreenSharing(result.data!!)
            } else {
                isScreenSharing = false
                updateScreenShareUI()
                Toast.makeText(context, "Partajare ecran anulată", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_channel_voice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val channelName = arguments?.getString("CHANNEL_NAME")
        view.findViewById<TextView>(R.id.txtVoiceChannelName).text = channelName
        
        recyclerVoiceUsers = view.findViewById(R.id.recyclerVoiceUsers)
        recyclerVoiceUsers.layoutManager = GridLayoutManager(context, 2)
        
        loadCurrentUser()
        
        adapter = VoiceUserAdapter(
            users = voiceUsers,
            myName = myName,
            onBindCamera = { userName, previewView ->
                if (userName == myName && isCamOn) {
                    bindCamera(previewView)
                }
            },
            onFlipCamera = { userName ->
                if (userName == myName) toggleCameraLens()
            },
            onWatchStream = { userName ->
                startWatchingStream(userName)
            }
        )
        recyclerVoiceUsers.adapter = adapter

        layoutVoiceControls = view.findViewById(R.id.layoutVoiceControls)
        layoutHeader = view.findViewById(R.id.layoutHeader)
        btnSettings = view.findViewById(R.id.btnSettings)
        btnDeafen = view.findViewById(R.id.btnDeafen)
        btnMic = view.findViewById(R.id.btnMic)
        btnCam = view.findViewById(R.id.btnCam)
        btnScreenShare = view.findViewById(R.id.btnScreenShare)
        btnHangUp = view.findViewById(R.id.btnHangUp)
        btnStopWatching = view.findViewById(R.id.btnStopWatching)
        btnGiftVoice = view.findViewById(R.id.btnGiftVoice)
        
        setupListeners()
        startVoiceActivitySimulation()
    }
    
    // ... (restul metodelor onPictureInPictureModeChanged, updateButtonLayout etc. rămân la fel, doar adăugăm btnGiftVoice la update)
    
    private fun setupListeners() {
        // ... (altele)
        btnSettings.setOnClickListener { showSettingsDialog() }
        btnDeafen.setOnClickListener { toggleDeafen() }
        btnMic.setOnClickListener { if (!isDeafened) toggleMic() else Toast.makeText(context, "Nu poți porni microfonul în modul Deafen", Toast.LENGTH_SHORT).show() }
        btnCam.setOnClickListener { 
             isCamOn = !isCamOn
            updateCamUI()
            if (isCamOn) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    startCameraForMe()
                } else {
                    requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                }
            } else {
                stopCameraForMe()
            }
        }
        btnScreenShare.setOnClickListener {
             if (!isScreenSharing) {
                val mediaProjectionManager = context?.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                if (mediaProjectionManager != null) {
                    screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                    isScreenSharing = true
                    updateScreenShareUI()
                } else {
                    Toast.makeText(context, "Serviciul nu este disponibil", Toast.LENGTH_SHORT).show()
                }
            } else {
                stopScreenSharing()
            }
        }
        btnStopWatching.setOnClickListener { stopWatchingAllStreams() }
        btnHangUp.setOnClickListener { (activity as? ServerActivity)?.disconnectVoice() }
        
        btnGiftVoice.setOnClickListener { openUserSelectionSideSheet() }
    }
    
    // --- Gift Logic Start ---
    
    private fun openUserSelectionSideSheet() {
        val dialog = AlertDialog.Builder(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert) // Corrected reference
        val view = layoutInflater.inflate(R.layout.dialog_select_users, null)
        dialog.setView(view)
        
        // Simulating a side sheet behavior using Gravity (requires custom dialog window config usually, 
        // but for now standard dialog is fine or we can configure window)
        val alert = dialog.create()
        alert.window?.setGravity(Gravity.START or Gravity.FILL_VERTICAL)
        alert.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerSelectUsers)
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        
        // Filter out self and screen shares
        val potentialRecipients = voiceUsers.filter { it.name != myName && !it.isScreenShare }
        if (potentialRecipients.isEmpty()) {
            Toast.makeText(context, "Nu sunt alți utilizatori aici.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedUsers = mutableSetOf<String>()
        val userAdapter = UserSelectAdapter(potentialRecipients) { name, isSelected ->
            if (isSelected) selectedUsers.add(name) else selectedUsers.remove(name)
        }
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = userAdapter
        
        btnNext.setOnClickListener {
            if (selectedUsers.isEmpty()) {
                Toast.makeText(context, "Selectează cel puțin un utilizator", Toast.LENGTH_SHORT).show()
            } else {
                alert.dismiss()
                openGiftSelectionBottomSheet(selectedUsers.toList())
            }
        }
        
        alert.show()
    }
    
    private fun openGiftSelectionBottomSheet(recipients: List<String>) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_select_gift, null)
        bottomSheet.setContentView(view)
        
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerSelectGift)
        recycler.layoutManager = GridLayoutManager(context, 3)
        
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val allStoreItems = db.storeDao().getAllItems().filter { it.type == "GIFT" || it.type == "CONSUMABLE_EMOTE" }
            val inventory = db.storeDao().getInventory()
            
            // Map store item to (StoreItem, InventoryCount)
            val giftData = allStoreItems.map { storeItem ->
                val count = inventory.count { it.itemName == storeItem.name }
                GiftViewData(storeItem, count)
            }
            
            recycler.adapter = GiftSelectAdapter(giftData) { selectedGiftData ->
                bottomSheet.dismiss()
                processGiftSending(selectedGiftData, recipients)
            }
        }
        
        bottomSheet.show()
    }
    
    private fun processGiftSending(giftData: GiftViewData, recipients: List<String>) {
        val totalNeeded = recipients.size
        val available = giftData.count
        
        if (available >= totalNeeded) {
            // Has enough, send immediately
            consumeAndSend(giftData.item, recipients, buyCount = 0)
        } else {
            // Needs to buy
            val missing = totalNeeded - available
            val cost = missing * giftData.item.price
            
            AlertDialog.Builder(requireContext())
                .setTitle("Stoc insuficient")
                .setMessage("Ai doar $available bucăți. Vrei să cumperi încă $missing bucăți pentru $cost OnlyCoins și să le trimiți?")
                .setPositiveButton("Da, Cumpără și Trimite") { _, _ ->
                     checkWalletAndBuy(giftData.item, missing, cost, recipients, available)
                }
                .setNegativeButton("Anulează", null)
                .show()
        }
    }
    
    private fun checkWalletAndBuy(item: StoreItem, buyCount: Int, cost: Int, recipients: List<String>, alreadyOwnedCount: Int) {
        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val currentCoins = prefs.getInt("userCoins", 0)
        
        if (currentCoins >= cost) {
            // Deduct coins
            prefs.edit().putInt("userCoins", currentCoins - cost).apply()
            
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(requireContext())
                
                // Log purchase
                 db.storeDao().insertTransaction(TransactionLog(
                    description = "Cumpărat automat în voice: ${item.name} x$buyCount",
                    amount = -cost,
                    type = "AUTO_PURCHASE"
                ))
                
                // Note: We don't necessarily need to insert into inventory and then delete.
                // We can just conceptually "buy and use".
                // But for consistency with "consumeAndSend" which might look for inventory to delete...
                // Actually, consumeAndSend will handle deleting the 'alreadyOwnedCount'.
                // The newly bought ones don't exist in inventory table yet, so we just don't delete them, we just "send" them.
                
                consumeAndSend(item, recipients, buyCount = buyCount)
            }
        } else {
             Toast.makeText(context, "Fonduri insuficiente! Ai nevoie de $cost Coins.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun consumeAndSend(item: StoreItem, recipients: List<String>, buyCount: Int) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val inventory = db.storeDao().getInventory().filter { it.itemName == item.name }
            
            // Delete 'available' items from inventory
            // We need to delete exactly (recipients.size - buyCount) items
            val toDeleteCount = recipients.size - buyCount
            
            if (toDeleteCount > 0) {
                // Delete top N items
                inventory.take(toDeleteCount).forEach { invItem ->
                     db.storeDao().deleteInventoryItem(invItem)
                }
            }
            
            // Send logic (Visual feedback + Log)
             db.storeDao().insertTransaction(TransactionLog(
                description = "Gift trimis în Voice către ${recipients.size} persoane: ${item.name}",
                amount = - (item.price * recipients.size), // Value tracking (though some paid with coins just now)
                type = "GIFT_SENT_VOICE"
            ))
            
            activity?.runOnUiThread {
                Toast.makeText(context, "Ai trimis ${item.name} către: ${recipients.joinToString(", ")}", Toast.LENGTH_LONG).show()
                // Here you would trigger an animation or send a WebSocket message to real server
            }
        }
    }

    data class GiftViewData(val item: StoreItem, val count: Int)

    inner class UserSelectAdapter(val users: List<VoiceUser>, val onSelect: (String, Boolean) -> Unit) : RecyclerView.Adapter<UserSelectAdapter.Holder>() {
        inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.txtName)
            val check: CheckBox = v.findViewById(R.id.chkUser)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(layoutInflater.inflate(R.layout.item_user_select, parent, false))
        }
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val user = users[position]
            holder.name.text = user.name
            holder.check.setOnCheckedChangeListener(null)
            holder.check.isChecked = false
            holder.check.setOnCheckedChangeListener { _, isChecked -> onSelect(user.name, isChecked) }
        }
        override fun getItemCount() = users.size
    }

    inner class GiftSelectAdapter(val gifts: List<GiftViewData>, val onSelect: (GiftViewData) -> Unit) : RecyclerView.Adapter<GiftSelectAdapter.Holder>() {
        inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.txtGiftName)
            val count: TextView = v.findViewById(R.id.txtInventoryCount)
            val price: TextView = v.findViewById(R.id.txtPrice)
            val img: ImageView = v.findViewById(R.id.imgGift)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(layoutInflater.inflate(R.layout.item_gift_select, parent, false))
        }
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val data = gifts[position]
            holder.name.text = data.item.name
            holder.count.text = if (data.count > 0) "x${data.count}" else ""
            holder.price.text = "${data.item.price} Coins"
            
            holder.itemView.setOnClickListener { onSelect(data) }
        }
        override fun getItemCount() = gifts.size
    }

    // --- Gift Logic End ---
    
    // ... (restul metodelor existente)
    
    private fun updateButtonLayout(btn: ImageButton, size: Int, marginEnd: Int, padding: Int) {
        if (btn.layoutParams == null) return
        
        val params = btn.layoutParams as? LinearLayout.LayoutParams
        params?.let {
            it.width = size
            it.height = size
            it.marginEnd = marginEnd
            btn.layoutParams = it
            btn.setPadding(padding, padding, padding, padding)
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        if (!isAdded) return 0
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
    
    private fun toggleDeafen() {
        isDeafened = !isDeafened
        if (isDeafened) {
            wasMicMutedBeforeDeafen = isMicMuted
            if (!isMicMuted) toggleMic(forceMute = true)
            
            btnDeafen.setImageResource(R.drawable.ic_headset_off)
            btnDeafen.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            btnDeafen.imageTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
            Toast.makeText(context, "Deafen Activat", Toast.LENGTH_SHORT).show()
        } else {
            if (!wasMicMutedBeforeDeafen) toggleMic(forceMute = false)
            
            btnDeafen.setImageResource(R.drawable.ic_headset)
            btnDeafen.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#444444"))
            btnDeafen.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            Toast.makeText(context, "Deafen Dezactivat", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun toggleMic(forceMute: Boolean? = null) {
        if (forceMute != null) {
            isMicMuted = forceMute
        } else {
            isMicMuted = !isMicMuted
        }
        updateMicUI()
        val index = voiceUsers.indexOfFirst { it.name == myName }
        if (index != -1) {
            voiceUsers[index].isMuted = isMicMuted
            adapter.notifyItemChanged(index, "MUTE")
        }
    }
    
    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_voice_settings, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
            
        val seekOutput = dialogView.findViewById<SeekBar>(R.id.seekOutputVolume)
        val seekMic = dialogView.findViewById<SeekBar>(R.id.seekMicSensitivity)
        val seekShare = dialogView.findViewById<SeekBar>(R.id.seekShareVolume)
        
        val spinnerCamera = dialogView.findViewById<Spinner>(R.id.spinnerCameraQuality)
        val spinnerShare = dialogView.findViewById<Spinner>(R.id.spinnerShareQuality)
        
        val prefs = requireContext().getSharedPreferences("VoiceSettings", Context.MODE_PRIVATE)
        
        seekOutput.progress = prefs.getInt("output_volume", 100)
        seekMic.progress = prefs.getInt("mic_sensitivity", 50)
        seekShare.progress = prefs.getInt("share_volume", 100)
        
        val allResolutions = listOf("144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p")
        val allResValues = listOf(144, 240, 360, 480, 720, 1080, 1440, 2160)
        
        val metrics = resources.displayMetrics
        val screenMin = Math.min(metrics.widthPixels, metrics.heightPixels)
        
        val shareOptions = allResolutions.filterIndexed { index, _ -> 
            allResValues[index] <= screenMin || (index > 0 && allResValues[index-1] < screenMin)
        }
        
        val camAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner, allResolutions)
        camAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCamera.adapter = camAdapter
        
        val shareAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner, shareOptions)
        shareAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerShare.adapter = shareAdapter
        
        val savedCam = prefs.getInt("camera_quality_idx", 4)
        if (savedCam < allResolutions.size) spinnerCamera.setSelection(savedCam)
        
        val savedShare = prefs.getInt("share_quality_idx", shareOptions.size - 1)
        if (savedShare < shareOptions.size) spinnerShare.setSelection(savedShare)
        
        seekMic.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {}
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {
                val db = (s?.progress ?: 50) - 50
                val text = if (db == -50) "Sensibilitate: Oprit" else "Sensibilitate: ${if (db>0) "+" else ""}$db dB"
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
        })
        
        dialogView.findViewById<Button>(R.id.btnCloseSettings).setOnClickListener {
            val editor = prefs.edit()
            editor.putInt("output_volume", seekOutput.progress)
            editor.putInt("mic_sensitivity", seekMic.progress)
            editor.putInt("share_volume", seekShare.progress)
            editor.putInt("camera_quality_idx", spinnerCamera.selectedItemPosition)
            editor.putInt("share_quality_idx", spinnerShare.selectedItemPosition)
            editor.apply()
            
            Toast.makeText(context, "Setări salvate", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun startWatchingStream(userName: String) {
        val index = voiceUsers.indexOfFirst { it.name == userName }
        if (index != -1) {
            voiceUsers[index].isBeingWatched = true
            adapter.notifyItemChanged(index, "WATCH_STATE")
            btnStopWatching.visibility = View.VISIBLE
            Toast.makeText(context, "Urmărești partajarea", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopWatchingAllStreams() {
        voiceUsers.forEachIndexed { index, user ->
            if (user.isBeingWatched) {
                user.isBeingWatched = false
                adapter.notifyItemChanged(index, "WATCH_STATE")
            }
        }
        btnStopWatching.visibility = View.GONE
        Toast.makeText(context, "Nu mai urmărești partajarea", Toast.LENGTH_SHORT).show()
    }
    
    private fun startScreenSharing(data: Intent) {
        val screenShareUser = VoiceUser(
            name = "$myName (Ecran)",
            isScreenShare = true,
            isSpeaking = false,
            isCamOn = false
        )
        voiceUsers.add(screenShareUser)
        adapter.notifyItemInserted(voiceUsers.size - 1)
        Toast.makeText(context, "Partajare ecran pornită", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopScreenSharing() {
        isScreenSharing = false
        updateScreenShareUI()
        val index = voiceUsers.indexOfFirst { it.isScreenShare && it.name.startsWith(myName) }
        if (index != -1) {
            voiceUsers.removeAt(index)
            adapter.notifyItemRemoved(index)
        }
        if (btnStopWatching.visibility == View.VISIBLE) {
             btnStopWatching.visibility = View.GONE
        }
        Toast.makeText(context, "Partajare ecran oprită", Toast.LENGTH_SHORT).show()
    }
    
    private fun startCameraForMe() {
        val index = voiceUsers.indexOfFirst { it.name == myName && !it.isScreenShare }
        if (index != -1) {
            voiceUsers[index].isCamOn = true
            currentCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            adapter.notifyItemChanged(index) 
            Toast.makeText(context, "Camera față activată", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopCameraForMe() {
        val index = voiceUsers.indexOfFirst { it.name == myName && !it.isScreenShare }
        if (index != -1) {
            voiceUsers[index].isCamOn = false
            adapter.notifyItemChanged(index)
            cameraProvider?.unbindAll()
            Toast.makeText(context, "Camera oprită", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun toggleCameraLens() {
        currentCameraSelector = if (currentCameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        val index = voiceUsers.indexOfFirst { it.name == myName && !it.isScreenShare }
        if (index != -1) {
            cameraProvider?.unbindAll()
            adapter.notifyItemChanged(index) 
        }
    }

    private fun bindCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, currentCameraSelector, preview)
            } catch (exc: Exception) { }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    
    private fun startVoiceActivitySimulation() {
        lifecycleScope.launch {
            while (isActive) {
                val prefs = context?.getSharedPreferences("VoiceSettings", Context.MODE_PRIVATE)
                val sensitivity = prefs?.getInt("mic_sensitivity", 50) ?: 50
                
                if (!isMicMuted && !isDeafened) { 
                    val chance = Random.nextInt(100)
                    val isSpeakingNow = chance < sensitivity
                    
                    val index = voiceUsers.indexOfFirst { it.name == myName && !it.isScreenShare }
                    if (index != -1 && voiceUsers[index].isSpeaking != isSpeakingNow) {
                        voiceUsers[index].isSpeaking = isSpeakingNow
                        adapter.notifyItemChanged(index, "SPEAKING")
                    }
                } else {
                     val index = voiceUsers.indexOfFirst { it.name == myName && !it.isScreenShare }
                     if (index != -1 && voiceUsers[index].isSpeaking) {
                         voiceUsers[index].isSpeaking = false
                         adapter.notifyItemChanged(index, "SPEAKING")
                     }
                }
                delay(Random.nextLong(300, 800))
            }
        }
    }
    
    private fun updateMicUI() {
        if (isMicMuted) {
            btnMic.setImageResource(R.drawable.ic_mic_off)
            btnMic.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            btnMic.imageTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
        } else {
            btnMic.setImageResource(R.drawable.ic_mic)
            btnMic.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#444444"))
            btnMic.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
        }
    }
    
    private fun updateCamUI() {
        if (isCamOn) {
            btnCam.setImageResource(R.drawable.ic_videocam)
            btnCam.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            btnCam.imageTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
        } else {
            btnCam.setImageResource(R.drawable.ic_videocam_off)
            btnCam.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#444444"))
            btnCam.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
        }
    }
    
    private fun updateScreenShareUI() {
        if (isScreenSharing) {
            btnScreenShare.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        } else {
            btnScreenShare.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#444444"))
        }
    }
    
    private fun loadCurrentUser() {
        voiceUsers.clear()
        val prefs = context?.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        myName = prefs?.getString("displayName", "Eu") ?: "Eu"
        val myImageUri = prefs?.getString("profileImageUri", null)
        voiceUsers.add(VoiceUser(name = myName, avatarUri = myImageUri))
        
        // Mock other users for demonstration purposes
        // voiceUsers.add(VoiceUser(name = "Andrei", isSpeaking = true))
        // voiceUsers.add(VoiceUser(name = "Maria"))
    }

    companion object {
        fun newInstance(channelName: String): ChannelVoiceFragment {
            val fragment = ChannelVoiceFragment()
            val args = Bundle()
            args.putString("CHANNEL_NAME", channelName)
            fragment.arguments = args
            return fragment
        }
    }
}
