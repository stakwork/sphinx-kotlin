package chat.sphinx.wrapper_subscription

@JvmInline
value class SubscriptionCount(val value: Int) {
    init {
        require(value >= 0) {
            "SubscriptionCount must be greater than or equal to 0"
        }
    }
}
