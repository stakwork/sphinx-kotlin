package chat.sphinx.wrapper_contact

inline class NodeAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "NodeAlias cannot be empty"
        }
    }
}
