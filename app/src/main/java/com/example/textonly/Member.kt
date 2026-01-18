package text.only.app

data class Member(
    val name: String,
    val roles: List<String>
    // Poți adăuga ulterior:
    // val avatarUrl: String?,
    // val status: String // "online", "offline"
)
