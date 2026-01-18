package text.only.app

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChannelAdapter(
    private val channels: List<Channel>,
    private val onChannelClicked: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private val expandedChannels = mutableSetOf<String>()
    
    // Mapă pentru a ține utilizatorii conectați pe fiecare canal
    private val connectedUsers = mutableMapOf<String, List<VoiceUser>>()

    class ChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val channelIcon: ImageView = view.findViewById(R.id.channelIcon)
        val channelName: TextView = view.findViewById(R.id.channelName)
        val imgExpandArrow: ImageView = view.findViewById(R.id.imgExpandArrow)
        val recyclerConnectedUsers: RecyclerView = view.findViewById(R.id.recyclerConnectedUsers)
        val channelRow: View = view.findViewById(R.id.channelRow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.channelName.text = channel.name
        
        val iconRes = when (channel.type) {
            ChannelType.TEXT -> R.drawable.ic_channel_text
            ChannelType.VOICE -> R.drawable.ic_channel_voice
        }
        holder.channelIcon.setImageResource(iconRes)

        if (channel.type == ChannelType.VOICE) {
            holder.imgExpandArrow.visibility = View.VISIBLE
            
            val isExpanded = expandedChannels.contains(channel.name)
            
            if (isExpanded) {
                holder.imgExpandArrow.rotation = 0f
                holder.recyclerConnectedUsers.visibility = View.VISIBLE
                
                holder.recyclerConnectedUsers.layoutManager = LinearLayoutManager(holder.itemView.context)
                val users = connectedUsers[channel.name] ?: emptyList()
                holder.recyclerConnectedUsers.adapter = ConnectedUsersSmallAdapter(users)
                
            } else {
                holder.imgExpandArrow.rotation = -90f
                holder.recyclerConnectedUsers.visibility = View.GONE
            }

            holder.imgExpandArrow.setOnClickListener {
                toggleExpansion(channel.name, position)
            }

        } else {
            holder.imgExpandArrow.visibility = View.GONE
            holder.recyclerConnectedUsers.visibility = View.GONE
        }

        holder.channelRow.setOnClickListener {
            onChannelClicked(channel)
        }
    }
    
    private fun toggleExpansion(channelName: String, position: Int) {
        if (expandedChannels.contains(channelName)) {
            expandedChannels.remove(channelName)
        } else {
            expandedChannels.add(channelName)
        }
        notifyItemChanged(position)
    }
    
    fun expandChannel(channelName: String) {
        if (!expandedChannels.contains(channelName)) {
            expandedChannels.add(channelName)
            val index = channels.indexOfFirst { it.name == channelName }
            if (index != -1) {
                notifyItemChanged(index)
            }
        }
    }

    fun collapseChannel(channelName: String) {
        if (expandedChannels.contains(channelName)) {
            expandedChannels.remove(channelName)
            val index = channels.indexOfFirst { it.name == channelName }
            if (index != -1) {
                notifyItemChanged(index)
            }
        }
    }
    
    // Metodă pentru a muta un utilizator de pe orice canal pe un canal țintă
    fun moveUserToChannel(user: VoiceUser, targetChannelName: String) {
        // 1. Căutăm utilizatorul în toate celelalte canale și îl ștergem
        for (channelName in connectedUsers.keys.toList()) {
            val users = connectedUsers[channelName] ?: continue
            
            // Verificăm dacă userul există aici (după nume)
            if (users.any { it.name == user.name }) {
                // Îl ștergem
                val updatedList = users.filter { it.name != user.name }
                connectedUsers[channelName] = updatedList
                
                // Notificăm update pe rândul respectiv
                val index = channels.indexOfFirst { it.name == channelName }
                if (index != -1) notifyItemChanged(index)
                
                // Colapsăm automat canalul vechi (logică duplicată dar sigură)
                if (expandedChannels.contains(channelName)) {
                    expandedChannels.remove(channelName)
                }
            }
        }

        // 2. Adăugăm utilizatorul în canalul nou
        val currentList = connectedUsers[targetChannelName] ?: emptyList()
        if (currentList.none { it.name == user.name }) {
            connectedUsers[targetChannelName] = currentList + user
            
            // Expandăm canalul nou
            expandedChannels.add(targetChannelName)
            
            val index = channels.indexOfFirst { it.name == targetChannelName }
            if (index != -1) notifyItemChanged(index)
        }
    }
    
    fun removeUserFromAllChannels(user: VoiceUser) {
        for (channelName in connectedUsers.keys.toList()) {
            val users = connectedUsers[channelName] ?: continue
            if (users.any { it.name == user.name }) {
                val updatedList = users.filter { it.name != user.name }
                connectedUsers[channelName] = updatedList
                
                // Colapsăm la deconectare
                if (expandedChannels.contains(channelName)) {
                    expandedChannels.remove(channelName)
                }
                
                val index = channels.indexOfFirst { it.name == channelName }
                if (index != -1) notifyItemChanged(index)
            }
        }
    }
    
    fun updateConnectedUsers(channelName: String, users: List<VoiceUser>) {
        connectedUsers[channelName] = users
        val index = channels.indexOfFirst { it.name == channelName }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    override fun getItemCount() = channels.size

    class ConnectedUsersSmallAdapter(private val users: List<VoiceUser>) : 
        RecyclerView.Adapter<ConnectedUsersSmallAdapter.SmallUserViewHolder>() {

        class SmallUserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgAvatar: ImageView = view.findViewById(R.id.imgSmallAvatar)
            val txtName: TextView = view.findViewById(R.id.txtSmallName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmallUserViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_connected_user_small, parent, false)
            return SmallUserViewHolder(view)
        }

        override fun onBindViewHolder(holder: SmallUserViewHolder, position: Int) {
            val user = users[position]
            holder.txtName.text = user.name
            
            if (user.avatarUri != null) {
                 try {
                    holder.imgAvatar.setImageURI(Uri.parse(user.avatarUri))
                } catch (e: Exception) {
                    holder.imgAvatar.setImageResource(user.avatarRes)
                }
            } else {
                holder.imgAvatar.setImageResource(user.avatarRes)
            }
        }

        override fun getItemCount() = users.size
    }
}
