package chat.sphinx.wrapper_invite

@Suppress("NOTHING_TO_INLINE")
inline fun String.toValidConnectionStringOrNull(): ConnectionString? =
    try {
        ConnectionString(this)
    } catch (e: Exception) {
        null
    }



@JvmInline
value class ConnectionString(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ConnectionString cannot be empty"
        }
    }
}
