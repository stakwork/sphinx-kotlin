package chat.sphinx.chat_dtos.model

/**
 * Comes off the wire as:
 *  - null (Not Muted)
 *  - 0 (Not Muted)
 *  - 1 (Muted)
 * */
sealed class ChatMuted {

    companion object {
        private const val MUTED = 1
        private const val NOT_MUTED = 0

        /**
         * Converts the integer value returned over the wire to an object.
         *
         * @throws [IllegalArgumentException] if the muted integer is not supported
         * */
        fun fromInt(muted: Int?): ChatMuted =
            when (muted) {
                MUTED -> {
                    True
                }
                null,
                NOT_MUTED -> {
                    False
                }
                else -> {
                    throw IllegalArgumentException("ChatMuted for integer '$muted' not supported")
                }
            }
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