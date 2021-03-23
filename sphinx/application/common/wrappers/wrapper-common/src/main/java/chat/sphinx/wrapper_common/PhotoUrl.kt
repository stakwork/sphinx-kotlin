package chat.sphinx.wrapper_common

inline class PhotoUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "PhotoUrl cannot be empty"
        }
    }
}
