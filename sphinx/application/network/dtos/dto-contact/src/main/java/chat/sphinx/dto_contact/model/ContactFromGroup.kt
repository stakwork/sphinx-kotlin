package chat.sphinx.dto_contact.model

@Suppress("NOTHING_TO_INLINE")
inline fun ContactFromGroup.isTrue(): Boolean =
    this is ContactFromGroup.True

/**
 * Comes off the wire as:
 *  - 0 (Not From Group)
 *  - 1 (From Group)
 *
 * Tribe Admins have contacts for all members of the tribe. The
 * [ContactFromGroup] field denotes if they are from a group or not
 * such that displaying contacts from the Address Book screen does not
 * show all the user's Tribe contacts.
 * */
sealed class ContactFromGroup {

    companion object {
        private const val FROM_GROUP = 1
        private const val NOT_FROM_GROUP = 0

        /**
         * Converts the integer value returned over the wire to an object.
         *
         * @throws [IllegalArgumentException] if the [fromGroup] integer is not supported
         * */
        @Throws(IllegalArgumentException::class)
        fun fromInt(fromGroup: Int): ContactFromGroup =
            when (fromGroup) {
                FROM_GROUP -> {
                    True
                }
                NOT_FROM_GROUP -> {
                    False
                }
                else -> {
                    throw IllegalArgumentException(
                        "ContactFromGroup for integer '$fromGroup' not supported"
                    )
                }
            }
    }

    abstract val value: Int

    object True: ContactFromGroup() {
        override val value: Int
            get() = FROM_GROUP
    }

    object False: ContactFromGroup() {
        override val value: Int
            get() = NOT_FROM_GROUP
    }
}