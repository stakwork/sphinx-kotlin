package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun String.toRecipientAlias(): RecipientAlias? =
    try {
        RecipientAlias(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class RecipientAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "RecipientAlias cannot be empty"
        }
    }
}
