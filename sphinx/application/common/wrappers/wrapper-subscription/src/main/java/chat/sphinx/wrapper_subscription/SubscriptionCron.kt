package chat.sphinx.wrapper_subscription

inline class SubscriptionCron(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "SubscriptionCron cannot be empty"
        }
    }
}
