package text.only.app

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private lateinit var recyclerMessages: RecyclerView
    private lateinit var inputMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnAttach: ImageButton
    private lateinit var btnGift: ImageButton
    private lateinit var messageAdapter: MessageAdapter
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var contextId: String 

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { showSetPriceDialog(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        contextId = arguments?.getString("CONTEXT_ID") ?: "default_context"
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerMessages = view.findViewById(R.id.recycler_chat_messages)
        inputMessage = view.findViewById(R.id.input_chat_message)
        btnSend = view.findViewById(R.id.btn_send_message)
        btnAttach = view.findViewById(R.id.btn_attach_file)
        btnGift = view.findViewById(R.id.btn_gift)

        setupRecyclerView()
        loadMessagesFromDb()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(chatMessages, requireActivity() as MessageInteractionListener)
        recyclerMessages.layoutManager = LinearLayoutManager(context)
        recyclerMessages.adapter = messageAdapter
    }
    
    private fun loadMessagesFromDb() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val messages = db.messageDao().getMessagesForContext(contextId)
            chatMessages.clear()
            
            chatMessages.addAll(messages.map { entity ->
                when (entity.type) {
                    "FILE" -> FileMessage(
                        fileName = entity.fileName ?: "Unknown",
                        fileType = null,
                        price = entity.filePrice ?: 0,
                        priceUnit = entity.filePriceUnit,
                        priceGiftName = entity.filePriceGiftName,
                        isSent = entity.isSent
                    )
                    "INVITE" -> InviteMessage(
                        serverName = entity.inviteServerName ?: "Server",
                        inviterName = "Eu",
                        inviteCode = entity.inviteCode ?: "",
                        expiryTimestamp = entity.inviteExpiry ?: (System.currentTimeMillis() + 7200000),
                        isSent = entity.isSent
                    )
                    "GIFT" -> GiftMessage(
                        giftName = entity.giftName ?: "Gift",
                        giftValue = entity.giftValue ?: 0,
                        giftResource = entity.giftResource ?: "ic_gift_card",
                        isSent = entity.isSent
                    )
                    else -> TextMessage(entity.content, entity.isSent)
                }
            })
            
            messageAdapter.notifyDataSetChanged()
            if (chatMessages.isNotEmpty()) {
                recyclerMessages.scrollToPosition(chatMessages.size - 1)
            }
        }
    }

    private fun setupClickListeners() {
        btnAttach.setOnClickListener { filePickerLauncher.launch("*/*") }
        
        btnGift.setOnClickListener { showGiftSelectionDialog() }

        btnSend.setOnClickListener {
            val messageText = inputMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                lifecycleScope.launch {
                    val newMessage = MessageEntity(contextId = contextId, content = messageText, isSent = true, type = "TEXT")
                    AppDatabase.getInstance(requireContext()).messageDao().insert(newMessage)
                    loadMessagesFromDb() 
                    inputMessage.text.clear()
                }
            }
        }
    }

    // --- New Gift Selection Logic (Visual) ---
    private fun showGiftSelectionDialog() {
        // Use BottomSheet with Visuals (Reuse logic from ChannelVoiceFragment style)
        val bottomSheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_select_gift, null)
        bottomSheet.setContentView(view)
        
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerSelectGift)
        recycler.layoutManager = GridLayoutManager(context, 3)
        
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            // Only consumables
            val allStoreItems = db.storeDao().getAllItems().filter { it.type == "GIFT" || it.type == "CONSUMABLE_EMOTE" }
            val inventory = db.storeDao().getInventory()
            
            // Map store item to (StoreItem, InventoryCount)
            val giftData = allStoreItems.map { storeItem ->
                val count = inventory.count { it.itemName == storeItem.name }
                GiftViewData(storeItem, count)
            }
            
            // Only show items we HAVE at least 1 of (or show all and disable? User said "send from inventory")
            // Let's show all so they know they can buy more, but only enable sending if count > 0 or auto-buy logic.
            // For Chat, let's keep it simple: Show all, handle logic.
            
            recycler.adapter = GiftSelectAdapter(giftData) { selectedGiftData ->
                bottomSheet.dismiss()
                processGiftSending(selectedGiftData)
            }
        }
        
        bottomSheet.show()
    }
    
    private fun processGiftSending(giftData: GiftViewData) {
        if (giftData.count > 0) {
            // Send directly
            consumeAndSend(giftData.item)
        } else {
            // Need to buy?
            AlertDialog.Builder(requireContext())
                .setTitle("Nu deții acest Gift")
                .setMessage("Vrei să cumperi ${giftData.item.name} pentru ${giftData.item.price} Coins și să îl trimiți?")
                .setPositiveButton("Cumpără și Trimite") { _, _ ->
                    // Simplified buy logic
                    checkWalletAndBuy(giftData.item)
                }
                .setNegativeButton("Anulează", null)
                .show()
        }
    }
    
    private fun checkWalletAndBuy(item: StoreItem) {
        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val coins = prefs.getInt("userCoins", 0)
        if (coins >= item.price) {
            prefs.edit().putInt("userCoins", coins - item.price).apply()
            // Log tx
            consumeAndSend(item)
        } else {
            Toast.makeText(context, "Fonduri insuficiente", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun consumeAndSend(item: StoreItem) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            // Remove one from inventory if exists
            val inventoryItem = db.storeDao().getInventory().firstOrNull { it.itemName == item.name }
            if (inventoryItem != null) {
                db.storeDao().deleteInventoryItem(inventoryItem)
            }
            
            // Insert Message
            val newMessage = MessageEntity(
                contextId = contextId,
                content = "GIFT: ${item.name}",
                isSent = true,
                type = "GIFT",
                giftName = item.name,
                giftValue = item.price,
                giftResource = item.resourceName
            )
            db.messageDao().insert(newMessage)
            
            loadMessagesFromDb()
        }
    }

    // --- New File Price Logic (Visual) ---
    private fun showSetPriceDialog(fileUri: Uri) {
        val fileName = getFileName(fileUri)
        
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_set_file_price, null)
        builder.setView(view)
        val dialog = builder.create()
        
        val radioCoins = view.findViewById<RadioButton>(R.id.radioCoins)
        val radioGift = view.findViewById<RadioButton>(R.id.radioGift)
        val containerCoins = view.findViewById<View>(R.id.containerCoins)
        val containerGifts = view.findViewById<View>(R.id.containerGifts)
        val inputPriceCoins = view.findViewById<EditText>(R.id.inputPriceCoins)
        val recyclerGifts = view.findViewById<RecyclerView>(R.id.recyclerGiftTypes)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmPrice)
        
        var selectedGiftItem: StoreItem? = null
        
        // Setup Gift Recycler
        recyclerGifts.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val giftTypes = db.storeDao().getAllItems().filter { it.type == "GIFT" }
            
            recyclerGifts.adapter = SimpleGiftTypeAdapter(giftTypes) { item ->
                selectedGiftItem = item
                // Highlight selection logic if needed
            }
        }
        
        radioCoins.setOnCheckedChangeListener { _, isChecked ->
            containerCoins.visibility = if (isChecked) View.VISIBLE else View.GONE
            containerGifts.visibility = if (isChecked) View.GONE else View.VISIBLE
        }
        
        btnConfirm.setOnClickListener {
            if (radioCoins.isChecked) {
                val priceStr = inputPriceCoins.text.toString()
                val price = if (priceStr.isNotEmpty()) priceStr.toInt() else 0
                sendFile(fileUri, price, "COINS", null)
            } else {
                if (selectedGiftItem != null) {
                    sendFile(fileUri, 1, selectedGiftItem!!.resourceName, selectedGiftItem!!.name)
                } else {
                    Toast.makeText(context, "Selectează un tip de Gift", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun sendFile(fileUri: Uri, price: Int, priceUnit: String, priceGiftName: String?) {
        val fileName = getFileName(fileUri)
        lifecycleScope.launch {
            val newMessage = MessageEntity(
                contextId = contextId,
                content = "File: $fileName",
                isSent = true,
                type = "FILE",
                fileName = fileName,
                filePrice = price,
                filePriceUnit = priceUnit,
                filePriceGiftName = priceGiftName
            )
            AppDatabase.getInstance(requireContext()).messageDao().insert(newMessage)
            loadMessagesFromDb()
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (it != null && it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "unknown_file"
    }
    
    // --- Adapters ---
    data class GiftViewData(val item: StoreItem, val count: Int)
    
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
            holder.img.setImageResource(ResourceMapper.getDrawableId(data.item.resourceName))
            holder.itemView.setOnClickListener { onSelect(data) }
        }
        override fun getItemCount() = gifts.size
    }
    
    inner class SimpleGiftTypeAdapter(val items: List<StoreItem>, val onSelect: (StoreItem) -> Unit) : RecyclerView.Adapter<SimpleGiftTypeAdapter.Holder>() {
        inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.txtGiftName)
            val img: ImageView = v.findViewById(R.id.imgGift)
            // Reuse item_gift_select but hide price/count
            val price: TextView = v.findViewById(R.id.txtPrice)
            val count: TextView = v.findViewById(R.id.txtInventoryCount)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(layoutInflater.inflate(R.layout.item_gift_select, parent, false))
        }
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.name.text = item.name
            holder.img.setImageResource(ResourceMapper.getDrawableId(item.resourceName))
            holder.price.visibility = View.GONE
            holder.count.visibility = View.GONE
            
            holder.itemView.setOnClickListener { 
                // Visual feedback logic (simple alpha change for demo)
                holder.itemView.alpha = 0.5f 
                onSelect(item) 
            }
        }
        override fun getItemCount() = items.size
    }
    
    companion object {
        fun newInstance(contextId: String): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle()
            args.putString("CONTEXT_ID", contextId)
            fragment.arguments = args
            return fragment
        }
    }
}
