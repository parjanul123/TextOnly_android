package text.only.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MemberAdapter(
    private val members: List<Member>,
    private val onMemberClicked: (Member) -> Unit // Funcție apelată la click
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val memberName: TextView = view.findViewById(R.id.memberName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        holder.memberName.text = member.name
        
        // Setăm acțiunea de click pentru întregul rând
        holder.itemView.setOnClickListener {
            onMemberClicked(member)
        }
    }

    override fun getItemCount() = members.size
}
