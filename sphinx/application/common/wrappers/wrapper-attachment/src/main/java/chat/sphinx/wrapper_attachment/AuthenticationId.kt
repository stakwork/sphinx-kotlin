package chat.sphinx.wrapper_attachment

@Suppress("NOTHING_TO_INLINE")
inline fun String.toAuthenticationId(): AuthenticationId? =
    try {
        AuthenticationId(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class AuthenticationId(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "AuthenticationId cannot be empty"
        }
    }
}
