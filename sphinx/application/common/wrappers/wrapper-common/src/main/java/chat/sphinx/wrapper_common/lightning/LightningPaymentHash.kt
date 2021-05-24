package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningPaymentHash(): LightningPaymentHash? =
    try {
        LightningPaymentHash(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class LightningPaymentHash(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LightningPaymentHash cannot be empty"
        }
    }
}
