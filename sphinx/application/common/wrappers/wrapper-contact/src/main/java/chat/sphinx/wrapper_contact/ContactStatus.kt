package chat.sphinx.wrapper_contact

@Suppress("NOTHING_TO_INLINE")
inline fun ContactStatus.isOwner(): Boolean =
    this is ContactStatus.AccountOwner

@Suppress("NOTHING_TO_INLINE")
inline fun ContactStatus.isConfirmed(): Boolean =
    this is ContactStatus.Confirmed

@Suppress("NOTHING_TO_INLINE")
inline fun ContactStatus.isPending(): Boolean =
    this is ContactStatus.Pending

@Suppress("NOTHING_TO_INLINE")
inline fun ContactStatus.isUnknown(): Boolean =
    this is ContactStatus.Unknown

/**
 * Converts the integer value returned over the wire to an object.
 * */
@Suppress("NOTHING_TO_INLINE")
inline fun Int?.toContactStatus(): ContactStatus =
    when (this) {
        null -> {
            ContactStatus.AccountOwner
        }
        ContactStatus.CONFIRMED -> {
            ContactStatus.Confirmed
        }
        ContactStatus.PENDING -> {
            ContactStatus.Pending
        }
        else -> {
            ContactStatus.Unknown(this)
        }
    }

/**
 * Comes off the wire as:
 *  - null (AccountOwner)
 *  - 0 (Pending)
 *  - 1 (Confirmed)
 *
 * [Pending] is used when inviting a new user. When the user signs up using the
 * invite code provided, status changes to 1 ([Confirmed]) and the contact info is
 * updated.
 *
 * https://github.com/stakwork/sphinx-relay/blob/7f8fd308101b5c279f6aac070533519160aa4a9f/src/constants.ts#L12
 * */
sealed class ContactStatus {

    companion object {
        const val PENDING = 0
        const val CONFIRMED = 1
    }

    abstract val value: Int?

    object AccountOwner: ContactStatus() {
        override val value: Int?
            get() = null
    }

    object Pending: ContactStatus() {
        override val value: Int
            get() = PENDING
    }

    object Confirmed: ContactStatus() {
        override val value: Int
            get() = CONFIRMED
    }

    data class Unknown(override val value: Int) : ContactStatus()
}
