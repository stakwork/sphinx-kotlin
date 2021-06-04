package chat.sphinx.wrapper_chat

@Suppress("NOTHING_TO_INLINE")
inline fun ChatUnlisted.isTrue(): Boolean =
    this is ChatUnlisted.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toChatUnlisted(): ChatUnlisted =
    when (this) {
        ChatUnlisted.UNLISTED -> {
            ChatUnlisted.True
        }
        else -> {
            ChatUnlisted.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toChatUnlisted(): ChatUnlisted =
    if (this) ChatUnlisted.True else ChatUnlisted.False

/**
 * Comes off the wire as:
 *  - 0 (Not Unlisted)
 *  - 1 (Unlisted)
 *  - true (Unlisted)
 *  - false (Not Unlisted)
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
