package text.only.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactSelectAdapter(
    private var contacts: List<Contact>,
    private val onContactClicked: (Contact) -> Unit
) : RecyclerView.Adapter<ContactSelectAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.chatTitle)
        // val phone: TextView = view.findViewById(R.id.chatPhone) // Comentat deoarece nu mai există
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name
        // holder.phone.text = contact.phone // Comentat deoarece nu mai există
        holder.itemView.setOnClickListener { onContactClicked(contact) }
    }

    override fun getItemCount() = contacts.size
    
    fun updateContacts(newContacts: List<Contact>) {
        this.contacts = newContacts
        notifyDataSetChanged()
    }
}
