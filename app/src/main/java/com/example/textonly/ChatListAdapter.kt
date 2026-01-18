package text.only.app

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatListAdapter(
    private val list: List<Contact>
) : RecyclerView.Adapter<ChatListAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.chatTitle)
        // val phone: TextView = v.findViewById(R.id.chatPhone) // Comentat deoarece nu mai există în layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = list[position]
        holder.name.text = c.name
        // holder.phone.text = c.phone // Comentat deoarece nu mai există

        holder.itemView.setOnClickListener {
            val ctx = holder.itemView.context
            ctx.startActivity(
                Intent(ctx, ChatWindowActivity::class.java).apply {
                    putExtra("contact_name", c.name)
                    putExtra("contact_phone", c.phone)
                }
            )
        }
    }

    override fun getItemCount() = list.size
}
