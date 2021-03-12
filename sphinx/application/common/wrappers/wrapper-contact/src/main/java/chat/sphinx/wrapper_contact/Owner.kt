package chat.sphinx.wrapper_contact

@Suppress("NOTHING_TO_INLINE")
inline fun Owner.isTrue(): Boolean =
    this is Owner.True

/**
 * Converts the integer value returned over the wire to an object.
 *
 * @throws [IllegalArgumentException] if the integer is not supported
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun Int.toOwner(): Owner =
    when (this) {
        Owner.OWNER -> {
            Owner.True
        }
        Owner.NOT_OWNER -> {
            Owner.False
        }
        else -> {
            throw IllegalArgumentException(
                "Owner for integer '$this' not supported"
            )
        }
    }

/**
 * Comes off the wire as:
 *  - 0 (NotOwner)
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
