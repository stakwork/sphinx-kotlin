package chat.sphinx.wrapper_contact

inline class ContactKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ContactKey cannot be empty"
        }
    }
}
