package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun String.toErrorMessage(): ErrorMessage? =
    try {
        ErrorMessage(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class ErrorMessage(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ErrorMessage cannot be empty"
        }
    }
}