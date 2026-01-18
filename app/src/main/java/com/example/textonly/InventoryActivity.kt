package text.only.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventoryActivity : AppCompatActivity() {

    private lateinit var recyclerInventory: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        val toolbar = findViewById<Toolbar>(R.id.toolbarInventory)
        toolbar.title = "Inventar"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerInventory = findViewById(R.id.recyclerInventory)
        recyclerInventory.layoutManager = LinearLayoutManager(this)
        
        loadInventory()
    }

    private fun loadInventory() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val items = db.storeDao().getInventory()
            recyclerInventory.adapter = InventoryAdapter(items)
        }
    }

    class InventoryAdapter(val items: List<InventoryItem>) : RecyclerView.Adapter<InventoryAdapter.Holder>() {
        class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val text: TextView = v.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(item.acquiredDate))
            holder.text.text = "${item.itemName} (${item.itemType}) - $date"
        }

        override fun getItemCount() = items.size
    }
}
