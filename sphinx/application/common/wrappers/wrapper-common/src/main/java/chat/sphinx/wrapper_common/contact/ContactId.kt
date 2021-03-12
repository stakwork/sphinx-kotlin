package chat.sphinx.wrapper_common.contact

inline class ContactId(val value: Long) {
    init {
        require(value > 0) {
            "ContactId must be greater than 0"
        }
    }
}
