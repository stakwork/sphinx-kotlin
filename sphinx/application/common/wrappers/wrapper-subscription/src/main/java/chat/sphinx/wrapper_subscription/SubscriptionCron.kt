package chat.sphinx.wrapper_subscription

@JvmInline
value class SubscriptionCron(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "SubscriptionCron cannot be empty"
        }
    }
}
