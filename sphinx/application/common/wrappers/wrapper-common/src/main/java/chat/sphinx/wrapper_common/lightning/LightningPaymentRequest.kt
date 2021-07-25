package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningPaymentRequestOrNull(): LightningPaymentRequest? =
    try {
        LightningPaymentRequest(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidLightningPaymentRequest: Boolean
    get() = toLightningPaymentRequestOrNull() != null

@Suppress("NOTHING_TO_INLINE")
inline fun LightningPaymentRequest.toBolt11OrNull(): Bolt11? =
    try {
        Bolt11.decode(this)
    } catch (e: Exception) {
        null
    }

@JvmInline
value class LightningPaymentRequest(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LightningPaymentRequest cannot be empty"
        }
        require(toBolt11OrNull() != null) {
            "LightningPaymentRequest could not be decoded using the bolt11 specification"
        }
    }
}
