package text.only.app

// Un model simplu pentru a reprezenta un canal
data class Channel(
    val name: String,
    val type: ChannelType
)

enum class ChannelType {
    TEXT,
    VOICE
}
