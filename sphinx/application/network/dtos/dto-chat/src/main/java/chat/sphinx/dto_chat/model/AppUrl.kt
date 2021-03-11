package chat.sphinx.dto_chat.model

inline class AppUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "AppUrl cannot be empty"
        }
    }
}
