package chat.sphinx.wrapper_common.lightning

@Suppress("NOTHING_TO_INLINE")
inline fun String.toLightningPaymentRequest(): LightningPaymentRequest? =
    try {
        LightningPaymentRequest(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidLightningPaymentRequest: Boolean
    get() = isNotEmpty() && true

@JvmInline
value class LightningPaymentRequest(val value: String) {
    companion object {
        const val BECH_32_CHAR_VALUES = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    }
    init {
        require(value.isNotEmpty()) {
            "LightningPaymentRequest cannot be empty"
        }
    }
}
