package text.only.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class StoreActivity : AppCompatActivity() {

    private lateinit var recyclerStoreItems: RecyclerView
    private lateinit var txtUserBalance: TextView
    private var userCoins = 0
    private val storeItems = mutableListOf<StoreItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        val toolbar = findViewById<Toolbar>(R.id.toolbarStore)
        toolbar.title = "Magazin"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerStoreItems = findViewById(R.id.recyclerStoreItems)
        txtUserBalance = findViewById(R.id.txtUserBalance)
        recyclerStoreItems.layoutManager = GridLayoutManager(this, 2)
        
        loadBalance()
        loadItems()
    }

    private fun loadBalance() {
        // Mock balance logic using SharedPreferences
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        userCoins = prefs.getInt("userCoins", 500) // Default 500 coins
        txtUserBalance.text = "Balanță: $userCoins OnlyCoins"
    }
    
    private fun updateBalance(newBalance: Int) {
        userCoins = newBalance
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("userCoins", userCoins).apply()
        txtUserBalance.text = "Balanță: $userCoins OnlyCoins"
    }

    private fun loadItems() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            var items = db.storeDao().getAllItems()
            
            if (items.isEmpty()) {
                // Initialize default items if DB is empty
                val defaults = listOf(
                    StoreItem(name = "Neon Blue Frame", type = "FRAME", price = 100, resourceName = "frame_blue"),
                    StoreItem(name = "Fire Aura", type = "FRAME", price = 250, resourceName = "frame_fire"),
                    StoreItem(name = "Happy Emote", type = "EMOTICON", price = 50, resourceName = "emote_happy"),
                    StoreItem(name = "Cool Cat", type = "EMOTICON", price = 50, resourceName = "emote_cat"),
                    StoreItem(name = "Golden Gift", type = "GIFT", price = 500, resourceName = "gift_gold")
                )
                defaults.forEach { db.storeDao().insertItem(it) }
                items = db.storeDao().getAllItems()
            }
            
            storeItems.clear()
            storeItems.addAll(items)
            recyclerStoreItems.adapter = StoreAdapter(storeItems)
        }
    }
    
    private fun buyItem(item: StoreItem) {
        if (userCoins >= item.price) {
            updateBalance(userCoins - item.price)
            
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                
                // Add to Inventory
                db.storeDao().insertInventoryItem(InventoryItem(
                    itemName = item.name,
                    itemType = item.type,
                    resourceName = item.resourceName
                ))
                
                // Log Transaction
                db.storeDao().insertTransaction(TransactionLog(
                    description = "Cumpărat: ${item.name}",
                    amount = -item.price,
                    type = "PURCHASE"
                ))
                
                Toast.makeText(this@StoreActivity, "Ai cumpărat ${item.name}!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Fonduri insuficiente!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun giftItem(item: StoreItem) {
         if (userCoins >= item.price) {
             val input = android.widget.EditText(this)
             input.hint = "Nume prieten"
             
             AlertDialog.Builder(this)
                 .setTitle("Trimite Gift: ${item.name}")
                 .setView(input)
                 .setPositiveButton("Trimite") { _, _ ->
                     val friendName = input.text.toString()
                     if (friendName.isNotEmpty()) {
                        processGift(item, friendName)
                     }
                 }
                 .setNegativeButton("Anulează", null)
                 .show()
         } else {
             Toast.makeText(this, "Fonduri insuficiente!", Toast.LENGTH_SHORT).show()
         }
    }
    
    private fun processGift(item: StoreItem, friendName: String) {
        updateBalance(userCoins - item.price)
        lifecycleScope.launch {
             val db = AppDatabase.getInstance(applicationContext)
             // Log Transaction
             db.storeDao().insertTransaction(TransactionLog(
                description = "Gift trimis către $friendName: ${item.name}",
                amount = -item.price,
                type = "GIFT_SENT"
             ))
             Toast.makeText(this@StoreActivity, "Gift trimis lui $friendName!", Toast.LENGTH_SHORT).show()
        }
    }

    inner class StoreAdapter(val items: List<StoreItem>) : RecyclerView.Adapter<StoreAdapter.StoreHolder>() {
        
        inner class StoreHolder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.txtName)
            val price: TextView = v.findViewById(R.id.txtPrice)
            val btnBuy: Button = v.findViewById(R.id.btnBuy)
            val btnGift: Button = v.findViewById(R.id.btnGift)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_store_product, parent, false)
            return StoreHolder(view)
        }

        override fun onBindViewHolder(holder: StoreHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.name
            holder.price.text = "${item.price} OnlyCoins"
            
            holder.btnBuy.setOnClickListener { buyItem(item) }
            holder.btnGift.setOnClickListener { giftItem(item) }
        }

        override fun getItemCount() = items.size
    }
}
