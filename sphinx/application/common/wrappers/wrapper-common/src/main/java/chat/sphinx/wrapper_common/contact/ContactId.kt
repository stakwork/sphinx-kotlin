package chat.sphinx.wrapper_common.contact

inline class ContactId(val value: Long) {
    init {
        require(value >= 0L) {
            "ContactId must be greater than or equal 0"
        }
    }
}
