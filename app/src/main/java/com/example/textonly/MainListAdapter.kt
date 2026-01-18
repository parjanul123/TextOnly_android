package text.only.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MainListAdapter(
    private var items: List<MainListItem>,
    private val clickListener: (MainListItem) -> Unit,
    private val longClickListener: (MainListItem) -> Boolean
) : RecyclerView.Adapter<MainListAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.chatTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        
        val name = when (item) {
            is MainListItem.ServerItem -> item.server.name
            is MainListItem.ConversationItem -> item.conversation.contactName
        }
        holder.title.text = name

        holder.itemView.setOnClickListener { clickListener(item) }
        holder.itemView.setOnLongClickListener { longClickListener(item) }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<MainListItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
