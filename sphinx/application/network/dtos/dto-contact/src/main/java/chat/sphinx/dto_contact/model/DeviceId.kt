package chat.sphinx.dto_contact.model

inline class DeviceId(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "DeviceId cannot be empty"
        }
    }
}
