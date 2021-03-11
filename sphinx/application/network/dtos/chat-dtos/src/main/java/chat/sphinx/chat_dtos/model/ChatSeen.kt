package chat.sphinx.chat_dtos.model

/**
 * Comes off the wier as:
 *  - 0 (Unseen)
 *  - 1 (Seen)
 * */
sealed class ChatSeen {

    companion object {
        private const val SEEN = 1
        private const val UNSEEN = 0

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