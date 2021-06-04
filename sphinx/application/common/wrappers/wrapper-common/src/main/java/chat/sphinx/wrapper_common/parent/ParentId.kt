package chat.sphinx.wrapper_common.parent

@JvmInline
value class ParentId(val value: Long) {
    init {
        require(value >= 0) {
            "ParentId must be greater than or equal to 0"
        }
    }
}
