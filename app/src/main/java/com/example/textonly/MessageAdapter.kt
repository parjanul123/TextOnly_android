package text.only.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val messages: List<ChatMessage>,
    private val listener: MessageInteractionListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_TEXT_SENT = 0
        const val TYPE_TEXT_RECEIVED = 1
        const val TYPE_FILE_SENT = 2
        const val TYPE_FILE_RECEIVED = 3
        const val TYPE_INVITE_SENT = 4
        const val TYPE_INVITE_RECEIVED = 5
        const val TYPE_GIFT_SENT = 6
        const val TYPE_GIFT_RECEIVED = 7
        const val TYPE_EMOTE_SENT = 8
        const val TYPE_EMOTE_RECEIVED = 9
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return when (msg) {
            is TextMessage -> if (msg.isSent) TYPE_TEXT_SENT else TYPE_TEXT_RECEIVED
            is FileMessage -> if (msg.isSent) TYPE_FILE_SENT else TYPE_FILE_RECEIVED
            is InviteMessage -> if (msg.isSent) TYPE_INVITE_SENT else TYPE_INVITE_RECEIVED
            is GiftMessage -> if (msg.isSent) TYPE_GIFT_SENT else TYPE_GIFT_RECEIVED
            is EmoteMessage -> if (msg.isSent) TYPE_EMOTE_SENT else TYPE_EMOTE_RECEIVED
            else -> -1 // Should not happen
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TEXT_SENT -> TextViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false))
            TYPE_TEXT_RECEIVED -> TextViewHolder(inflater.inflate(R.layout.item_message_received, parent, false))
            TYPE_FILE_SENT -> FileViewHolder(inflater.inflate(R.layout.item_message_file_sent, parent, false))
            TYPE_FILE_RECEIVED -> FileViewHolder(inflater.inflate(R.layout.item_message_file_received, parent, false))
            TYPE_INVITE_SENT -> InviteViewHolder(inflater.inflate(R.layout.item_message_invite_sent, parent, false))
            TYPE_INVITE_RECEIVED -> InviteViewHolder(inflater.inflate(R.layout.item_message_invite_received, parent, false))
            TYPE_GIFT_SENT -> GiftViewHolder(inflater.inflate(R.layout.item_message_gift_sent, parent, false))
            TYPE_GIFT_RECEIVED -> GiftViewHolder(inflater.inflate(R.layout.item_message_gift_received, parent, false))
            TYPE_EMOTE_SENT -> EmoteViewHolder(inflater.inflate(R.layout.item_message_emote_sent, parent, false), true)
            TYPE_EMOTE_RECEIVED -> EmoteViewHolder(inflater.inflate(R.layout.item_message_emote_received, parent, false), false)
            else -> TextViewHolder(inflater.inflate(R.layout.item_message_received, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val msg = messages[position]) {
            is TextMessage -> (holder as TextViewHolder).bind(msg)
            is FileMessage -> (holder as FileViewHolder).bind(msg, listener, position)
            is InviteMessage -> (holder as InviteViewHolder).bind(msg, listener, position)
            is GiftMessage -> (holder as GiftViewHolder).bind(msg)
            is EmoteMessage -> (holder as EmoteViewHolder).bind(msg)
        }
    }

    override fun getItemCount() = messages.size

    // --- ViewHolders ---

    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtContent: TextView = itemView.findViewById(R.id.text_message_body)
        fun bind(msg: TextMessage) {
            txtContent.text = msg.content
        }
    }

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtFileName: TextView = itemView.findViewById(R.id.txtFileName)
        private val txtFilePrice: TextView = itemView.findViewById(R.id.txtFilePrice)
        private val btnAction: Button = itemView.findViewById(R.id.btnFileAction)
        private val imgGiftIcon: ImageView = itemView.findViewById(R.id.imgGiftIcon)

        fun bind(msg: FileMessage, listener: MessageInteractionListener, position: Int) {
            txtFileName.text = msg.fileName
            
            if (msg.price > 0) {
                if (msg.priceUnit == "COINS") {
                    txtFilePrice.text = "Preț: ${msg.price} Coins"
                    imgGiftIcon.visibility = View.GONE
                } else {
                    txtFilePrice.text = "Preț: 1x ${msg.priceGiftName}"
                    imgGiftIcon.visibility = View.VISIBLE
                    imgGiftIcon.setImageResource(ResourceMapper.getDrawableId(msg.priceUnit ?: "ic_gift_card"))
                }
                
                if (msg.isSent) {
                    btnAction.text = "Trimis"
                    btnAction.isEnabled = false
                } else {
                    btnAction.text = "Deblochează"
                    btnAction.isEnabled = true
                    btnAction.setOnClickListener { listener.onUnlockFileRequested(msg, position) }
                }
            } else {
                txtFilePrice.text = "Gratuit"
                imgGiftIcon.visibility = View.GONE
                btnAction.text = "Descarcă"
                btnAction.isEnabled = true 
            }
        }
    }

    class InviteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtServerName: TextView = itemView.findViewById(R.id.txtInviteServerName)
        private val btnJoin: Button = itemView.findViewById(R.id.btnJoinServer)
        
        fun bind(msg: InviteMessage, listener: MessageInteractionListener, position: Int) {
            txtServerName.text = "Invitație: ${msg.serverName}"
            
            val isExpired = System.currentTimeMillis() > msg.expiryTimestamp
            
            if (msg.isSent) {
                btnJoin.text = if (isExpired) "Expirată" else "Trimisă"
                btnJoin.isEnabled = false
            } else {
                if (isExpired) {
                    btnJoin.text = "Expirată"
                    btnJoin.isEnabled = false
                } else {
                    btnJoin.text = "Acceptă"
                    btnJoin.isEnabled = true
                    btnJoin.setOnClickListener { listener.onInviteAction(msg.inviteCode, true, position) }
                }
            }
        }
    }

    class GiftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtGiftName: TextView = itemView.findViewById(R.id.txtGiftName)
        private val txtGiftValue: TextView = itemView.findViewById(R.id.txtGiftValue)
        private val imgGift: ImageView = itemView.findViewById(R.id.imgGift)
        
        fun bind(msg: GiftMessage) {
            txtGiftName.text = msg.giftName
            txtGiftValue.text = "Valoare: ${msg.giftValue} Coins"
            imgGift.setImageResource(ResourceMapper.getDrawableId(msg.giftResource))
        }
    }
    
    // --- FIX AICI ---
    class EmoteViewHolder(itemView: View, isSent: Boolean) : RecyclerView.ViewHolder(itemView) {
        // Find by specific ID depending on layout, OR handle nullability safe
        private val imgEmote: ImageView? = if (isSent) {
            itemView.findViewById(R.id.imgEmoteSent)
        } else {
            itemView.findViewById(R.id.imgEmoteReceived)
        }
        
        fun bind(msg: EmoteMessage) {
            imgEmote?.setImageResource(ResourceMapper.getDrawableId(msg.emoteResource))
        }
    }
}
