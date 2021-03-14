package chat.sphinx.wrapper_subscription

inline class SubscriptionCount(val value: Int) {
    init {
        require(value >= 0) {
            "SubscriptionCount must be greater than or equal to 0"
        }
    }
}
