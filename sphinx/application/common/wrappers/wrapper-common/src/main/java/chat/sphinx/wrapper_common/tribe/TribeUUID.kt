package chat.sphinx.wrapper_common.tribe

@JvmInline
value class TribeUUID(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "TribeUUID cannot be empty"
        }
    }
}