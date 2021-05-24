package chat.sphinx.wrapper_chat

@Suppress("NOTHING_TO_INLINE")
inline fun ChatStatus.isApproved(): Boolean =
    this is ChatStatus.Approved

@Suppress("NOTHING_TO_INLINE")
inline fun ChatStatus.isPending(): Boolean =
    this is ChatStatus.Pending

@Suppress("NOTHING_TO_INLINE")
inline fun ChatStatus.isRejected(): Boolean =
    this is ChatStatus.Rejected

@Suppress("NOTHING_TO_INLINE")
inline fun ChatStatus.isUnknown(): Boolean =
    this is ChatStatus.Unknown

/**
 * Converts the integer value returned over the wire to an object.
 * */
@Suppress("NOTHING_TO_INLINE")
inline fun Int?.toChatStatus(): ChatStatus =
    when (this) {
        null,
        ChatStatus.APPROVED -> {
            ChatStatus.Approved
        }
        ChatStatus.PENDING -> {
            ChatStatus.Pending
        }
        ChatStatus.REJECTED -> {
            ChatStatus.Rejected
        }
        else -> {
            ChatStatus.Unknown(this)
        }
    }


/**
 * Comes off the wire as:
 *  - null (Approved)
 *  - 0 (Approved)
 *  - 1 (Pending)
 *  - 2 (Rejected)
 *
 * Note: Status code 1 and 2 ([Pending] & [Rejected] respectively) are used for Tribes with
 * admin approval
 *
 * https://github.com/stakwork/sphinx-relay/blob/7f8fd308101b5c279f6aac070533519160aa4a9f/src/constants.ts#L24
 * */
sealed class ChatStatus {

    companion object {
        const val APPROVED = 0
        const val PENDING = 1
        const val REJECTED = 2
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

    data class Unknown(override val value: Int): ChatStatus()
}
