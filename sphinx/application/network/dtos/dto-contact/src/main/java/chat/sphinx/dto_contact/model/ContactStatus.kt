package chat.sphinx.dto_contact.model

@Suppress("NOTHING_TO_INLINE")
inline fun ContactStatus.isPending(): Boolean =
    this is ContactStatus.Pending

/**
 * Converts the integer value returned over the wire to an object.
 *
 * @throws [IllegalArgumentException] if the integer is not supported
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun Int?.toContactStatus(): ContactStatus =
    when (this) {
        null, // the only time null is sent, is for the owner account
        ContactStatus.CONFIRMED -> {
            ContactStatus.Confirmed
        }
        ContactStatus.PENDING -> {
            ContactStatus.Pending
        }
        else -> {
            throw IllegalArgumentException(
                "ContactStatus for integer '$this' is not supported"
            )
        }
    }

/**
 * Comes off the wire as:
 *  - null (account owner, confirmed)
 *  - 0 (Pending)
 *  - 1 (Confirmed)
 *
 * Pending is used when inviting a new user. When the user signs up using the
 * invite code provided, status changes to 1 (Confirmed) and the contact info is
 * updated.
 * */
sealed class ContactStatus {

    companion object {
        const val PENDING = 0
        const val CONFIRMED = 1
    }

    abstract val value: Int

    object Pending: ContactStatus() {
        override val value: Int
            get() = PENDING
    }

    object Confirmed: ContactStatus() {
        override val value: Int
            get() = CONFIRMED
    }
}
