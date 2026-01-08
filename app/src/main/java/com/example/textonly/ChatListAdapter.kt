package text.only.app


import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatListAdapter(private val chatList: List<Contact>) :
    RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatTitle: TextView = itemView.findViewById(R.id.chatTitle)
        val chatPhone: TextView = itemView.findViewById(R.id.chatPhone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val contact = chatList[position]

        holder.chatTitle.text = contact.name
        holder.chatPhone.text = contact.phone

        // 👉 Când se apasă pe un contact, deschide fereastra de chat
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ChatWindowActivity::class.java)
            intent.putExtra("contact_name", contact.name)
            intent.putExtra("contact_phone", contact.phone)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = chatList.size
}
