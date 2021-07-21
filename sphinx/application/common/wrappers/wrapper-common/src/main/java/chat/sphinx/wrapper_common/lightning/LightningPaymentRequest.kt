package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningPaymentRequest(): LightningPaymentRequest? =
    try {
        LightningPaymentRequest(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidLightningPaymentRequest: Boolean
    get() = isNotEmpty() && toLightningPaymentRequest() != null

@JvmInline
value class LightningPaymentRequest(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LightningPaymentRequest cannot be empty"
        }
        require(toBolt11() != null) {
            "LightningPaymentRequest could not be decoded using the bolt11 specification"
        }
    }

    fun toBolt11(): Bolt11? {
        return try {
            Bolt11.decode(this)
        } catch (e: Exception) {
            null
        }
    }
}
