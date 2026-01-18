package text.only.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChannelAdapter(private val channels: List<Channel>) :
    RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    class ChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val channelIcon: ImageView = view.findViewById(R.id.channelIcon)
        val channelName: TextView = view.findViewById(R.id.channelName)
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
            ChannelType.TEXT -> R.drawable.ic_text_channel // Va trebui să adaugi aceste iconițe
            ChannelType.VOICE -> R.drawable.ic_voice_channel // Va trebui să adaugi aceste iconițe
        }
        holder.channelIcon.setImageResource(iconRes)
    }

    override fun getItemCount() = channels.size
}
