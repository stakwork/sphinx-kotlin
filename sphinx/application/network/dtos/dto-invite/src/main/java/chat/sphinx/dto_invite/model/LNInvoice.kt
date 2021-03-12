package chat.sphinx.dto_invite.model

inline class LNInvoice(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LNInvoice cannot be empty"
        }
    }
}
