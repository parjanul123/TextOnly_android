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

class TransactionsActivity : AppCompatActivity() {

    private lateinit var recyclerTransactions: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        val toolbar = findViewById<Toolbar>(R.id.toolbarTransactions)
        toolbar.title = "Jurnal Tranzacții"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerTransactions = findViewById(R.id.recyclerTransactions)
        recyclerTransactions.layoutManager = LinearLayoutManager(this)
        
        loadTransactions()
    }

    private fun loadTransactions() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val logs = db.storeDao().getTransactions()
            recyclerTransactions.adapter = TransactionAdapter(logs)
        }
    }

    class TransactionAdapter(val logs: List<TransactionLog>) : RecyclerView.Adapter<TransactionAdapter.Holder>() {
        class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val text: TextView = v.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val log = logs[position]
            val date = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(log.timestamp))
            val amountStr = if (log.amount > 0) "+${log.amount}" else "${log.amount}"
            holder.text.text = "$date: ${log.description} ($amountStr Coins)"
        }

        override fun getItemCount() = logs.size
    }
}
