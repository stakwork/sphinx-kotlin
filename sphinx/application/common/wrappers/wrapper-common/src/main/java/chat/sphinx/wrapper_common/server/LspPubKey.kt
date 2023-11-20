package chat.sphinx.wrapper_common.server

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLspPubKey(): LspPubKey? =
    try {
        LspPubKey(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class LspPubKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LspPubKey cannot be empty"
        }
    }
}
