package chat.sphinx.wrapper_invite

@Suppress("NOTHING_TO_INLINE")
inline fun String.toInviteCodeOrNull(): InviteCode? =
    try {
        InviteCode(this)
    } catch (e: Exception) {
        null
    }

@JvmInline
value class InviteCode(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "InviteCode cannot be empty"
        }
    }
}
