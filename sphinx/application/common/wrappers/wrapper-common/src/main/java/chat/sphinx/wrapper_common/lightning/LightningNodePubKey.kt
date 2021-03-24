package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningNodePubKey(): LightningNodePubKey? =
    try {
        LightningNodePubKey(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline class LightningNodePubKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LightningNodePubKey cannot be empty"
        }
    }
}
