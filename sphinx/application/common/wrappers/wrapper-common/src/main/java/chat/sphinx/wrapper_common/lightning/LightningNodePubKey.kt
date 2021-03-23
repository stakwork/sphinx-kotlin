package chat.sphinx.wrapper_common.lightning

inline class LightningNodePubKey(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LightningNodePubKey cannot be empty"
        }
    }
}
