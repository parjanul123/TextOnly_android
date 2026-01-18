package text.only.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class VoiceUser(val name: String, val avatarRes: Int = R.mipmap.textonly_round)

class VoiceUserAdapter(private val users: List<VoiceUser>) : RecyclerView.Adapter<VoiceUserAdapter.VoiceUserViewHolder>() {

    class VoiceUserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: ImageView = view.findViewById(R.id.imgVoiceUserAvatar)
        val txtName: TextView = view.findViewById(R.id.txtVoiceUserName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoiceUserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_voice_user, parent, false)
        return VoiceUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoiceUserViewHolder, position: Int) {
        val user = users[position]
        holder.txtName.text = user.name
        holder.imgAvatar.setImageResource(user.avatarRes)
    }

    override fun getItemCount() = users.size
}
