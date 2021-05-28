package chat.sphinx.wrapper_common.tribe

@JvmInline
value class TribeHost(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "TribeHost cannot be empty"
        }
    }
}