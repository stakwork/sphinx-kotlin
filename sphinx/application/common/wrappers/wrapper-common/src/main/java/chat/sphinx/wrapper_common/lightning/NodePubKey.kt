package chat.sphinx.wrapper_common.lightning

inline class NodePubKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "NodePubKey cannot be empty"
        }
    }
}
