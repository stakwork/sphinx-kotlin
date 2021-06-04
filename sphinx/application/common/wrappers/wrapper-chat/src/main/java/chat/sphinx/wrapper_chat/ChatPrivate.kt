package chat.sphinx.wrapper_chat

@Suppress("NOTHING_TO_INLINE")
inline fun ChatPrivate.isTrue(): Boolean =
    this is ChatPrivate.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int?.toChatPrivate(): ChatPrivate =
    when (this) {
        null,
        ChatPrivate.PRIVATE -> {
            ChatPrivate.True
        }
        else -> {
            ChatPrivate.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean?.toChatPrivate(): ChatPrivate =
    if (this == false) ChatPrivate.False else ChatPrivate.True

/**
 * Comes off the wire as:
 *  - null (Private)
 *  - 0 (Not Private)
 *  - 1 (Private)
 *  - false (Not Private)
 *  - true (Private)
 * */
sealed class ChatPrivate {

    companion object {
        const val PRIVATE = 1
        const val NOT_PRIVATE = 0
    }

    abstract val value: Int

    object True: ChatPrivate() {
        override val value: Int
            get() = PRIVATE
    }

    object False: ChatPrivate() {
        override val value: Int
            get() = NOT_PRIVATE
    }
}
