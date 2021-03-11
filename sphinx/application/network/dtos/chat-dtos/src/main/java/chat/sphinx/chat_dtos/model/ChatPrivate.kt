package chat.sphinx.chat_dtos.model

/**
 * Comes off the wire as:
 *  - null (Not Private)
 *  - 0 (Not Private)
 *  - 1 (Private)
 * */
sealed class ChatPrivate {

    companion object {
        private const val PRIVATE = 1
        private const val NOT_PRIVATE = 0

        fun fromInt(private: Int?): ChatPrivate =
            when (private) {
                null,
                NOT_PRIVATE -> {
                    False
                }
                PRIVATE -> {
                    True
                }
                else -> {
                    throw IllegalArgumentException(
                        "ChatPrivate for integer '$private' not supported"
                    )
                }
            }
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