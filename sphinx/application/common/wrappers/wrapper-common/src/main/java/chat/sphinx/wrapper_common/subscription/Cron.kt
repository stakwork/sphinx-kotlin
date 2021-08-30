package chat.sphinx.wrapper_common.subscription

@JvmInline
value class Cron(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "SubscriptionCount cannot be empty"
        }
    }
}