package chat.sphinx.dto_invite.model

inline class InviteString(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "InviteString cannot be empty"
        }
    }
}
