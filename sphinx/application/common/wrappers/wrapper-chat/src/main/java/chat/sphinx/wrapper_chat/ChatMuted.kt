package chat.sphinx.wrapper_chat

@Suppress("NOTHING_TO_INLINE")
inline fun ChatMuted.isTrue(): Boolean =
    this is ChatMuted.True

/**
 * Converts the integer value returned over the wire to an object.
 *
 * @throws [IllegalArgumentException] if the integer is not supported
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun Int?.toChatMuted(): ChatMuted =
    when (this) {
        ChatMuted.MUTED -> {
            ChatMuted.True
        }
        null,
        ChatMuted.NOT_MUTED -> {
            ChatMuted.False
        }
        else -> {
            throw IllegalArgumentException("ChatMuted for integer '$this' not supported")
        }
    }

/**
 * Comes off the wire as:
 *  - null (Not Muted)
 *  - 0 (Not Muted)
 *  - 1 (Muted)
 * */
sealed class ChatMuted {

    companion object {
        const val MUTED = 1
        const val NOT_MUTED = 0
    }

    abstract val value: Int

    object True: ChatMuted() {
        override val value: Int
            get() = MUTED
    }

    object False: ChatMuted() {
        override val value: Int
            get() = NOT_MUTED
    }
}
