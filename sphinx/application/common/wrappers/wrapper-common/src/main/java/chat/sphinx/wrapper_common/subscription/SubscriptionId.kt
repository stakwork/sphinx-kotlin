package chat.sphinx.wrapper_common.subscription

inline class SubscriptionId(val value: Long) {
    init {
        require(value >= 0) {
            "SubscriptionId must be greater than or equal to 0"
        }
    }
}
