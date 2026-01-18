package text.only.app

// O clasă sigilată pentru a reprezenta itemii din lista principală
sealed class MainListItem {
    data class ServerItem(val server: Server) : MainListItem()
    data class ConversationItem(val conversation: ConversationEntity) : MainListItem()
}
