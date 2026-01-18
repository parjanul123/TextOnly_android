package text.only.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val contextId: String, 
    val content: String,
    val isSent: Boolean,
    val timestamp: Long = System.currentTimeMillis(),

    val type: String = "TEXT", // TEXT, FILE, INVITE, GIFT

    // File specific
    val fileName: String? = null,
    val filePrice: Int? = null,
    val filePriceUnit: String? = null, // "COINS" or resourceName (e.g. "ic_rose")
    val filePriceGiftName: String? = null, // e.g. "Trandafir"

    // Invite specific
    val inviteCode: String? = null,
    val inviteServerName: String? = null,
    val inviteExpiry: Long? = null,

    // Gift specific
    val giftName: String? = null,
    val giftValue: Int? = null,
    val giftResource: String? = null
)
