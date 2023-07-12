package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun String.toThreadUUID(): ThreadUUID? =
    try {
        ThreadUUID(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class ThreadUUID(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ThreadUUID cannot be empty"
        }
    }
}