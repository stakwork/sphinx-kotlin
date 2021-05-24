package chat.sphinx.wrapper_common.subscription

@JvmInline
value class SubscriptionId(val value: Long) {
    init {
        require(value >= 0) {
            "SubscriptionId must be greater than or equal to 0"
        }
    }
}
