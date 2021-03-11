package chat.sphinx.dto_common

inline class DateTime(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "DateTime cannot be empty"
        }
    }
}