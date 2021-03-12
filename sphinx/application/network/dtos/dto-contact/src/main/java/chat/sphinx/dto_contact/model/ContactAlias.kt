package chat.sphinx.dto_contact.model

inline class ContactAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ContactAlias cannot be empty"
        }
    }
}
