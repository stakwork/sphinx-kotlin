package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningNodeAlias(): LightningNodeAlias? =
    try {
        LightningNodeAlias(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class LightningNodeAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LightningNodeAlias cannot be empty"
        }
    }
}
