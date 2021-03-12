package chat.sphinx.wrapper_common.lightning

inline class LightningPaymentHash(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LightningPaymentHash cannot be empty"
        }
    }
}
