package chat.sphinx.wrapper_chat

@Suppress("NOTHING_TO_INLINE")
inline fun String.toChatAlias(): ChatAlias? =
    try {
        ChatAlias(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class ChatAlias(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "ChatAlias cannot be empty"
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.fixedAlias(): String {
    var fixedAlias = ""

    for (character in this.toCharArray()) {
        if (Character.isSpaceChar(character)) {
            fixedAlias += "_"
        } else if (Character.isLetterOrDigit(character) || character.toString() == "_") {
            fixedAlias += character
        }
    }

    return fixedAlias
}
