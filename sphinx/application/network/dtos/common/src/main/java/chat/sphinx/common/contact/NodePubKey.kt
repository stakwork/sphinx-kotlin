package chat.sphinx.common.contact

inline class NodePubKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "NodePubKey cannot be empty"
        }
    }
}