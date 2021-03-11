package chat.sphinx.common.contact

inline class ContactAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ContactAlias cannot be empty"
        }
    }
}