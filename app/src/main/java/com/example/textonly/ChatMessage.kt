package text.only.app

sealed class ChatMessage {
    abstract val isSent: Boolean
}

data class TextMessage(
    val content: String,
    override val isSent: Boolean
) : ChatMessage()

data class FileMessage(
    val fileName: String,
    val fileType: String?, // "image/png", etc.
    val price: Int, // 0 if free
    val priceUnit: String?, // "COINS" or gift resource name
    val priceGiftName: String?,
    override val isSent: Boolean
) : ChatMessage()

data class InviteMessage(
    val serverName: String,
    val inviterName: String,
    val inviteCode: String,
    val expiryTimestamp: Long,
    override val isSent: Boolean
) : ChatMessage()

data class GiftMessage(
    val giftName: String,
    val giftValue: Int,
    val giftResource: String,
    override val isSent: Boolean
) : ChatMessage()

data class EmoteMessage(
    val emoteName: String,
    val emoteResource: String,
    override val isSent: Boolean
) : ChatMessage()
