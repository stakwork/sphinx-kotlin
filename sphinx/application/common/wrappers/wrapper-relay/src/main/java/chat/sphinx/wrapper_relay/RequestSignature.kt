package chat.sphinx.wrapper_relay

@Suppress("NOTHING_TO_INLINE")
inline fun String.toRequestSignature(): RequestSignature? =
    try {
        RequestSignature(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class RequestSignature(val value: String) {

    companion object {
        const val REQUEST_SIGNATURE_HEADER = "x-hmac"
    }

    init {
        require(value.isNotEmpty()) {
            "RequestSignature cannot be empty"
        }
    }
}
