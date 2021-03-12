package chat.sphinx.dto_contact.model

inline class ContactKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ContactKey cannot be empty"
        }
    }
}
