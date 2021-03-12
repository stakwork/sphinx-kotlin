package chat.sphinx.wrapper_contact

inline class DeviceId(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "DeviceId cannot be empty"
        }
    }
}
