package chat.sphinx.wrapper_common.subscription

@JvmInline
value class Cron(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "Subscription Cron cannot be empty"
        }
    }
}