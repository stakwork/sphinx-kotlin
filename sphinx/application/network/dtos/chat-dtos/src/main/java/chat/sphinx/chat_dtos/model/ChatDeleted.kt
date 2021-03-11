package chat.sphinx.chat_dtos.model

/**
 * Comes off the wire as:
 *  - 0 (Not Deleted)
 *  - 1 (Deleted)
 * */
sealed class ChatDeleted {

    companion object {
        private const val DELETED = 1
        private const val NOT_DELETED = 0

        fun fromInt(deleted: Int): ChatDeleted =
            when (deleted) {
                DELETED -> {
                    True
                }
                NOT_DELETED -> {
                    False
                }
                else -> {
                    throw IllegalArgumentException(
                        "ChatDeleted for integer '$deleted' not supported"
                    )
                }
            }
    }

    abstract val value: Int

    object True: ChatDeleted() {
        override val value: Int
            get() = DELETED
    }

    object False: ChatDeleted(){
        override val value: Int
            get() = NOT_DELETED
    }
}