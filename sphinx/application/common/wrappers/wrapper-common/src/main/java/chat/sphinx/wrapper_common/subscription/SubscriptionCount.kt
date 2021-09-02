package chat.sphinx.wrapper_common.subscription

@JvmInline
value class SubscriptionCount(val value: Long) {
    init {
        require(value >= 0) {
            "SubscriptionCount must be greater than or equal to 0"
        }
    }
}