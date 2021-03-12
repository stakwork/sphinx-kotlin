package chat.sphinx.wrapper_invite

inline class InviteString(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "InviteString cannot be empty"
        }
    }
}
