package chat.sphinx.dto_chat.model

/**
 * Comes off the wire as:
 *  - null (Approved)
 *  - 0 (Approved)
 *  - 1 (Pending)
 *  - 2 (Rejected)
 *
 * Note: Status code 1 and 2 ([Pending] & [Rejected] respectively) are used for Tribes with
 * admin approval
 * */
sealed class ChatStatus {

    companion object {
        private const val APPROVED = 0
        private const val PENDING = 1
        private const val REJECTED = 2

        /**
         * Converts the integer value returned over the wire to an object.
         *
         * @throws [IllegalArgumentException] if the [status] integer is not supported
         * */
        fun fromInt(status: Int?): ChatStatus =
            when (status) {
                null,
                APPROVED -> {
                    Approved
                }
                PENDING -> {
                    Pending
                }
                REJECTED -> {
                    Rejected
                }
                else -> {
                    throw IllegalArgumentException(
                        "ChatStatus for integer '$status' not supported"
                    )
                }
            }
    }

    abstract val value: Int

    object Approved: ChatStatus() {
        override val value: Int
            get() = APPROVED
    }

    object Pending: ChatStatus() {
        override val value: Int
            get() = PENDING
    }

    object Rejected: ChatStatus() {
        override val value: Int
            get() = REJECTED
    }
}