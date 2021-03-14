package chat.sphinx.wrapper_chat

@Suppress("NOTHING_TO_INLINE")
inline fun ChatUnlisted.isTrue(): Boolean =
    this is ChatUnlisted.True

/**
 * Converts the integer value returned over the wire to an object.
 *
 * @throws [IllegalArgumentException] if the integer is not supported
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun Int.toChatUnlisted(): ChatUnlisted =
    when (this) {
        ChatUnlisted.NOT_UNLISTED -> {
            ChatUnlisted.True
        }
        ChatUnlisted.UNLISTED -> {
            ChatUnlisted.False
        }
        else -> {
            throw IllegalArgumentException(
                "ChatUnlisted for integer '$this' not supported"
            )
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toChatUnlisted(): ChatUnlisted =
    if (this) ChatUnlisted.True else ChatUnlisted.False

/**
 * Comes off the wire as:
 *  - 0 (Not Unlisted)
 *  - 1 (Unlisted)
 * */
sealed class ChatUnlisted {

    companion object {
        const val UNLISTED = 1
        const val NOT_UNLISTED = 0
    }

    abstract val value: Int

    object True: ChatUnlisted() {
        override val value: Int
            get() = UNLISTED
    }

    object False: ChatUnlisted() {
        override val value: Int
            get() = NOT_UNLISTED
    }
}
