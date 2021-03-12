package chat.sphinx.wrapper_common.lightning

inline class LNInvoice(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LNInvoice cannot be empty"
        }
    }
}
