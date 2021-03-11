package chat.sphinx.dto_chat.model

@Suppress("NOTHING_TO_INLINE")
inline fun ChatSeen.isTrue(): Boolean =
    this is ChatSeen.True

/**
 * Comes off the wier as:
 *  - 0 (Unseen)
 *  - 1 (Seen)
 * */
sealed class ChatSeen {

    companion object {
        private const val SEEN = 1
        private const val UNSEEN = 0

        /**
         * Converts the integer value returned over the wire to an object.
         *
         * @throws [IllegalArgumentException] if the [seen] integer is not supported
         * */
        fun fromInt(seen: Int): ChatSeen =
            when (seen) {
                SEEN -> {
                    True
                }
                UNSEEN -> {
                    False
                }
                else -> {
                    throw IllegalArgumentException(
                        "ChatSeen for integer '$seen' not supported"
                    )
                }
            }
    }

    abstract val value: Int

    object True: ChatSeen() {
        override val value: Int
            get() = SEEN
    }

    object False: ChatSeen() {
        override val value: Int
            get() = UNSEEN
    }
}
