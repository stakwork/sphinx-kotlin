package chat.sphinx.wrapper_message

inline class SenderAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "SenderAlias cannot be empty"
        }
    }
}
