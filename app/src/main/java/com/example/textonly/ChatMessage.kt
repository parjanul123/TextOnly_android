package text.only.app

import android.net.Uri

// O clasă sigilată pentru a reprezenta toate tipurile posibile de mesaje
sealed class ChatMessage {
    abstract val isSent: Boolean
}

data class TextMessage(
    val content: String,
    override val isSent: Boolean
) : ChatMessage()

data class FileMessage(
    val fileName: String,
    val fileType: String?, // ex: "image/png", "video/mp4"
    val price: Int,
    override val isSent: Boolean,
    var isUnlocked: Boolean = false,
    val localUri: Uri? = null, // URI-ul local, pentru upload
    var remoteUrl: String? = null // URL-ul de pe server, după upload
) : ChatMessage()
