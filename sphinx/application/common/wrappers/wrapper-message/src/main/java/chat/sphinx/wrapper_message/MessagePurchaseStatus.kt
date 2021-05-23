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

    data class Unknown(override val value: Int) : MessagePurchaseStatus()
}
