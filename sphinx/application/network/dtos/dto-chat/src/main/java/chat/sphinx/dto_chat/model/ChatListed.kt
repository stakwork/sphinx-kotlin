package chat.sphinx.dto_chat.model

@Suppress("NOTHING_TO_INLINE")
inline fun ChatListed.isTrue(): Boolean =
    this is ChatListed.True

/**
 * Converts the integer value returned over the wire to an object.
 *
 * @throws [IllegalArgumentException] if the integer is not supported
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun Int.toChatListed(): ChatListed =
    when (this) {
        ChatListed.LISTED -> {
            ChatListed.True
        }
        ChatListed.UNLISTED -> {
            ChatListed.False
        }
        else -> {
            throw IllegalArgumentException(
                "ChatListed for integer '$this' not supported"
            )
        }
    }

/**
 * Comes off the wire as:
 *  - 0 (Listed)
 *  - 1 (Unlisted)
 * */
sealed class ChatListed {

    companion object {
        const val UNLISTED = 1
        const val LISTED = 0
    }

    abstract val value: Int

    object True: ChatListed() {
        override val value: Int
            get() = LISTED
    }

    object False: ChatListed() {
        override val value: Int
            get() = UNLISTED
    }
}
