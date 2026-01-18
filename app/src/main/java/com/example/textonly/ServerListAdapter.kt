package text.only.app

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ServerListAdapter(
    private var servers: List<Server>,
    private val clickListener: (Server) -> Unit, // Listener pentru click normal
    private val longClickListener: (Server) -> Boolean // Listener pentru click lung
) : RecyclerView.Adapter<ServerListAdapter.ServerViewHolder>() {

    class ServerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val serverName: TextView = view.findViewById(R.id.chatTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ServerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val server = servers[position]
        holder.serverName.text = server.name
        
        // Setăm ambele tipuri de listeneri
        holder.itemView.setOnClickListener { clickListener(server) }
        holder.itemView.setOnLongClickListener { longClickListener(server) }
    }

    override fun getItemCount() = servers.size

    fun updateServers(newServers: List<Server>) {
        servers = newServers
        notifyDataSetChanged()
    }
}
