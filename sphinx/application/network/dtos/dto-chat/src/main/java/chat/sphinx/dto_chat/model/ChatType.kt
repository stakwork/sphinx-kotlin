package chat.sphinx.dto_chat.model

/**
 * Comes off the wire as:
 *  - 0 (Contact)
 *  - 1 (Group)
 *  - 2 (Tribe)
 * */
sealed class ChatType {

    companion object {
        private const val CONTACT = 0
        private const val GROUP = 1
        private const val TRIBE = 2

        /**
         * Converts the integer value returned over the wire to an object.
         *
         * @throws [IllegalArgumentException] if the [type] integer is not supported
         * */
        @Throws(IllegalArgumentException::class)
        fun fromInt(type: Int): ChatType =
            when (type) {
                CONTACT -> {
                    Contact
                }
                GROUP -> {
                    Group
                }
                TRIBE -> {
                    Tribe
                }
                else -> {
                    throw IllegalArgumentException(
                        "ChatType for integer '$type' not supported"
                    )
                }
            }

    }

    abstract val value: Int

    object Contact: ChatType() {
        override val value: Int
            get() = CONTACT
    }

    object Group: ChatType() {
        override val value: Int
            get() = GROUP
    }

    object Tribe: ChatType() {
        override val value: Int
            get() = TRIBE
    }
}