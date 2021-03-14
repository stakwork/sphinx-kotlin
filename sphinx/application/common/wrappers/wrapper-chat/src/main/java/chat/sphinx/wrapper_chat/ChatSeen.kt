package chat.sphinx.wrapper_chat

@Suppress("NOTHING_TO_INLINE")
inline fun ChatSeen.isTrue(): Boolean =
    this is ChatSeen.True

/**
 * Converts the integer value returned over the wire to an object.
 *
 * @throws [IllegalArgumentException] if the integer is not supported
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun Int.toChatSeen(): ChatSeen =
    when (this) {
        ChatSeen.SEEN -> {
            ChatSeen.True
        }
        ChatSeen.NOT_SEEN -> {
            ChatSeen.False
        }
        else -> {
            throw IllegalArgumentException(
                "ChatSeen for integer '$this' not supported"
            )
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toChatSeen(): ChatSeen =
    if (this) ChatSeen.True else ChatSeen.False

/**
 * Comes off the wire as:
 *  - 0 (Unseen)
 *  - 1 (Seen)
 * */
sealed class ChatSeen {

    companion object {
        const val SEEN = 1
        const val NOT_SEEN = 0
    }

    abstract val value: Int

    object True: ChatSeen() {
        override val value: Int
            get() = SEEN
    }

    object False: ChatSeen() {
        override val value: Int
            get() = NOT_SEEN
    }
}
