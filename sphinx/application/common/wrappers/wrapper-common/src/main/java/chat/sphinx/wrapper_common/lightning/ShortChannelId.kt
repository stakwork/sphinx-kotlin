package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toShortChannelId(): ShortChannelId? =
    try {
        ShortChannelId(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class ShortChannelId(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ShortChannelId cannot be empty"
        }
    }
}
