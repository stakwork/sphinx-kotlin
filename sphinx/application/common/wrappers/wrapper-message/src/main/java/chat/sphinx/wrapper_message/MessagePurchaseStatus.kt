package chat.sphinx.wrapper_message

inline val MessagePurchaseStatus.isPending: Boolean
    get() = this is MessagePurchaseStatus.Pending

inline val MessagePurchaseStatus.isAccepted: Boolean
    get() = this is MessagePurchaseStatus.Accepted

inline val MessagePurchaseStatus.isDenied: Boolean
    get() = this is MessagePurchaseStatus.Denied

inline val MessagePurchaseStatus.isProcessing: Boolean
    get() = this is MessagePurchaseStatus.Processing

sealed class MessagePurchaseStatus {
    object Accepted: MessagePurchaseStatus()
    object Denied: MessagePurchaseStatus()
    object Pending: MessagePurchaseStatus()
    object Processing: MessagePurchaseStatus()
}
