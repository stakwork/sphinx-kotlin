package chat.sphinx.dto_common

inline class PhotoUrl(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "PhotoUrl cannot be empty"
        }
    }
}