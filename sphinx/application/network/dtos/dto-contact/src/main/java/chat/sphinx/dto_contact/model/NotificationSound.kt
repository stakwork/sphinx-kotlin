package chat.sphinx.dto_contact.model

inline class NotificationSound(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "NotificationSound cannot be empty"
        }
    }
}
