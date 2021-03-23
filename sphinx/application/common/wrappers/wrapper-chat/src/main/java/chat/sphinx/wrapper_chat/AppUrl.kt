package chat.sphinx.wrapper_chat

inline class AppUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "AppUrl cannot be empty"
        }
    }
}
