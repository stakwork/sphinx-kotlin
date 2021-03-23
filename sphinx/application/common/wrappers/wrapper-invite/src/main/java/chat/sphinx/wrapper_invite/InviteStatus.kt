package chat.sphinx.wrapper_invite

@Suppress("NOTHING_TO_INLINE")
inline fun InviteStatus.isPending(): Boolean =
    this is InviteStatus.Pending

@Suppress("NOTHING_TO_INLINE")
inline fun InviteStatus.isReady(): Boolean =
    this is InviteStatus.Ready

@Suppress("NOTHING_TO_INLINE")
inline fun InviteStatus.isDelivered(): Boolean =
    this is InviteStatus.Delivered

@Suppress("NOTHING_TO_INLINE")
inline fun InviteStatus.isInProgress(): Boolean =
    this is InviteStatus.InProgress

@Suppress("NOTHING_TO_INLINE")
inline fun InviteStatus.isComplete(): Boolean =
    this is InviteStatus.Complete

@Suppress("NOTHING_TO_INLINE")
inline fun InviteStatus.isExpired(): Boolean =
    this is InviteStatus.Expired

@Suppress("NOTHING_TO_INLINE")
inline fun InviteStatus.isPaymentPending(): Boolean =
    this is InviteStatus.PaymentPending

/**
 * Converts the integer value returned over the wire to an object.
 *
 * @throws [IllegalArgumentException] if the integer is not supported
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun Int.toInviteStatus(): InviteStatus =
    when (this) {
        InviteStatus.PENDING -> {
            InviteStatus.Pending
        }
        InviteStatus.READY -> {
            InviteStatus.Ready
        }
        InviteStatus.DELIVERED -> {
            InviteStatus.Delivered
        }
        InviteStatus.IN_PROGRESS -> {
            InviteStatus.InProgress
        }
        InviteStatus.COMPLETE -> {
            InviteStatus.Complete
        }
        InviteStatus.EXPIRED -> {
            InviteStatus.Expired
        }
        InviteStatus.PAYMENT_PENDING -> {
            InviteStatus.PaymentPending
        }
        else -> {
            throw IllegalArgumentException(
                "InviteStatus for integer '$this' not supported"
            )
        }
    }

/**
 * Comes off the wire as:
 *  - 0 (Pending)
 *  - 1 (Ready)
 *  - 2 (Delivered)
 *  - 3 (InProgress)
 *  - 4 (Complete)
 *  - 5 (Expired)
 *  - 6 (PaymentPending)
 *
 *  https://github.com/stakwork/sphinx-relay/blob/7f8fd308101b5c279f6aac070533519160aa4a9f/src/constants.ts#L3
 * */
sealed class InviteStatus {

    companion object {
        const val PENDING = 0
        const val READY = 1
        const val DELIVERED = 2
        const val IN_PROGRESS = 3
        const val COMPLETE = 4
        const val EXPIRED = 5
        const val PAYMENT_PENDING = 6
    }

    abstract val value: Int

    object Pending: InviteStatus() {
        override val value: Int
            get() = PENDING
    }

    object Ready: InviteStatus() {
        override val value: Int
            get() = READY
    }

    object Delivered: InviteStatus() {
        override val value: Int
            get() = DELIVERED
    }

    object InProgress: InviteStatus() {
        override val value: Int
            get() = IN_PROGRESS
    }

    object Complete: InviteStatus() {
        override val value: Int
            get() = COMPLETE
    }

    object Expired: InviteStatus() {
        override val value: Int
            get() = EXPIRED
    }

    object PaymentPending: InviteStatus() {
        override val value: Int
            get() = PAYMENT_PENDING
    }
}
