package chat.sphinx.wrapper_message

@Suppress("NOTHING_TO_INLINE")
inline fun PurchaseStatus.isPurchasePending(): Boolean =
    this is PurchaseStatus.Pending

@Suppress("NOTHING_TO_INLINE")
inline fun PurchaseStatus.isPurchaseProcessing(): Boolean =
    this is PurchaseStatus.Processing

@Suppress("NOTHING_TO_INLINE")
inline fun PurchaseStatus.isPurchaseAccepted(): Boolean =
    this is PurchaseStatus.Accepted

@Suppress("NOTHING_TO_INLINE")
inline fun PurchaseStatus.isPurchaseDenied(): Boolean =
    this is PurchaseStatus.Denied

sealed class PurchaseStatus {

    companion object {
        const val PENDING = 0
        const val PROCESSING = 1
        const val ACCEPTED = 2
        const val DENIED = 3
    }

    abstract val value: Int

    object Pending : PurchaseStatus() {
        override val value: Int
            get() = PENDING
    }

    object Processing : PurchaseStatus() {
        override val value: Int
            get() = PROCESSING
    }

    object Accepted : PurchaseStatus() {
        override val value: Int
            get() = ACCEPTED
    }

    object Denied : PurchaseStatus() {
        override val value: Int
            get() = DENIED
    }
}