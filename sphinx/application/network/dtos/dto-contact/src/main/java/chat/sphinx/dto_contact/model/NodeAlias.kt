package chat.sphinx.dto_contact.model

inline class NodeAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "NodeAlias cannot be empty"
        }
    }
}
