package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningPaymentRequest(): LightningPaymentRequest? =
    try {
        LightningPaymentRequest(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline class LightningPaymentRequest(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LightningPaymentRequest cannot be empty"
        }
    }
}
