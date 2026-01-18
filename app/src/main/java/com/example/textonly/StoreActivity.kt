package text.only.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class StoreActivity : AppCompatActivity() {

    private lateinit var recyclerStoreItems: RecyclerView
    private lateinit var txtUserBalance: TextView
    private lateinit var txtCategoryTitle: TextView
    
    private lateinit var btnCatEmotes: Button
    private lateinit var btnCatGifts: Button
    private lateinit var btnCatFrames: Button

    private var userCoins = 0
    private val allStoreItems = mutableListOf<StoreItem>()
    private val displayedItems = mutableListOf<StoreItem>()
    // We need a set of owned item names to check against
    private val ownedItemNames = mutableSetOf<String>()
    
    private lateinit var adapter: StoreAdapter
    
    private var currentCategory = "EMOTICON" // Default
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        val toolbar = findViewById<Toolbar>(R.id.toolbarStore)
        toolbar.title = "Magazin (Online)"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerStoreItems = findViewById(R.id.recyclerStoreItems)
        txtUserBalance = findViewById(R.id.txtUserBalance)
        txtCategoryTitle = findViewById(R.id.txtCategoryTitle)
        
        btnCatEmotes = findViewById(R.id.btnCatEmotes)
        btnCatGifts = findViewById(R.id.btnCatGifts)
        btnCatFrames = findViewById(R.id.btnCatFrames)

        recyclerStoreItems.layoutManager = GridLayoutManager(this, 2)
        adapter = StoreAdapter(displayedItems, ownedItemNames)
        recyclerStoreItems.adapter = adapter
        
        setupCategoryButtons()
        
        // Initial load
        loadBalance()
        loadItemsFromServer()
    }
    
    override fun onResume() {
        super.onResume()
        loadBalance()
        refreshOwnedItems() // Refresh ownership when coming back
    }
    
    private fun refreshOwnedItems() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val inventory = db.storeDao().getInventory()
            ownedItemNames.clear()
            ownedItemNames.addAll(inventory.map { it.itemName })
            adapter.notifyDataSetChanged()
        }
    }
    
    private fun setupCategoryButtons() {
        btnCatEmotes.setOnClickListener { filterItems("EMOTICON"); updateButtonStyles(btnCatEmotes) }
        btnCatGifts.setOnClickListener { filterItems("GIFT"); updateButtonStyles(btnCatGifts) }
        btnCatFrames.setOnClickListener { filterItems("FRAME"); updateButtonStyles(btnCatFrames) }
        
        // Init style
        updateButtonStyles(btnCatEmotes)
    }
    
    private fun updateButtonStyles(activeBtn: Button) {
        val inactiveColor = Color.parseColor("#E0E0E0")
        val activeColor = Color.parseColor("#FF9800") // Orange
        
        btnCatEmotes.setBackgroundColor(inactiveColor)
        btnCatGifts.setBackgroundColor(inactiveColor)
        btnCatFrames.setBackgroundColor(inactiveColor)
        
        activeBtn.setBackgroundColor(activeColor)
    }

    private fun loadBalance() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        userCoins = prefs.getInt("userCoins", 0)
        txtUserBalance.text = "$userCoins"
    }

    private fun loadItemsFromServer() {
        val request = Request.Builder()
            .url(Config.STORE_ITEMS_URL)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loadItemsFromLocalDb()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string()
                    if (jsonStr != null) {
                        try {
                            val jsonArray = JSONArray(jsonStr)
                            val items = mutableListOf<StoreItem>()
                            
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                items.add(StoreItem(
                                    id = obj.optInt("id", 0),
                                    name = obj.getString("name"),
                                    type = obj.getString("type"),
                                    price = obj.getInt("price"),
                                    resourceName = obj.optString("resourceName", "ic_coin_shape")
                                ))
                            }
                            
                            runOnUiThread {
                                allStoreItems.clear()
                                allStoreItems.addAll(items)
                                refreshOwnedItems() 
                                filterItems(currentCategory)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    runOnUiThread { 
                        loadItemsFromLocalDb()
                    }
                }
            }
        })
    }
    
    private fun loadItemsFromLocalDb() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            var items = db.storeDao().getAllItems()
            
            // --- FIX FOR PHONE 2: SEED IF EMPTY ---
            if (items.isEmpty()) {
                seedDefaultItems(db)
                items = db.storeDao().getAllItems() // Reload after seeding
            }
            // --------------------------------------
            
            allStoreItems.clear()
            allStoreItems.addAll(items)
            refreshOwnedItems()
            filterItems(currentCategory)
        }
    }
    
    private suspend fun seedDefaultItems(db: AppDatabase) {
        val defaults = listOf(
            // FRAMES
            StoreItem(name = "Rama Neon", type = "FRAME", price = 5, resourceName = "ic_coin_shape"),
            StoreItem(name = "Rama Ploaie", type = "FRAME", price = 8, resourceName = "ic_frame_rain"),
            StoreItem(name = "Rama Foc", type = "FRAME", price = 10, resourceName = "frame_fire"),
            
            // EMOTES
            StoreItem(name = "Happy", type = "EMOTICON", price = 2, resourceName = "emote_happy"),
            StoreItem(name = "Cool Cat", type = "EMOTICON", price = 3, resourceName = "emote_cat"),
            StoreItem(name = "Sad", type = "EMOTICON", price = 1, resourceName = "emote_sad"),
            
            // GIFTS
            StoreItem(name = "Trandafir", type = "GIFT", price = 1, resourceName = "ic_rose"),
            StoreItem(name = "Racheta", type = "GIFT", price = 5, resourceName = "ic_rocket"),
            StoreItem(name = "Inimioara", type = "GIFT", price = 2, resourceName = "ic_heart"),
            StoreItem(name = "Gift Surpriza", type = "GIFT", price = 10, resourceName = "ic_gift_card")
        )
        
        defaults.forEach { item ->
            db.storeDao().insertItem(item)
        }
    }
    
    private fun filterItems(category: String) {
        currentCategory = category
        displayedItems.clear()
        
        val filtered = allStoreItems.filter { it.type == category }
        displayedItems.addAll(filtered)
        
        // Title update
        when(category) {
            "EMOTICON" -> txtCategoryTitle.text = "Emotes (Permanente)"
            "GIFT" -> txtCategoryTitle.text = "Gifts (De trimis în chat)"
            "FRAME" -> txtCategoryTitle.text = "Rame Avatar (Decorative)"
        }
        
        adapter.notifyDataSetChanged()
    }
    
    private fun buyItem(item: StoreItem) {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val phoneNumber = prefs.getString("phoneNumber", "") ?: ""
        
        val json = JSONObject()
        json.put("phoneNumber", phoneNumber)
        json.put("itemId", item.id)
        json.put("itemName", item.name)
        
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(Config.BUY_ITEM_URL)
            .post(body)
            .build()
            
        Toast.makeText(this, "Se procesează...", Toast.LENGTH_SHORT).show()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@StoreActivity, "Server indisponibil. Încercare locală...", Toast.LENGTH_SHORT).show()
                    processLocalPurchase(item)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@StoreActivity, "Cumpărat cu succes!", Toast.LENGTH_SHORT).show()
                        processLocalPurchase(item, skipValidation = true)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@StoreActivity, "Server nu răspunde (Err ${response.code}). Încercare locală...", Toast.LENGTH_SHORT).show()
                        processLocalPurchase(item)
                    }
                }
            }
        })
    }
    
    private fun processLocalPurchase(item: StoreItem, skipValidation: Boolean = false) {
        if (!skipValidation) {
            if (userCoins < item.price) {
                Toast.makeText(this, "Fonduri insuficiente!", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        finalizeLocalPurchase(item)
    }
    
    private fun finalizeLocalPurchase(item: StoreItem) {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        userCoins -= item.price
        txtUserBalance.text = "$userCoins"
        prefs.edit().putInt("userCoins", userCoins).apply()
        
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            
            db.storeDao().insertInventoryItem(InventoryItem(
                itemName = item.name,
                itemType = item.type,
                resourceName = item.resourceName
            ))
            
            db.storeDao().insertTransaction(TransactionLog(
                description = "Cumpărat: ${item.name}",
                amount = -item.price,
                type = "PURCHASE"
            ))
            
            // Auto-equip removed to allow manual equip from Inventory as requested
            // But we must refresh owned list so button updates to "Owned"
            refreshOwnedItems()
        }
    }

    inner class StoreAdapter(
        val items: List<StoreItem>,
        val ownedNames: Set<String>
    ) : RecyclerView.Adapter<StoreAdapter.StoreHolder>() {
        
        inner class StoreHolder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.txtName)
            val price: TextView = v.findViewById(R.id.txtPrice)
            val btnBuy: Button = v.findViewById(R.id.btnBuy)
            val img: ImageView = v.findViewById(R.id.imgProduct)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_store_product, parent, false)
            return StoreHolder(view)
        }

        override fun onBindViewHolder(holder: StoreHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.name
            holder.price.text = "${item.price}" 
            
            val resId = when(item.resourceName) {
                "ic_rose" -> R.drawable.ic_rose
                "ic_frame_rain" -> R.drawable.ic_frame_rain
                "ic_heart" -> R.drawable.ic_heart
                "ic_rocket" -> R.drawable.ic_rocket
                "ic_gift_card" -> R.drawable.ic_gift_card
                else -> R.drawable.ic_coin_shape
            }
            holder.img.setImageResource(resId)
            
            // OWNERSHIP CHECK
            val isOwned = ownedNames.contains(item.name)
            val isPermanent = item.type == "FRAME" || item.type == "EMOTICON"
            
            if (isPermanent && isOwned) {
                holder.btnBuy.text = "Deținut"
                holder.btnBuy.isEnabled = false
                holder.btnBuy.setBackgroundColor(Color.GRAY)
                holder.btnBuy.setOnClickListener(null)
            } else {
                holder.btnBuy.text = "Cumpără"
                holder.btnBuy.isEnabled = true
                holder.btnBuy.setBackgroundColor(Color.parseColor("#FF9800")) // Orange default
                holder.btnBuy.setOnClickListener { buyItem(item) }
            }
        }

        override fun getItemCount() = items.size
    }
}
