package text.only.app

interface MessageInteractionListener {
    fun onUnlockFileRequested(message: FileMessage, position: Int)
    fun onInviteAction(inviteCode: String, accepted: Boolean, position: Int)
}
