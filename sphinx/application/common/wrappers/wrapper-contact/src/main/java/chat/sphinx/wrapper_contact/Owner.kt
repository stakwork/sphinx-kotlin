package chat.sphinx.wrapper_contact

@Suppress("NOTHING_TO_INLINE")
inline fun Owner.isTrue(): Boolean =
    this is Owner.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toOwner(): Owner =
    when (this) {
        Owner.OWNER -> {
            Owner.True
        }
        else -> {
            Owner.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toOwner(): Owner =
    if (this) Owner.True else Owner.False

/**
 * Comes off the wire as:
 *  - 0 (Not Owner)
 *  - 1 (Owner)
 * */
sealed class Owner {

    companion object {
        const val OWNER = 1
        const val NOT_OWNER = 0
    }

    abstract val value: Int

    object True: Owner() {
        override val value: Int
            get() = OWNER
    }

    object False: Owner() {
        override val value: Int
            get() = NOT_OWNER
    }
}
