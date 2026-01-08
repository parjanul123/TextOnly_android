package text.only.app


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GiftAdapter(
    private val gifts: List<Gift>,
    private val onGiftClick: (Gift) -> Unit
) : RecyclerView.Adapter<GiftAdapter.GiftViewHolder>() {

    inner class GiftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val giftIcon: ImageView = itemView.findViewById(R.id.giftIcon)
        val giftName: TextView = itemView.findViewById(R.id.giftName)
        val giftPrice: TextView = itemView.findViewById(R.id.giftPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GiftViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gift, parent, false)
        return GiftViewHolder(view)
    }

    override fun onBindViewHolder(holder: GiftViewHolder, position: Int) {
        val gift = gifts[position]
        holder.giftIcon.setImageResource(gift.iconRes)
        holder.giftName.text = gift.name
        holder.giftPrice.text = "${gift.price} 🪙"

        holder.itemView.setOnClickListener {
            onGiftClick(gift)
        }
    }

    override fun getItemCount(): Int = gifts.size
}
