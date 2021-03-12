package chat.sphinx.dto_contact.model

@Suppress("NOTHING_TO_INLINE")
inline fun ContactStatus.isPending(): Boolean =
    this is ContactStatus.Pending

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
        private const val PENDING = 0
        private const val CONFIRMED = 1

        /**
         * Converts the integer value returned over the wire to an object.
         *
         * @throws [IllegalArgumentException] if the [status] integer is not supported
         * */
        @Throws(IllegalArgumentException::class)
        fun fromInt(status: Int?): ContactStatus =
            when (status) {
                null, // the only time null is sent, is for the owner account
                CONFIRMED -> {
                    Confirmed
                }
                PENDING -> {
                    Pending
                }
                else -> {
                    throw IllegalArgumentException(
                        "ContactStatus for integer '$status' is not supported"
                    )
                }
            }
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