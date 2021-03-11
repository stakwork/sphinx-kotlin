package chat.sphinx.dto_common.contact

inline class ContactKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ContactKey cannot be empty"
        }
    }
}