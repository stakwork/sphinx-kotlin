package chat.sphinx.wrapper_invite

@JvmInline
value class InviteString(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "InviteString cannot be empty"
        }
    }
}
