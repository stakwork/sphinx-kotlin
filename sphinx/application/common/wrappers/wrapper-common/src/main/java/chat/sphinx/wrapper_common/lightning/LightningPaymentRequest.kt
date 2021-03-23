package chat.sphinx.wrapper_common.lightning

inline class LightningPaymentRequest(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LightningPaymentRequest cannot be empty"
        }
    }
}
