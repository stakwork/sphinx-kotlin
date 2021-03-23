package chat.sphinx.wrapper_contact

inline class NotificationSound(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "NotificationSound cannot be empty"
        }
    }
}
