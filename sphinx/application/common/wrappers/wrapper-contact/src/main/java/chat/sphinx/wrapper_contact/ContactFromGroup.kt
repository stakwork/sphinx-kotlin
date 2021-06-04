package chat.sphinx.wrapper_contact

@Suppress("NOTHING_TO_INLINE")
inline fun ContactFromGroup.isTrue(): Boolean =
    this is ContactFromGroup.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toContactFromGroup(): ContactFromGroup =
    when (this) {
        ContactFromGroup.FROM_GROUP -> {
            ContactFromGroup.True
        }
        else -> {
            ContactFromGroup.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toContactFromGroup(): ContactFromGroup =
    if (this) ContactFromGroup.True else ContactFromGroup.False

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
        const val FROM_GROUP = 1
        const val NOT_FROM_GROUP = 0
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
