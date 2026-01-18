package text.only.app

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ServerListAdapter(private var servers: List<Server>) :
    RecyclerView.Adapter<ServerListAdapter.ServerViewHolder>() {

    class ServerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val serverName: TextView = view.findViewById(R.id.chatTitle) // Refolosim ID-ul din item_chat.xml
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        // Putem refolosi layout-ul `item_chat` pentru că are un titlu simplu
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ServerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val server = servers[position]
        holder.serverName.text = server.name
        
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ServerActivity::class.java).apply {
                putExtra("SERVER_NAME", server.name)
                putExtra("SERVER_ID", server.id)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = servers.size

    fun updateServers(newServers: List<Server>) {
        servers = newServers
        notifyDataSetChanged()
    }
}
