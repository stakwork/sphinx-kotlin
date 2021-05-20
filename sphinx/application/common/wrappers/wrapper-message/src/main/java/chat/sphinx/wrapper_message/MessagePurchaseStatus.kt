package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline val MessagePurchaseStatus.isNoStatus: Boolean
    get() = this is MessagePurchaseStatus.NoStatusMessage

@Suppress("NOTHING_TO_INLINE")
inline val MessagePurchaseStatus.isPending: Boolean
    get() = this is MessagePurchaseStatus.Pending

@Suppress("NOTHING_TO_INLINE")
inline val MessagePurchaseStatus.isAccepted: Boolean
    get() = this is MessagePurchaseStatus.Accepted

@Suppress("NOTHING_TO_INLINE")
inline val MessagePurchaseStatus.isDenied: Boolean
    get() = this is MessagePurchaseStatus.Denied

@Suppress("NOTHING_TO_INLINE")
inline val MessagePurchaseStatus.isProcessing: Boolean
    get() = this is MessagePurchaseStatus.Processing

@Suppress("NOTHING_TO_INLINE")
inline val MessagePurchaseStatus.isUnknown: Boolean
    get() = this is MessagePurchaseStatus.Unknown


/**
 * Converts the integer value returned over the wire to an object.
 * */
@Suppress("NOTHING_TO_INLINE")
inline fun Int?.toMessagePurchaseStatus(): MessagePurchaseStatus =
    when (this) {
        null -> {
            MessagePurchaseStatus.NoStatusMessage
        }
        MessagePurchaseStatus.PENDING -> {
            MessagePurchaseStatus.Pending
        }
        MessagePurchaseStatus.ACCEPTED -> {
            MessagePurchaseStatus.Accepted
        }
        MessagePurchaseStatus.DENIED -> {
            MessagePurchaseStatus.Denied
        }
        MessagePurchaseStatus.PROCESSING -> {
            MessagePurchaseStatus.Processing
        }
        else -> {
            MessagePurchaseStatus.Unknown(this)
        }
    }


@Suppress("NOTHING_TO_INLINE")
inline val MessagePurchaseStatus.incomingLabelText: String
    get() = when (this) {
        MessagePurchaseStatus.Pending -> {
            "Pending"
        }
        MessagePurchaseStatus.Accepted -> {
            "Purchase Succeeded"
        }
        MessagePurchaseStatus.Denied -> {
            "Purchase Denied"
        }
        MessagePurchaseStatus.Processing -> {
            "Processing payment"
        }
        else -> {
            "Pay"
        }
    }


@Suppress("NOTHING_TO_INLINE")
inline val MessagePurchaseStatus.outgoingLabelText: String
    get() = when (this) {
        MessagePurchaseStatus.Pending -> {
            "Pending"
        }
        MessagePurchaseStatus.Accepted -> {
            "Succeeded"
        }
        MessagePurchaseStatus.Denied -> {
            "Denied"
        }
        MessagePurchaseStatus.Processing -> {
            "Processing"
        }
        else -> {
            "Unknown"
        }
    }

sealed class MessagePurchaseStatus {

    companion object {
        const val PENDING = 0
        const val ACCEPTED = 1
        const val DENIED = 2
        const val PROCESSING = 3
    }

    abstract val value: Int?

    object NoStatusMessage : MessagePurchaseStatus() {
        override val value: Int?
            get() = null
    }

    object Pending : MessagePurchaseStatus() {
        override val value: Int
            get() = PENDING
    }

    object Accepted : MessagePurchaseStatus() {
        override val value: Int
            get() = ACCEPTED
    }

    object Denied : MessagePurchaseStatus() {
        override val value: Int
            get() = DENIED
    }

    object Processing : MessagePurchaseStatus() {
        override val value: Int
            get() = PROCESSING
    }

    class Unknown(override val value: Int) : MessagePurchaseStatus()
}
