package text.only.app

import android.net.Uri

sealed class ChatMessage {
    abstract val isSent: Boolean
}

data class TextMessage(
    val content: String,
    override val isSent: Boolean
) : ChatMessage()

data class FileMessage(
    val fileName: String,
    val fileType: String?,
    val price: Int, // Can be 1 (qty) if unit is Gift
    val priceUnit: String?, // "COINS" or resourceName
    val priceGiftName: String?,
    override val isSent: Boolean,
    var isUnlocked: Boolean = false,
    val localUri: Uri? = null,
    var remoteUrl: String? = null
) : ChatMessage()

data class InviteMessage(
    val serverName: String,
    val inviterName: String, 
    val inviteCode: String,
    val expiryTimestamp: Long,
    override val isSent: Boolean,
    var state: InviteState = InviteState.PENDING 
) : ChatMessage()

data class GiftMessage(
    val giftName: String,
    val giftValue: Int,
    val giftResource: String,
    override val isSent: Boolean
) : ChatMessage()

enum class InviteState {
    PENDING,
    ACCEPTED,
    REFUSED,
    EXPIRED
}
