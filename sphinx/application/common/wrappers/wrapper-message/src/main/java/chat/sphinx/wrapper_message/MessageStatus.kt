package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun MessageStatus.isNoStatus(): Boolean =
    this is MessageStatus.NoStatus

@Suppress("NOTHING_TO_INLINE")
inline fun MessageStatus.isPending(): Boolean =
    this is MessageStatus.Pending

@Suppress("NOTHING_TO_INLINE")
inline fun MessageStatus.isConfirmed(): Boolean =
    this is MessageStatus.Confirmed

@Suppress("NOTHING_TO_INLINE")
inline fun MessageStatus.isCancelled(): Boolean =
    this is MessageStatus.Cancelled

@Suppress("NOTHING_TO_INLINE")
inline fun MessageStatus.isReceived(): Boolean =
    this is MessageStatus.Received

@Suppress("NOTHING_TO_INLINE")
inline fun MessageStatus.isFailed(): Boolean =
    this is MessageStatus.Failed

@Suppress("NOTHING_TO_INLINE")
inline fun MessageStatus.isDeleted(): Boolean =
    this is MessageStatus.Deleted

@Suppress("NOTHING_TO_INLINE")
inline fun MessageStatus.isUnknown(): Boolean =
    this is MessageStatus.Unknown

/**
 * Converts the integer value returned over the wire to an object.
 * */
@Suppress("NOTHING_TO_INLINE")
inline fun Int?.toMessageStatus(): MessageStatus =
    when (this) {
        null -> {
            MessageStatus.NoStatus
        }
        MessageStatus.PENDING -> {
            MessageStatus.Pending
        }
        MessageStatus.CONFIRMED -> {
            MessageStatus.Confirmed
        }
        MessageStatus.CANCELLED -> {
            MessageStatus.Cancelled
        }
        MessageStatus.RECEIVED -> {
            MessageStatus.Received
        }
        MessageStatus.FAILED -> {
            MessageStatus.Failed
        }
        MessageStatus.DELETED -> {
            MessageStatus.Deleted
        }
        else -> {
            MessageStatus.Unknown(this)
        }
    }

/**
 * Comes off the wire as:
 *  - null (No Status) // returned for message types 5 (Direct Payment) & 6 (Attachment)
 *  - 0 (Pending)
 *  - 1 (Confirmed)
 *  - 2 (Cancelled)
 *  - 3 (Received)
 *  - 4 (Failed)
 *  - 5 (Deleted)
 *
 * https://github.com/stakwork/sphinx-relay/blob/7f8fd308101b5c279f6aac070533519160aa4a9f/src/constants.ts#L16
 * */
sealed class MessageStatus {

    companion object {
        // NO_STATUS = null
        const val PENDING = 0
        const val CONFIRMED = 1
        const val CANCELLED = 2
        const val RECEIVED = 3
        const val FAILED = 4
        const val DELETED = 5
    }

    abstract val value: Int?

    object NoStatus: MessageStatus() {
        override val value: Int?
            get() = null
    }

    object Pending: MessageStatus() {
        override val value: Int
            get() = PENDING
    }

    object Confirmed: MessageStatus() {
        override val value: Int
            get() = CONFIRMED
    }

    object Cancelled: MessageStatus() {
        override val value: Int
            get() = CANCELLED
    }

    object Received: MessageStatus() {
        override val value: Int
            get() = RECEIVED
    }

    object Failed: MessageStatus() {
        override val value: Int
            get() = FAILED
    }

    object Deleted: MessageStatus() {
        override val value: Int
            get() = DELETED
    }

    class Unknown(override val value: Int) : MessageStatus()
}
