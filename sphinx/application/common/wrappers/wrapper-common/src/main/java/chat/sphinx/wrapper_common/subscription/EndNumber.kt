package chat.sphinx.wrapper_common.subscription

@JvmInline
value class EndNumber(val value: Long) {
    init {
        require(value >= 0) {
            "EndNumber must be greater than or equal to 0"
        }
    }
}
