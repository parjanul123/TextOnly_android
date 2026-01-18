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
import org.json.JSONArray
import java.io.IOException

class InventoryActivity : AppCompatActivity() {

    private lateinit var recyclerInventory: RecyclerView
    private val client = OkHttpClient()
    private val inventoryItems = mutableListOf<InventoryItem>()
    private lateinit var adapter: InventoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        val toolbar = findViewById<Toolbar>(R.id.toolbarInventory)
        toolbar.title = "Inventar"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerInventory = findViewById(R.id.recyclerInventory)
        recyclerInventory.layoutManager = GridLayoutManager(this, 2)
        
        adapter = InventoryAdapter(inventoryItems, this)
        recyclerInventory.adapter = adapter
        
        // We load local first for speed, assuming sync happens in background or Store
        loadInventoryFromLocal()
    }

    private fun loadInventoryFromLocal() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val items = db.storeDao().getInventory()
            inventoryItems.clear()
            inventoryItems.addAll(items)
            adapter.notifyDataSetChanged()
        }
    }
    
    fun equipFrame(item: InventoryItem) {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("equipped_frame", item.resourceName).apply()
        Toast.makeText(this, "${item.itemName} a fost echipat!", Toast.LENGTH_SHORT).show()
        adapter.notifyDataSetChanged() // Refresh to update button states (Equipped vs Use)
    }
    
    fun addEmoteToKeyboard(item: InventoryItem) {
        Toast.makeText(this, "${item.itemName} adăugat la tastatură!", Toast.LENGTH_SHORT).show()
        // Here you would implement logic to enable this sticker in your Chat Input UI
    }

    class InventoryAdapter(
        val items: List<InventoryItem>,
        val activity: InventoryActivity
    ) : RecyclerView.Adapter<InventoryAdapter.Holder>() {
        
        class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.txtName)
            val type: TextView = v.findViewById(R.id.txtType)
            val img: ImageView = v.findViewById(R.id.imgProduct)
            val btn: Button = v.findViewById(R.id.btnAction)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory_product, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.name.text = item.itemName
            holder.type.text = item.itemType
            
            // Image Logic
            val resId = when(item.resourceName) {
                "ic_rose" -> R.drawable.ic_rose
                "ic_frame_rain" -> R.drawable.ic_frame_rain
                "ic_heart" -> R.drawable.ic_heart
                "ic_rocket" -> R.drawable.ic_rocket
                "ic_gift_card" -> R.drawable.ic_gift_card
                else -> R.drawable.ic_coin_shape
            }
            holder.img.setImageResource(resId)
            
            // Button Logic based on Type
            when(item.itemType) {
                "FRAME" -> {
                    val prefs = activity.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    val currentFrame = prefs.getString("equipped_frame", null)
                    
                    if (currentFrame == item.resourceName) {
                        holder.btn.text = "Echipat"
                        holder.btn.isEnabled = false
                        holder.btn.setBackgroundColor(Color.GRAY)
                    } else {
                        holder.btn.text = "Folosește"
                        holder.btn.isEnabled = true
                        holder.btn.setBackgroundColor(Color.parseColor("#2196F3")) // Blue
                        holder.btn.setOnClickListener { activity.equipFrame(item) }
                    }
                }
                "EMOTICON" -> {
                    holder.btn.text = "Adaugă"
                    holder.btn.isEnabled = true
                    holder.btn.setBackgroundColor(Color.parseColor("#4CAF50")) // Green
                    holder.btn.setOnClickListener { activity.addEmoteToKeyboard(item) }
                }
                "GIFT" -> {
                    // Count how many we have of this type
                    val count = items.count { it.itemName == item.itemName }
                    holder.btn.text = "Deții: $count"
                    holder.btn.isEnabled = false // Consumed in chat, not here
                    holder.btn.setBackgroundColor(Color.LTGRAY)
                }
                else -> {
                    holder.btn.visibility = View.GONE
                }
            }
        }

        override fun getItemCount() = items.size
    }
}
