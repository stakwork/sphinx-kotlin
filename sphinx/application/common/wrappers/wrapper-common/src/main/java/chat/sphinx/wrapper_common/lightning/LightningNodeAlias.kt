package chat.sphinx.wrapper_common.lightning

inline class LightningNodeAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "LightningNodeAlias cannot be empty"
        }
    }
}
