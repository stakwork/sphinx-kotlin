package chat.sphinx.dto_chat.model

/**
 * Comes off the wire as:
 *  - 0 (Listed)
 *  - 1 (Unlisted)
 * */
sealed class ChatListed {

    companion object {
        private const val UNLISTED = 1
        private const val LISTED = 0

        /**
         * Converts the integer value returned over the wire to an object.
         *
         * @throws [IllegalArgumentException] if the unlisted type is not supported
         * */
        fun fromInt(unlisted: Int): ChatListed =
            when (unlisted) {
                LISTED -> {
                    True
                }
                UNLISTED -> {
                    False
                }
                else -> {
                    throw IllegalArgumentException(
                        "ChatListed for integer '$unlisted' not supported"
                    )
                }
            }
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